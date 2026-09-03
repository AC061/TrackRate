#!/usr/bin/env bash
# Alias → stack-reset.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
exec bash "$ROOT/scripts/stack-reset.sh"
