
# Product API Redis Performance Benchmark

## Purpose

Measure the local HTTP response latency of the Product Service
when the product-list cache is empty versus when the result is
already cached in Redis.

## Requirements

- Windows PowerShell 5.1 or later
- Java 21 and the Product Service running on port 8081
- Docker Desktop running
- Redis container named `redis_container`
- PostgreSQL available to the Product Service
- The product-list cache configured with the key `products::allActive`

## Run the benchmark

Open PowerShell in the repository root and execute:

```powershell
.\performance-tests\benchmark.ps1
```

The default run performs 30 warm-up pairs and 1,000 measured pairs.

To run a shorter test:

```powershell
.\performance-tests\benchmark.ps1 -Iterations 100 -WarmupPairs 10
```

## Output files

The script creates a `results` directory automatically.

- `measurements_<timestamp>.csv`: individual cache-miss and
  cache-hit request measurements.
- `summary_<timestamp>.csv`: average, median, p95, minimum,
  maximum, and latency-reduction percentages.

Each measured pair consists of a cache-miss request followed by
a cache-hit request to the same endpoint.

## Metrics

- Average latency: arithmetic mean of the measured request times.
- Median latency: middle observation after sorting.
- p95 latency: latency at the 95th percentile.
- Average latency reduction:

  (miss average - hit average) / miss average * 100

- Median latency reduction:

  (miss median - hit median) / miss median * 100

## Important limitations

This is a local, sequential endpoint benchmark, not a production
load test. The measurements include HTTP overhead, Spring Boot
processing, Redis operations, and database access on cache misses.

The cache-miss path is verified by deleting the expected Redis key
before each request and checking that the key is populated afterward.
The cache-hit path is requested immediately afterward.

Run multiple independent benchmark sessions and compare the results
before using a percentage in a resume. Do not interpret these results
as a guarantee of performance under concurrent production traffic.
