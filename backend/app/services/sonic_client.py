"""Cliente mínimo para Sonic Channel (protocolo TCP)."""

from __future__ import annotations

import logging
import socket
import time
from contextlib import contextmanager
from typing import Iterator, Literal

from app.config import settings

logger = logging.getLogger(__name__)

SonicMode = Literal["search", "ingest", "control"]


class SonicError(Exception):
    pass


class SonicClient:
    def __init__(
        self,
        *,
        host: str | None = None,
        port: int | None = None,
        password: str | None = None,
        timeout: float | None = None,
    ) -> None:
        self.host = host or settings.sonic_host
        self.port = port or settings.sonic_port
        self.password = password or settings.sonic_password
        self.timeout = timeout if timeout is not None else settings.sonic_timeout_seconds
        self._sock: socket.socket | None = None
        self._mode: SonicMode | None = None

    def _readline(self) -> str:
        assert self._sock is not None
        buf = bytearray()
        while True:
            chunk = self._sock.recv(4096)
            if not chunk:
                raise SonicError("Conexión Sonic cerrada inesperadamente")
            buf.extend(chunk)
            if b"\n" in chunk:
                break
        line, _, rest = bytes(buf).partition(b"\n")
        if rest:
            logger.debug("Sonic: bytes sobrantes tras readline: %r", rest[:80])
        return line.decode("utf-8", errors="replace").strip()

    @staticmethod
    def _quote(text: str) -> str:
        escaped = text.replace("\\", "\\\\").replace('"', '\\"')
        return f'"{escaped}"'

    def _send(self, command: str) -> None:
        assert self._sock is not None
        self._sock.sendall(f"{command}\n".encode("utf-8"))

    def _connect(self) -> None:
        if self._sock is not None:
            return
        self._sock = socket.create_connection((self.host, self.port), timeout=self.timeout)
        self._sock.settimeout(self.timeout)
        connected = self._readline()
        if not connected.startswith("CONNECTED"):
            raise SonicError(f"Handshake Sonic inválido: {connected}")

    def _disconnect(self) -> None:
        if self._sock is None:
            return
        try:
            if self._mode is not None:
                self._send("QUIT")
                try:
                    self._readline()
                except (OSError, SonicError):
                    pass
        except OSError:
            pass
        finally:
            self._sock.close()
            self._sock = None
            self._mode = None

    def __enter__(self) -> SonicClient:
        self._connect()
        return self

    def __exit__(self, *args: object) -> None:
        self._disconnect()

    def _start(self, mode: SonicMode) -> None:
        if self._mode == mode:
            return
        if self._mode is not None:
            raise SonicError("Sesión Sonic ya iniciada en otro modo")
        self._send(f"START {mode} {self.password}")
        started = self._readline()
        if started.startswith("ERR"):
            raise SonicError(started)
        if not started.startswith("STARTED"):
            raise SonicError(f"START {mode} falló: {started}")
        self._mode = mode

    @contextmanager
    def session(self, mode: SonicMode) -> Iterator[SonicClient]:
        self._connect()
        try:
            self._start(mode)
            yield self
        finally:
            self._disconnect()

    def _await_event(self, event_prefix: str, *, max_lines: int = 200) -> list[str]:
        marker: str | None = None
        for _ in range(max_lines):
            line = self._readline()
            if line.startswith("PENDING"):
                parts = line.split(maxsplit=1)
                if len(parts) == 2:
                    marker = parts[1]
                continue
            if line.startswith("ERR"):
                raise SonicError(line)
            if line.startswith(event_prefix):
                parts = line.split()
                if marker is None:
                    raise SonicError(f"EVENT sin PENDING previo: {line}")
                if len(parts) >= 3 and parts[2] == marker:
                    return parts[3:]
                logger.debug("Sonic: ignorando %s (esperaba marker %s)", line, marker)
        raise SonicError(f"Sin respuesta Sonic para {event_prefix} tras {max_lines} líneas")

    def ping(self, mode: SonicMode = "search") -> bool:
        try:
            with self.session(mode) as client:
                client._send("PING")
                return client._readline() == "PONG"
        except (OSError, SonicError) as exc:
            logger.warning("Sonic ping falló: %s", exc)
            return False

    def query(
        self,
        collection: str,
        bucket: str,
        terms: str,
        *,
        limit: int = 25,
        offset: int = 0,
        lang: str | None = None,
    ) -> list[str]:
        clean = terms.strip()
        if not clean:
            return []

        with self.session("search") as client:
            return client._query(collection, bucket, clean, limit=limit, offset=offset, lang=lang)

    def _query(
        self,
        collection: str,
        bucket: str,
        terms: str,
        *,
        limit: int = 25,
        offset: int = 0,
        lang: str | None = None,
    ) -> list[str]:
        parts = [
            "QUERY",
            collection,
            bucket,
            self._quote(terms),
            f"LIMIT({limit})",
        ]
        if offset > 0:
            parts.append(f"OFFSET({offset})")
        if lang:
            parts.append(f"LANG({lang})")
        assert self._sock is not None
        prev = self._sock.gettimeout()
        self._sock.settimeout(self.timeout)
        try:
            self._send(" ".join(parts))
            return self._await_event("EVENT QUERY")
        finally:
            self._sock.settimeout(prev)

    def suggest(
        self,
        collection: str,
        bucket: str,
        word: str,
        *,
        limit: int = 10,
        lang: str | None = None,
    ) -> list[str]:
        clean = word.strip()
        if not clean:
            return []

        with self.session("search") as client:
            return client._suggest(collection, bucket, clean, limit=limit, lang=lang)

    def _suggest(
        self,
        collection: str,
        bucket: str,
        word: str,
        *,
        limit: int = 10,
        lang: str | None = None,
    ) -> list[str]:
        parts = [
            "SUGGEST",
            collection,
            bucket,
            self._quote(word),
            f"LIMIT({limit})",
        ]
        if lang:
            parts.append(f"LANG({lang})")
        assert self._sock is not None
        prev = self._sock.gettimeout()
        self._sock.settimeout(self.timeout)
        try:
            self._send(" ".join(parts))
            return self._await_event("EVENT SUGGEST")
        finally:
            self._sock.settimeout(prev)

    def push(
        self,
        collection: str,
        bucket: str,
        object_id: str,
        text: str,
        *,
        lang: str | None = None,
    ) -> None:
        with self.session("ingest") as client:
            client._push(collection, bucket, object_id, text, lang=lang)

    def _push(
        self,
        collection: str,
        bucket: str,
        object_id: str,
        text: str,
        *,
        lang: str | None = None,
    ) -> None:
        parts = [
            "PUSH",
            collection,
            bucket,
            object_id,
            self._quote(text),
        ]
        if lang:
            parts.append(f"LANG({lang})")
        self._send(" ".join(parts))
        response = self._readline()
        if response != "OK":
            raise SonicError(response)

    def push_batch(
        self,
        collection: str,
        bucket: str,
        items: list[tuple[str, str]],
        *,
        lang: str | None = None,
    ) -> int:
        return self.push_batches(collection, bucket, [items], lang=lang)

    def push_batches(
        self,
        collection: str,
        bucket: str,
        batches: list[list[tuple[str, str]]] | object,
        *,
        lang: str | None = None,
    ) -> int:
        count = 0
        with self.session("ingest") as client:
            for batch in batches:
                for object_id, text in batch:
                    client._push(collection, bucket, object_id, text, lang=lang)
                    count += 1
        return count

    def flush_bucket(self, collection: str, bucket: str) -> None:
        with self.session("ingest") as client:
            client._send(f"FLUSHB {collection} {bucket}")
            response = client._readline()
            if not response.startswith("RESULT"):
                raise SonicError(response)

    def count(self, collection: str, bucket: str | None = None) -> int:
        with self.session("ingest") as client:
            if bucket:
                client._send(f"COUNT {collection} {bucket}")
            else:
                client._send(f"COUNT {collection}")
            response = client._readline()
            if not response.startswith("RESULT"):
                raise SonicError(response)
            return int(response.split(maxsplit=1)[1])

    def consolidate(self) -> None:
        with self.session("control") as client:
            client._send("TRIGGER consolidate")
            response = client._readline()
            if response != "OK":
                raise SonicError(response)

    def wait_for_search_ready(
        self,
        collection: str,
        bucket: str = "artist",
        probe_term: str = "the",
        *,
        timeout_seconds: float = 900,
        poll_interval: float = 5.0,
        query_timeout: float | None = None,
    ) -> float:
        """Espera a que QUERY responda (post-consolidate). Devuelve latencia ms del probe."""
        deadline = time.monotonic() + timeout_seconds
        last_error = "sin respuesta"
        qt = query_timeout or settings.sonic_query_read_timeout_seconds
        probe_client = SonicClient(
            host=self.host,
            port=self.port,
            password=self.password,
            timeout=qt,
        )
        while time.monotonic() < deadline:
            try:
                t0 = time.perf_counter()
                ids = probe_client.query(collection, bucket, probe_term, limit=1)
                ms = (time.perf_counter() - t0) * 1000
                logger.info(
                    "Sonic QUERY listo (%s/%s %r) ms=%.0f ids=%d",
                    collection,
                    bucket,
                    probe_term,
                    ms,
                    len(ids),
                )
                return ms
            except (TimeoutError, OSError, SonicError) as exc:
                last_error = str(exc)
                logger.info("Sonic aún consolidando o no listo: %s", exc)
                time.sleep(poll_interval)
        raise SonicError(f"Sonic QUERY no respondió en {timeout_seconds}s: {last_error}")
