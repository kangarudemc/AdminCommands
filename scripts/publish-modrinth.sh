#!/usr/bin/env bash
# Build and publish AdminCommands to Modrinth.
# Prerequisites: modrinth_project_id in gradle.properties, MODRINTH_TOKEN in env.
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ -z "${MODRINTH_TOKEN:-}" ]]; then
  echo "Set MODRINTH_TOKEN (Modrinth pat with CREATE_VERSION + PROJECT_WRITE)." >&2
  exit 1
fi

if ! grep -qE '^modrinth_project_id=.+$' gradle.properties 2>/dev/null; then
  echo "Set modrinth_project_id=... in gradle.properties (see docs/MODRINTH.md)." >&2
  exit 1
fi

./gradlew build modrinthPublish --no-daemon "$@"
echo "Done. Check your project on Modrinth."
