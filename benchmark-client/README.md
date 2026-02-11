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

`run_compare.sh` now executes multiple rounds with randomized endpoint order and reports median metrics.

```bash
./run_compare.sh
```

Optional overrides:
```bash
REQUESTS=1000 CONCURRENCY=50 WARMUP_REQUESTS=50 ROUNDS=7 ./run_compare.sh
```

Custom URLs:
```bash
DOTNET_MINIMAL_URL=http://localhost:5080/bills-minimal \
DOTNET_DDD_URL=http://localhost:5080/bills \
PYTHON_MINIMAL_URL=http://localhost:5081/bills-minimal \
PYTHON_DDD_URL=http://localhost:5081/bills \
GO_MINIMAL_URL=http://localhost:5082/bills-minimal \
GO_DDD_URL=http://localhost:5082/bills \
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
