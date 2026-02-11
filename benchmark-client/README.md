# benchmark-client

Python client for API comparison benchmarks.

## Metrics captured
- Latency: min, avg, median, p90, p95, p99, max, stdev
- Throughput: requests per second
- Reliability: success rate, failure count, error distribution
- Status code distribution
- Response size stats (success responses)

## Setup
```bash
cd benchmark-client
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Run
```bash
python benchmark.py \
  --url http://localhost:8000/health \
  --requests 200 \
  --concurrency 20 \
  --warmup-requests 10 \
  --output-json ./report.json
```

## Compare APIs
Runs all APIs with the same load profile and prints a side-by-side summary:
- `.NET Minimal`: `/bills-minimal`
- `.NET DDD`: `/bills`
- `Python Minimal`: `/bills-minimal`
- `Python DDD`: `/bills`
- `Go Minimal`: `/bills-minimal`
- `Go DDD`: `/bills`
- `Kotlin Minimal`: `/bills-minimal`
- `Kotlin DDD`: `/bills`

`run_compare.sh` executes multiple rounds with randomized endpoint order and reports median GET metrics.
It can also run POST benchmarks in table format across configured targets (`.NET`, `Python`, `Go`, `Kotlin`), creating tagged bills and auto-cleaning them.

```bash
./run_compare.sh
```

Optional overrides:
```bash
REQUESTS=1000 CONCURRENCY=50 WARMUP_REQUESTS=50 ROUNDS=7 ./run_compare.sh
```

Disable POST benchmark:
```bash
RUN_POST_BENCHMARK=0 ./run_compare.sh
```

Tune POST benchmark:
```bash
POST_ROUNDS=3 \
POST_REQUESTS=10 \
POST_CONCURRENCY=5 \
POST_MIN_LINES=10 \
POST_MAX_LINES=15 \
POST_DB_HOST=localhost \
POST_DB_PORT=5440 \
POST_DOTNET_URL=http://localhost:5080/bills \
POST_PYTHON_URL=http://localhost:5081/bills \
POST_GO_URL=http://localhost:5082/bills \
POST_KOTLIN_URL=http://localhost:5083/bills \
./run_compare.sh
```
By default, POST comparison uses `POST_ROUNDS=5` with shuffled language order per round and reports median metrics.

Custom URLs:
```bash
DOTNET_MINIMAL_URL=http://localhost:5080/bills-minimal \
DOTNET_DDD_URL=http://localhost:5080/bills \
PYTHON_MINIMAL_URL=http://localhost:5081/bills-minimal \
PYTHON_DDD_URL=http://localhost:5081/bills \
GO_MINIMAL_URL=http://localhost:5082/bills-minimal \
GO_DDD_URL=http://localhost:5082/bills \
KOTLIN_MINIMAL_URL=http://localhost:5083/bills-minimal \
KOTLIN_DDD_URL=http://localhost:5083/bills \
./run_compare.sh
```

## Optional payload and headers
```bash
python benchmark.py \
  --url http://localhost:8000/api/bills \
  --method POST \
  --header "Content-Type: application/json" \
  --header "Authorization: Bearer TOKEN" \
  --payload-file ./payload.json
```

## Dedicated POST benchmark runner
```bash
python benchmark_post.py \
  --name ".NET-Post" \
  --url http://localhost:5080/bills \
  --requests 10 \
  --concurrency 5 \
  --min-lines 10 \
  --max-lines 15 \
  --db-host localhost \
  --db-port 5440 \
  --db-name api_lang_arena \
  --db-user api_lang_user \
  --db-password api_lang_password
```

It generates unique `billNumber`/`customerName` values with a benchmark prefix, then deletes created rows after the run.

Default POST targets in `run_compare.sh`:
- `.NET`: enabled (`POST_DOTNET_URL=http://localhost:5080/bills`)
- `Python`: enabled (`POST_PYTHON_URL=http://localhost:5081/bills`)
- `Go`: enabled (`POST_GO_URL=http://localhost:5082/bills`)
- `Kotlin`: enabled (`POST_KOTLIN_URL=http://localhost:5083/bills`)

## Metric caveat
- GET metrics are useful for read-path comparison across stacks.
- For POST with only 10 requests, `p95` and `p99` are directionally useful but statistically noisy. Use larger request counts (for example 100-500) when you need stable tail-latency conclusions.
