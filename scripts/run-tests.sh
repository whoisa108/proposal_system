#!/usr/bin/env bash
# run-tests.sh — load .env then execute Maven verify (tests + JaCoCo coverage gate)
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"

# ── Load .env if present ──────────────────────────────────────────────────────
if [[ -f "$ENV_FILE" ]]; then
    echo "[run-tests] Loading environment from .env"
    # Export each non-comment, non-blank line
    set -o allexport
    # shellcheck source=/dev/null
    source "$ENV_FILE"
    set +o allexport
else
    echo "[run-tests] No .env file found — using existing environment variables"
fi

# ── Verify required variables are present ────────────────────────────────────
REQUIRED_VARS=(
    MONGODB_URI
    JWT_SECRET
    MINIO_ENDPOINT
    MINIO_ACCESS_KEY
    MINIO_SECRET_KEY
    ADMIN_EMPLOYEE_ID
    ADMIN_PASSWORD
    USER_EMPLOYEE_ID
    USER_PASSWORD
)

MISSING=()
for var in "${REQUIRED_VARS[@]}"; do
    if [[ -z "${!var:-}" ]]; then
        MISSING+=("$var")
    fi
done

if [[ ${#MISSING[@]} -gt 0 ]]; then
    echo "[run-tests] ERROR: Missing required environment variables:"
    for v in "${MISSING[@]}"; do
        echo "  - $v"
    done
    echo ""
    echo "  Copy .env.example to .env and fill in the values, or export the"
    echo "  variables directly in your shell before running this script."
    exit 1
fi

# ── Run Maven verify ──────────────────────────────────────────────────────────
# 'verify' runs: compile → test → package → verify (JaCoCo coverage gate)
# This is the correct phase for CI because:
#   - 'test'            skips the JaCoCo coverage-ratio check
#   - 'clean package'   stops before the verify phase (no coverage gate)
#   - 'verify'          runs everything and enforces coverage thresholds
echo "[run-tests] Running: mvn clean verify"
cd "${REPO_ROOT}/backend"
mvn clean verify

echo ""
echo "[run-tests] All tests passed."
