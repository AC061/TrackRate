#!/usr/bin/env bash
# Reset solo TrackRate (delega a backend/scripts/reset-trackrate.sh).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
exec bash "$ROOT/backend/scripts/reset-trackrate.sh"
