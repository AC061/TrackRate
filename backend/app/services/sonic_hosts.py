"""Resolución de hostname Sonic dentro y fuera de Docker Compose."""

from __future__ import annotations

import logging
import os
import socket

from app.config import settings

logger = logging.getLogger(__name__)

_last_working_host: str | None = None


def running_in_container() -> bool:
    return os.path.exists("/.dockerenv") or os.environ.get("RUNNING_IN_DOCKER") == "1"


def sonic_host_candidates() -> list[str]:
    """Orden de hosts a probar. host.docker.internal primero en Docker."""
    seen: set[str] = set()
    ordered: list[str] = []

    def add(host: str | None) -> None:
        if host and host not in seen:
            seen.add(host)
            ordered.append(host)

    if running_in_container():
        # host.docker.internal → puerto publicado en el host (QUERY fiable).
        # El alias DNS "sonic" suele responder PING pero colgar en QUERY.
        add("host.docker.internal")
        configured = settings.sonic_host or os.environ.get("SONIC_HOST")
        if configured and configured not in ("sonic", "trackrate-sonic"):
            add(configured)
        elif configured in ("sonic", "trackrate-sonic"):
            logger.debug(
                "SONIC_HOST=%s ignorado en contenedor; usar host.docker.internal",
                configured,
            )
    else:
        add("127.0.0.1")
        add("localhost")
        add(settings.sonic_host)
        add(os.environ.get("SONIC_HOST"))

    return ordered


def remember_working_host(host: str) -> None:
    global _last_working_host
    _last_working_host = host
    logger.debug("Sonic host operativo: %s", host)


def clear_working_host_cache() -> None:
    global _last_working_host
    _last_working_host = None


def resolve_sonic_host(*, timeout: float = 2.0) -> str | None:
    """Devuelve el primer host que resuelva por DNS."""
    port = settings.sonic_port
    prev = socket.getdefaulttimeout()
    socket.setdefaulttimeout(timeout)
    try:
        for host in sonic_host_candidates():
            try:
                socket.getaddrinfo(host, port, type=socket.SOCK_STREAM)
                return host
            except OSError:
                continue
    finally:
        socket.setdefaulttimeout(prev)
    return None


def resolve_sonic_host_for_query(*, probe_timeout: float | None = None) -> str:
    """Host que responde QUERY (probe corto). Reutiliza el último host OK del status."""
    global _last_working_host
    if _last_working_host is not None:
        return _last_working_host

    from app.services.sonic_client import SonicClient, SonicError

    timeout = probe_timeout if probe_timeout is not None else settings.sonic_probe_timeout_seconds
    last_error = "sin host"

    for host in sonic_host_candidates():
        client = SonicClient(host=host, timeout=timeout)
        try:
            if not client.ping():
                continue
            client.query(
                settings.sonic_collection,
                "artist",
                "the",
                limit=1,
                lang="eng",
            )
            remember_working_host(host)
            return host
        except (OSError, SonicError, TimeoutError) as exc:
            last_error = str(exc)
            logger.debug("Sonic probe falló en %s: %s", host, exc)
            continue

    raise SonicError(f"Ningún host Sonic respondió QUERY: {last_error}")


def sonic_dns_help() -> str:
    if running_in_container():
        return (
            "Sonic no responde QUERY.\n"
            "En ~/backend/.env: SONIC_HOST=host.docker.internal\n"
            "  docker compose up -d --force-recreate --no-deps trackrate-api"
        )
    return (
        "Ejecutando fuera del contenedor API. Usa localhost:1491 o:\n"
        "  docker compose exec trackrate-api python -m scripts.diagnose_sonic"
    )
