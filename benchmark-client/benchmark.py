#!/usr/bin/env python3
import argparse
import json
import math
import statistics
import threading
import time
import warnings
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from typing import Any, Optional

# Suppress urllib3 LibreSSL runtime warning on macOS system Python.
warnings.filterwarnings(
    "ignore",
    message=r"urllib3 v2 only supports OpenSSL 1\.1\.1\+",
    category=Warning,
)

import requests


thread_local = threading.local()


@dataclass
class RequestResult:
    ok: bool
    status_code: Optional[int]
    latency_ms: float
    response_bytes: int
    error: Optional[str]


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


def run_single_request(
    url: str,
    method: str,
    timeout_sec: float,
    headers: dict[str, str],
    payload: Optional[dict[str, Any]],
) -> RequestResult:
    session = get_session()
    started = time.perf_counter()
    try:
        response = session.request(
            method=method,
            url=url,
            timeout=timeout_sec,
            headers=headers,
            json=payload,
        )
        latency_ms = (time.perf_counter() - started) * 1000
        body = response.content or b""
        return RequestResult(
            ok=200 <= response.status_code < 300,
            status_code=response.status_code,
            latency_ms=latency_ms,
            response_bytes=len(body),
            error=None,
        )
    except requests.RequestException as exc:
        latency_ms = (time.perf_counter() - started) * 1000
        return RequestResult(
            ok=False,
            status_code=None,
            latency_ms=latency_ms,
            response_bytes=0,
            error=type(exc).__name__,
        )


def build_report(results: list[RequestResult], wall_time_sec: float) -> dict[str, Any]:
    latencies = sorted(r.latency_ms for r in results)
    successes = [r for r in results if r.ok]
    failures = [r for r in results if not r.ok]
    sizes = [r.response_bytes for r in successes]
    status_codes = Counter(
        str(r.status_code) if r.status_code is not None else "exception" for r in results
    )
    errors = Counter(r.error for r in failures if r.error is not None)

    report = {
        "summary": {
            "total_requests": len(results),
            "success_count": len(successes),
            "failure_count": len(failures),
            "success_rate_pct": round((len(successes) / len(results)) * 100, 2) if results else 0.0,
            "throughput_req_per_sec": round((len(results) / wall_time_sec), 2) if wall_time_sec > 0 else 0.0,
            "total_wall_time_sec": round(wall_time_sec, 3),
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
        "response_size_bytes_success_only": {
            "min": min(sizes) if sizes else 0,
            "avg": round(statistics.mean(sizes), 2) if sizes else 0.0,
            "max": max(sizes) if sizes else 0,
        },
        "status_code_distribution": dict(status_codes),
        "error_distribution": dict(errors),
    }
    return report


def parse_headers(values: list[str]) -> dict[str, str]:
    headers: dict[str, str] = {}
    for value in values:
        if ":" not in value:
            raise ValueError(f"Invalid header '{value}'. Use 'Header: Value'.")
        name, header_value = value.split(":", 1)
        headers[name.strip()] = header_value.strip()
    return headers


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Simple API benchmark client")
    parser.add_argument("--url", required=True, help="Endpoint URL to test")
    parser.add_argument("--method", default="GET", help="HTTP method (GET, POST, ...)")
    parser.add_argument("--requests", type=int, default=100, help="Total number of requests")
    parser.add_argument("--concurrency", type=int, default=10, help="Number of concurrent workers")
    parser.add_argument("--timeout-sec", type=float, default=10.0, help="Request timeout in seconds")
    parser.add_argument("--header", action="append", default=[], help="Header as 'Name: Value'")
    parser.add_argument("--payload-file", help="JSON payload file path for request body")
    parser.add_argument("--warmup-requests", type=int, default=5, help="Warm-up requests before benchmark")
    parser.add_argument("--output-json", help="Optional path to write full report JSON")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.requests <= 0:
        raise SystemExit("--requests must be greater than 0")
    if args.concurrency <= 0:
        raise SystemExit("--concurrency must be greater than 0")
    if args.warmup_requests < 0:
        raise SystemExit("--warmup-requests must be 0 or greater")

    headers = parse_headers(args.header)
    payload = None
    if args.payload_file:
        with open(args.payload_file, "r", encoding="utf-8") as f:
            payload = json.load(f)

    method = args.method.upper()

    for _ in range(args.warmup_requests):
        run_single_request(args.url, method, args.timeout_sec, headers, payload)

    started = time.perf_counter()
    results: list[RequestResult] = []
    with ThreadPoolExecutor(max_workers=args.concurrency) as executor:
        futures = [
            executor.submit(
                run_single_request,
                args.url,
                method,
                args.timeout_sec,
                headers,
                payload,
            )
            for _ in range(args.requests)
        ]
        for future in as_completed(futures):
            results.append(future.result())
    wall_time_sec = time.perf_counter() - started

    report = build_report(results, wall_time_sec)
    print(json.dumps(report, indent=2))

    if args.output_json:
        with open(args.output_json, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2)
        print(f"\nReport written to {args.output_json}")


if __name__ == "__main__":
    main()
