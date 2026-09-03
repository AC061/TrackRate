#!/usr/bin/env bash
# Redirige al stack unificado en la raíz del repo.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
exec bash "$ROOT/scripts/stack-setup.sh" "$@"
