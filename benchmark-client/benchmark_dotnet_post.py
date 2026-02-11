#!/usr/bin/env python3
import argparse
import json
import math
import os
import random
import statistics
import threading
import time
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from datetime import date
from typing import Optional

import psycopg
import requests


thread_local = threading.local()


@dataclass
class PostResult:
    ok: bool
    status_code: Optional[int]
    latency_ms: float
    response_bytes: int
    error: Optional[str]
    bill_id: Optional[int]


def percentile(sorted_values: list[float], p: float) -> float:
    if not sorted_values:
        return 0.0
    if p <= 0:
        return sorted_values[0]
    if p >= 100:
        return sorted_values[-1]
    k = (len(sorted_values) - 1) * (p / 100.0)
    f = math.floor(k)
    c = math.ceil(k)
    if f == c:
        return sorted_values[int(k)]
    d0 = sorted_values[f] * (c - k)
    d1 = sorted_values[c] * (k - f)
    return d0 + d1


def get_session() -> requests.Session:
    if not hasattr(thread_local, "session"):
        thread_local.session = requests.Session()
    return thread_local.session


def run_single_post(url: str, timeout_sec: float, payload: dict) -> PostResult:
    session = get_session()
    started = time.perf_counter()
    try:
        response = session.post(url=url, timeout=timeout_sec, json=payload)
        latency_ms = (time.perf_counter() - started) * 1000
        body = response.content or b""
        bill_id = None
        if 200 <= response.status_code < 300:
            try:
                body_json = response.json()
                if isinstance(body_json, dict):
                    maybe_id = body_json.get("id")
                    if isinstance(maybe_id, int):
                        bill_id = maybe_id
            except (json.JSONDecodeError, ValueError):
                pass

        return PostResult(
            ok=200 <= response.status_code < 300,
            status_code=response.status_code,
            latency_ms=latency_ms,
            response_bytes=len(body),
            error=None,
            bill_id=bill_id,
        )
    except requests.RequestException as exc:
        latency_ms = (time.perf_counter() - started) * 1000
        return PostResult(
            ok=False,
            status_code=None,
            latency_ms=latency_ms,
            response_bytes=0,
            error=type(exc).__name__,
            bill_id=None,
        )


def build_payload(prefix: str, idx: int, lines_count: int, rng: random.Random) -> dict:
    concepts = [
        "Cloud Hosting",
        "API Requests",
        "Data Processing",
        "Consulting Hours",
        "Support Plan",
        "Storage Usage",
        "Security Monitoring",
        "Training Session",
        "Integration Fee",
        "Premium Feature",
    ]

    lines = []
    subtotal = 0.0
    for _ in range(lines_count):
        quantity = round(rng.uniform(1.0, 5.0), 2)
        unit_amount = round(rng.uniform(10.0, 500.0), 2)
        line_amount = round(quantity * unit_amount, 2)
        subtotal += line_amount
        lines.append(
            {
                "concept": rng.choice(concepts),
                "quantity": quantity,
                "unitAmount": unit_amount,
            }
        )

    tax = round(subtotal * rng.uniform(0.05, 0.12), 2)

    return {
        "billNumber": f"{prefix}-{idx:03d}",
        "issuedAt": date.today().isoformat(),
        "customerName": f"{prefix}-customer-{idx:03d}",
        "currency": "USD",
        "tax": tax,
        "lines": lines,
    }


def cleanup_by_prefix(
    prefix: str,
    db_host: str,
    db_port: int,
    db_name: str,
    db_user: str,
    db_password: str,
) -> dict:
    conninfo = (
        f"host={db_host} port={db_port} dbname={db_name} "
        f"user={db_user} password={db_password}"
    )
    try:
        with psycopg.connect(conninfo) as conn:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    DELETE FROM bill
                    WHERE bill_number LIKE %s
                       OR customer_name LIKE %s
                    RETURNING id;
                    """,
                    (f"{prefix}%", f"{prefix}%"),
                )
                deleted_ids = [row[0] for row in cur.fetchall()]
            conn.commit()
        return {
            "attempted": True,
            "deleted_bill_count": len(deleted_ids),
            "deleted_bill_ids": deleted_ids,
            "error": None,
        }
    except Exception as exc:  # noqa: BLE001
        return {
            "attempted": True,
            "deleted_bill_count": 0,
            "deleted_bill_ids": [],
            "error": str(exc),
        }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Benchmark POST /bills with generated data and optional cleanup"
    )
    parser.add_argument("--name", default="POST-Target", help="Human friendly target name")
    parser.add_argument("--url", required=True, help="Endpoint URL to test")
    parser.add_argument("--requests", type=int, default=10, help="Number of bills to create")
    parser.add_argument("--concurrency", type=int, default=5, help="Number of concurrent workers")
    parser.add_argument("--timeout-sec", type=float, default=10.0, help="Request timeout in seconds")
    parser.add_argument("--min-lines", type=int, default=10, help="Minimum lines per bill")
    parser.add_argument("--max-lines", type=int, default=15, help="Maximum lines per bill")
    parser.add_argument("--prefix", help="Prefix marker for billNumber/customerName")
    parser.add_argument("--seed", type=int, default=42, help="Random seed for payload generation")
    parser.add_argument("--skip-cleanup", action="store_true", help="Do not delete generated benchmark bills")
    parser.add_argument("--quiet", action="store_true", help="Suppress summary line output")
    parser.add_argument("--output-json", help="Optional path to write full report JSON")

    parser.add_argument("--db-host", default=os.getenv("POSTGRES_HOST", "localhost"), help="Postgres host")
    parser.add_argument("--db-port", type=int, default=int(os.getenv("POSTGRES_PORT", "5440")), help="Postgres port")
    parser.add_argument("--db-name", default=os.getenv("POSTGRES_DB", "api_lang_arena"), help="Postgres database")
    parser.add_argument("--db-user", default=os.getenv("POSTGRES_USER", "api_lang_user"), help="Postgres user")
    parser.add_argument("--db-password", default=os.getenv("POSTGRES_PASSWORD", "api_lang_password"), help="Postgres password")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.requests <= 0:
        raise SystemExit("--requests must be greater than 0")
    if args.concurrency <= 0:
        raise SystemExit("--concurrency must be greater than 0")
    if args.min_lines <= 0 or args.max_lines <= 0:
        raise SystemExit("--min-lines and --max-lines must be greater than 0")
    if args.min_lines > args.max_lines:
        raise SystemExit("--min-lines cannot be greater than --max-lines")

    prefix = args.prefix or f"BENCH-POST-{int(time.time())}"
    rng = random.Random(args.seed)

    payloads = []
    lines_per_bill: list[int] = []
    for idx in range(1, args.requests + 1):
        lines_count = rng.randint(args.min_lines, args.max_lines)
        lines_per_bill.append(lines_count)
        payloads.append(build_payload(prefix, idx, lines_count, rng))

    started = time.perf_counter()
    results: list[PostResult] = []
    with ThreadPoolExecutor(max_workers=min(args.concurrency, args.requests)) as executor:
        futures = [
            executor.submit(run_single_post, args.url, args.timeout_sec, payload)
            for payload in payloads
        ]
        for future in as_completed(futures):
            results.append(future.result())
    wall_time_sec = time.perf_counter() - started

    latencies = sorted(item.latency_ms for item in results)
    successes = [item for item in results if item.ok]
    failures = [item for item in results if not item.ok]
    status_codes = Counter(str(item.status_code) if item.status_code is not None else "exception" for item in results)
    errors = Counter(item.error for item in failures if item.error is not None)
    created_ids = [item.bill_id for item in successes if item.bill_id is not None]

    report = {
        "target": {"name": args.name, "url": args.url},
        "run": {
            "prefix": prefix,
            "requests": args.requests,
            "concurrency": args.concurrency,
            "timeout_sec": args.timeout_sec,
            "min_lines": args.min_lines,
            "max_lines": args.max_lines,
            "seed": args.seed,
            "lines_per_bill": lines_per_bill,
        },
        "summary": {
            "total_requests": len(results),
            "success_count": len(successes),
            "failure_count": len(failures),
            "success_rate_pct": round((len(successes) / len(results)) * 100, 2) if results else 0.0,
            "throughput_req_per_sec": round((len(results) / wall_time_sec), 2) if wall_time_sec > 0 else 0.0,
            "total_wall_time_sec": round(wall_time_sec, 3),
            "created_count": len(created_ids),
            "created_rate_pct": round((len(created_ids) / len(results)) * 100, 2) if results else 0.0,
            "http_201_count": int(status_codes.get("201", 0)),
            "http_409_count": int(status_codes.get("409", 0)),
            "http_400_count": int(status_codes.get("400", 0)),
            "http_500_count": int(status_codes.get("500", 0)),
        },
        "latency_ms": {
            "min": round(min(latencies), 2) if latencies else 0.0,
            "avg": round(statistics.mean(latencies), 2) if latencies else 0.0,
            "median": round(statistics.median(latencies), 2) if latencies else 0.0,
            "p90": round(percentile(latencies, 90), 2) if latencies else 0.0,
            "p95": round(percentile(latencies, 95), 2) if latencies else 0.0,
            "p99": round(percentile(latencies, 99), 2) if latencies else 0.0,
            "max": round(max(latencies), 2) if latencies else 0.0,
            "stdev": round(statistics.pstdev(latencies), 2) if len(latencies) > 1 else 0.0,
        },
        "status_code_distribution": dict(status_codes),
        "error_distribution": dict(errors),
        "created_bill_ids": created_ids,
    }

    cleanup = {"attempted": False, "deleted_bill_count": 0, "deleted_bill_ids": [], "error": None}
    if not args.skip_cleanup:
        cleanup = cleanup_by_prefix(
            prefix=prefix,
            db_host=args.db_host,
            db_port=args.db_port,
            db_name=args.db_name,
            db_user=args.db_user,
            db_password=args.db_password,
        )
    report["cleanup"] = cleanup

    if args.output_json:
        with open(args.output_json, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2)

    if not args.quiet:
        print(
            f"{args.name} | success={report['summary']['success_rate_pct']}% "
            f"created={report['summary']['created_rate_pct']}% "
            f"thr={report['summary']['throughput_req_per_sec']} req/s "
            f"avg={report['latency_ms']['avg']}ms "
            f"p95={report['latency_ms']['p95']}ms "
            f"p99={report['latency_ms']['p99']}ms "
            f"deleted={cleanup['deleted_bill_count']}"
        )


if __name__ == "__main__":
    main()
