#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORTS_DIR="${SCRIPT_DIR}/reports"
mkdir -p "${REPORTS_DIR}"

PYTHON_BIN="${PYTHON_BIN:-}"
if [[ -z "${PYTHON_BIN}" ]]; then
  if command -v python >/dev/null 2>&1; then
    PYTHON_BIN="python"
  elif command -v python3 >/dev/null 2>&1; then
    PYTHON_BIN="python3"
  else
    echo "Error: python/python3 not found in PATH." >&2
    exit 1
  fi
fi

if [[ -z "${VIRTUAL_ENV:-}" ]]; then
  if [[ ! -x "${SCRIPT_DIR}/.venv/bin/python" ]]; then
    echo "Creating virtual environment in ${SCRIPT_DIR}/.venv ..."
    "${PYTHON_BIN}" -m venv "${SCRIPT_DIR}/.venv"
    "${SCRIPT_DIR}/.venv/bin/pip" install -r "${SCRIPT_DIR}/requirements.txt"
  fi
  PYTHON_BIN="${SCRIPT_DIR}/.venv/bin/python"
fi

DOTNET_MINIMAL_URL="${DOTNET_MINIMAL_URL:-http://localhost:5080/bills-minimal}"
DOTNET_DDD_URL="${DOTNET_DDD_URL:-http://localhost:5080/bills}"
PYTHON_MINIMAL_URL="${PYTHON_MINIMAL_URL:-http://localhost:5081/bills-minimal}"
PYTHON_DDD_URL="${PYTHON_DDD_URL:-http://localhost:5081/bills}"
REQUESTS="${REQUESTS:-500}"
CONCURRENCY="${CONCURRENCY:-25}"
WARMUP_REQUESTS="${WARMUP_REQUESTS:-20}"
TIMEOUT_SEC="${TIMEOUT_SEC:-10}"
TS="$(date +%Y%m%d-%H%M%S)"

DOTNET_MINIMAL_REPORT="${REPORTS_DIR}/dotnet-minimal-${TS}.json"
DOTNET_DDD_REPORT="${REPORTS_DIR}/dotnet-ddd-${TS}.json"
PYTHON_MINIMAL_REPORT="${REPORTS_DIR}/python-minimal-${TS}.json"
PYTHON_DDD_REPORT="${REPORTS_DIR}/python-ddd-${TS}.json"

echo "Running .NET Minimal benchmark..."
"${PYTHON_BIN}" "${SCRIPT_DIR}/benchmark.py" \
  --url "${DOTNET_MINIMAL_URL}" \
  --requests "${REQUESTS}" \
  --concurrency "${CONCURRENCY}" \
  --warmup-requests "${WARMUP_REQUESTS}" \
  --timeout-sec "${TIMEOUT_SEC}" \
  --output-json "${DOTNET_MINIMAL_REPORT}" >/dev/null

echo "Running .NET DDD benchmark..."
"${PYTHON_BIN}" "${SCRIPT_DIR}/benchmark.py" \
  --url "${DOTNET_DDD_URL}" \
  --requests "${REQUESTS}" \
  --concurrency "${CONCURRENCY}" \
  --warmup-requests "${WARMUP_REQUESTS}" \
  --timeout-sec "${TIMEOUT_SEC}" \
  --output-json "${DOTNET_DDD_REPORT}" >/dev/null

echo "Running Python Minimal benchmark..."
"${PYTHON_BIN}" "${SCRIPT_DIR}/benchmark.py" \
  --url "${PYTHON_MINIMAL_URL}" \
  --requests "${REQUESTS}" \
  --concurrency "${CONCURRENCY}" \
  --warmup-requests "${WARMUP_REQUESTS}" \
  --timeout-sec "${TIMEOUT_SEC}" \
  --output-json "${PYTHON_MINIMAL_REPORT}" >/dev/null

echo "Running Python DDD benchmark..."
"${PYTHON_BIN}" "${SCRIPT_DIR}/benchmark.py" \
  --url "${PYTHON_DDD_URL}" \
  --requests "${REQUESTS}" \
  --concurrency "${CONCURRENCY}" \
  --warmup-requests "${WARMUP_REQUESTS}" \
  --timeout-sec "${TIMEOUT_SEC}" \
  --output-json "${PYTHON_DDD_REPORT}" >/dev/null

echo
echo "Reports:"
echo "- .NET Minimal: ${DOTNET_MINIMAL_REPORT}"
echo "- .NET DDD    : ${DOTNET_DDD_REPORT}"
echo "- Python Minimal: ${PYTHON_MINIMAL_REPORT}"
echo "- Python DDD    : ${PYTHON_DDD_REPORT}"
echo

"${PYTHON_BIN}" - "${DOTNET_MINIMAL_REPORT}" "${DOTNET_DDD_REPORT}" "${PYTHON_MINIMAL_REPORT}" "${PYTHON_DDD_REPORT}" <<'PY'
import json
import sys

dotnet_minimal_path, dotnet_ddd_path, python_minimal_path, python_ddd_path = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]

with open(dotnet_minimal_path, "r", encoding="utf-8") as f:
    dotnet_minimal = json.load(f)
with open(dotnet_ddd_path, "r", encoding="utf-8") as f:
    dotnet_ddd = json.load(f)
with open(python_minimal_path, "r", encoding="utf-8") as f:
    python_minimal = json.load(f)
with open(python_ddd_path, "r", encoding="utf-8") as f:
    python_ddd = json.load(f)

def row(name, data):
    s = data["summary"]
    l = data["latency_ms"]
    return {
        "name": name,
        "throughput": s["throughput_req_per_sec"],
        "success_rate": s["success_rate_pct"],
        "p95": l["p95"],
        "p99": l["p99"],
        "avg": l["avg"],
    }

rows = [
    row(".NET-Min", dotnet_minimal),
    row(".NET-DDD", dotnet_ddd),
    row("Py-Min", python_minimal),
    row("Py-DDD", python_ddd),
]

print("Comparison (same load profile):")
print(f"{'API':<8} {'Throughput':>12} {'Success%':>10} {'Avg ms':>10} {'P95 ms':>10} {'P99 ms':>10}")
for r in rows:
    print(f"{r['name']:<8} {r['throughput']:>12} {r['success_rate']:>10} {r['avg']:>10} {r['p95']:>10} {r['p99']:>10}")

print()
winner_thr = max(rows, key=lambda x: x["throughput"])["name"]
winner_p95 = min(rows, key=lambda x: x["p95"])["name"]
winner_p99 = min(rows, key=lambda x: x["p99"])["name"]
print(f"Throughput winner: {winner_thr}")
print(f"P95 latency winner: {winner_p95}")
print(f"P99 latency winner: {winner_p99}")
PY
