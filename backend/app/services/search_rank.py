"""Relevancia de resultados de búsqueda de catálogo."""

from __future__ import annotations

_ARTICLE_PREFIX = "the "


def _normalize(text: str) -> str:
    t = text.casefold().strip()
    if t.startswith(_ARTICLE_PREFIX):
        t = t[len(_ARTICLE_PREFIX) :].strip()
    return t


def _display_name(row: dict) -> str:
    return (row.get("name") or row.get("title") or "").strip()


def relevance_key(query: str, row: dict) -> tuple[int, int, str]:
    """Menor tupla = más relevante."""
    q = _normalize(query)
    name = _display_name(row)
    n = _normalize(name)
    if not q or not n:
        return (99, len(name), name)
    if n == q:
        return (0, len(name), name)
    if n.startswith(q):
        return (1, len(name), name)
    if q in n.split():
        return (2, len(name), name)
    if q in n:
        return (3, len(name), name)
    return (4, len(name), name)


def dedupe_search_results(results: list[dict]) -> list[dict]:
    seen: set[tuple[str, str]] = set()
    out: list[dict] = []
    for row in results:
        key = (str(row.get("_trackrate_type", "")), str(row.get("id", "")))
        if key in seen:
            continue
        seen.add(key)
        out.append(row)
    return out


def rerank_search_results(
    query: str,
    results: list[dict],
    *,
    limit: int | None = None,
) -> list[dict]:
    if not results:
        return []
    ranked = sorted(results, key=lambda row: relevance_key(query, row))
    if limit is not None:
        return ranked[:limit]
    return ranked


def needs_broader_search(query: str, results: list[dict]) -> bool:
    """True si el mejor resultado no es coincidencia exacta (p. ej. 'Beatles Ranked' vs 'beatles')."""
    clean = query.strip()
    if not results or len(clean) < 3:
        return False
    return relevance_key(clean, results[0])[0] > 0
