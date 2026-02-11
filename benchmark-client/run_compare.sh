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
GO_MINIMAL_URL="${GO_MINIMAL_URL:-http://localhost:5082/bills-minimal}"
GO_DDD_URL="${GO_DDD_URL:-http://localhost:5082/bills}"
REQUESTS="${REQUESTS:-500}"
CONCURRENCY="${CONCURRENCY:-25}"
WARMUP_REQUESTS="${WARMUP_REQUESTS:-20}"
TIMEOUT_SEC="${TIMEOUT_SEC:-10}"
ROUNDS="${ROUNDS:-5}"
RUN_POST_BENCHMARK="${RUN_POST_BENCHMARK:-1}"
POST_ROUNDS="${POST_ROUNDS:-5}"
POST_REQUESTS="${POST_REQUESTS:-10}"
POST_CONCURRENCY="${POST_CONCURRENCY:-5}"
POST_TIMEOUT_SEC="${POST_TIMEOUT_SEC:-10}"
POST_MIN_LINES="${POST_MIN_LINES:-10}"
POST_MAX_LINES="${POST_MAX_LINES:-15}"
POST_DB_HOST="${POST_DB_HOST:-localhost}"
POST_DB_PORT="${POST_DB_PORT:-${POSTGRES_PORT:-5440}}"
POST_DB_NAME="${POST_DB_NAME:-${POSTGRES_DB:-api_lang_arena}}"
POST_DB_USER="${POST_DB_USER:-${POSTGRES_USER:-api_lang_user}}"
POST_DB_PASSWORD="${POST_DB_PASSWORD:-${POSTGRES_PASSWORD:-api_lang_password}}"
POST_DOTNET_URL="${POST_DOTNET_URL:-http://localhost:5080/bills}"
POST_PYTHON_URL="${POST_PYTHON_URL:-http://localhost:5081/bills}"
POST_GO_URL="${POST_GO_URL:-}"
TS="$(date +%Y%m%d-%H%M%S)"

"${PYTHON_BIN}" - "${PYTHON_BIN}" "${SCRIPT_DIR}" "${REPORTS_DIR}" "${TS}" "${REQUESTS}" "${CONCURRENCY}" "${WARMUP_REQUESTS}" "${TIMEOUT_SEC}" "${ROUNDS}" "${DOTNET_MINIMAL_URL}" "${DOTNET_DDD_URL}" "${PYTHON_MINIMAL_URL}" "${PYTHON_DDD_URL}" "${GO_MINIMAL_URL}" "${GO_DDD_URL}" <<'PY'
import json
import random
import statistics
import subprocess
import sys
from pathlib import Path

(
    python_bin,
    script_dir,
    reports_dir,
    ts,
    requests,
    concurrency,
    warmup_requests,
    timeout_sec,
    rounds,
    dotnet_min_url,
    dotnet_ddd_url,
    python_min_url,
    python_ddd_url,
    go_min_url,
    go_ddd_url,
) = sys.argv[1:16]

requests = int(requests)
concurrency = int(concurrency)
warmup_requests = int(warmup_requests)
timeout_sec = float(timeout_sec)
rounds = int(rounds)

targets = [
    ("dotnet-minimal", ".NET-Min", dotnet_min_url),
    ("dotnet-ddd", ".NET-DDD", dotnet_ddd_url),
    ("python-minimal", "Py-Min", python_min_url),
    ("python-ddd", "Py-DDD", python_ddd_url),
    ("go-minimal", "Go-Min", go_min_url),
    ("go-ddd", "Go-DDD", go_ddd_url),
]

metrics = {
    label: {"throughput": [], "success_rate": [], "avg": [], "p95": [], "p99": []}
    for _, label, _ in targets
}

report_paths = []

for round_idx in range(1, rounds + 1):
    order = list(targets)
    random.shuffle(order)
    print(f"Round {round_idx}/{rounds} order: " + ", ".join(label for _, label, _ in order))
    for key, label, url in order:
        report_path = Path(reports_dir) / f"{key}-r{round_idx}-{ts}.json"
        cmd = [
            python_bin,
            str(Path(script_dir) / "benchmark.py"),
            "--url", url,
            "--requests", str(requests),
            "--concurrency", str(concurrency),
            "--warmup-requests", str(warmup_requests),
            "--timeout-sec", str(timeout_sec),
            "--output-json", str(report_path),
        ]
        subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL)
        report_paths.append((label, str(report_path)))
        with report_path.open("r", encoding="utf-8") as f:
            data = json.load(f)
        summary = data["summary"]
        latency = data["latency_ms"]
        metrics[label]["throughput"].append(float(summary["throughput_req_per_sec"]))
        metrics[label]["success_rate"].append(float(summary["success_rate_pct"]))
        metrics[label]["avg"].append(float(latency["avg"]))
        metrics[label]["p95"].append(float(latency["p95"]))
        metrics[label]["p99"].append(float(latency["p99"]))

rows = []
for _, label, _ in targets:
    rows.append({
        "name": label,
        "throughput": round(statistics.median(metrics[label]["throughput"]), 2),
        "success_rate": round(statistics.median(metrics[label]["success_rate"]), 2),
        "avg": round(statistics.median(metrics[label]["avg"]), 2),
        "p95": round(statistics.median(metrics[label]["p95"]), 2),
        "p99": round(statistics.median(metrics[label]["p99"]), 2),
    })

print()
print(f"Reports generated: {len(report_paths)} (directory: {reports_dir})")
print("Comparison (median across rounds):")
print(f"{'API':<8} {'Throughput':>12} {'Success%':>10} {'Avg ms':>10} {'P95 ms':>10} {'P99 ms':>10}")
for row in rows:
    print(f"{row['name']:<8} {row['throughput']:>12} {row['success_rate']:>10} {row['avg']:>10} {row['p95']:>10} {row['p99']:>10}")

print()
winner_thr = max(rows, key=lambda x: x["throughput"])["name"]
winner_p95 = min(rows, key=lambda x: x["p95"])["name"]
winner_p99 = min(rows, key=lambda x: x["p99"])["name"]
print(f"Throughput winner: {winner_thr}")
print(f"P95 latency winner: {winner_p95}")
print(f"P99 latency winner: {winner_p99}")
PY

if [[ "${RUN_POST_BENCHMARK}" == "1" ]]; then
  echo
  "${PYTHON_BIN}" - "${PYTHON_BIN}" "${SCRIPT_DIR}" "${REPORTS_DIR}" "${TS}" \
    "${POST_ROUNDS}" "${POST_REQUESTS}" "${POST_CONCURRENCY}" "${POST_TIMEOUT_SEC}" \
    "${POST_MIN_LINES}" "${POST_MAX_LINES}" "${POST_DB_HOST}" "${POST_DB_PORT}" \
    "${POST_DB_NAME}" "${POST_DB_USER}" "${POST_DB_PASSWORD}" \
    "${POST_DOTNET_URL}" "${POST_PYTHON_URL}" "${POST_GO_URL}" <<'PY'
import json
import random
import statistics
import subprocess
import sys
from pathlib import Path

(
    python_bin,
    script_dir,
    reports_dir,
    ts,
    rounds,
    requests,
    concurrency,
    timeout_sec,
    min_lines,
    max_lines,
    db_host,
    db_port,
    db_name,
    db_user,
    db_password,
    dotnet_url,
    python_url,
    go_url,
) = sys.argv[1:19]

rounds = int(rounds)
requests = int(requests)
concurrency = int(concurrency)
timeout_sec = float(timeout_sec)
min_lines = int(min_lines)
max_lines = int(max_lines)

targets = [
    ("dotnet-post", ".NET-Post", dotnet_url),
    ("python-post", "Py-Post", python_url),
    ("go-post", "Go-Post", go_url),
]
targets = [item for item in targets if item[2].strip()]

if not targets:
    print("POST comparison skipped: no POST_*_URL values configured.")
    raise SystemExit(0)

metrics = {
    label: {"throughput": [], "success_rate": [], "created_rate": [], "avg": [], "p95": [], "p99": []}
    for _, label, _ in targets
}

for round_idx in range(1, rounds + 1):
    order = list(targets)
    random.shuffle(order)
    print(f"POST round {round_idx}/{rounds} order: " + ", ".join(label for _, label, _ in order))
    for key, label, url in order:
        report_path = Path(reports_dir) / f"{key}-r{round_idx}-{ts}.json"
        prefix = f"BENCH-{key.upper()}-{ts}-R{round_idx}"
        cmd = [
            python_bin,
            str(Path(script_dir) / "benchmark_post.py"),
            "--name", label,
            "--url", url,
            "--requests", str(requests),
            "--concurrency", str(concurrency),
            "--timeout-sec", str(timeout_sec),
            "--min-lines", str(min_lines),
            "--max-lines", str(max_lines),
            "--prefix", prefix,
            "--db-host", db_host,
            "--db-port", db_port,
            "--db-name", db_name,
            "--db-user", db_user,
            "--db-password", db_password,
            "--quiet",
            "--output-json", str(report_path),
        ]
        subprocess.run(cmd, check=True)
        with report_path.open("r", encoding="utf-8") as f:
            data = json.load(f)
        summary = data["summary"]
        latency = data["latency_ms"]
        metrics[label]["throughput"].append(float(summary["throughput_req_per_sec"]))
        metrics[label]["success_rate"].append(float(summary["success_rate_pct"]))
        metrics[label]["created_rate"].append(float(summary["created_rate_pct"]))
        metrics[label]["avg"].append(float(latency["avg"]))
        metrics[label]["p95"].append(float(latency["p95"]))
        metrics[label]["p99"].append(float(latency["p99"]))

rows = []
for _, label, _ in targets:
    rows.append({
        "name": label,
        "throughput": round(statistics.median(metrics[label]["throughput"]), 2),
        "success_rate": round(statistics.median(metrics[label]["success_rate"]), 2),
        "created_rate": round(statistics.median(metrics[label]["created_rate"]), 2),
        "avg": round(statistics.median(metrics[label]["avg"]), 2),
        "p95": round(statistics.median(metrics[label]["p95"]), 2),
        "p99": round(statistics.median(metrics[label]["p99"]), 2),
    })

print()
print("POST comparison (median across rounds):")
print(f"{'API':<10} {'Throughput':>12} {'Success%':>10} {'Created%':>10} {'Avg ms':>10} {'P95 ms':>10} {'P99 ms':>10}")
for row in rows:
    print(
        f"{row['name']:<10} {row['throughput']:>12} {row['success_rate']:>10} "
        f"{row['created_rate']:>10} {row['avg']:>10} {row['p95']:>10} {row['p99']:>10}"
    )
PY
fi
