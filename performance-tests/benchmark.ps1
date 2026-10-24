
[CmdletBinding()]
param(
    [ValidateRange(10, 10000)]
    [int]$Iterations = 1000,

    [ValidateRange(0, 500)]
    [int]$WarmupPairs = 30
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Net.Http

$uri = "http://localhost:8081/api/products"
$cacheKey = "products::allActive"
$redisContainer = "redis_container"
$resultsDir = Join-Path $PSScriptRoot "results"

if (-not (Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Path $resultsDir -Force | Out-Null
}

$runId = Get-Date -Format "yyyyMMdd_HHmmss"
$rawFile = Join-Path $resultsDir "measurements_$runId.csv"
$summaryFile = Join-Path $resultsDir "summary_$runId.csv"

$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(15)

$measurements = [System.Collections.Generic.List[object]]::new()

function Invoke-TimedRequest {
    param([string]$Url)

    $response = $null
    $timer = [System.Diagnostics.Stopwatch]::StartNew()

    try {
        $response = $client.GetAsync($Url).GetAwaiter().GetResult()

        # Read the full body so timing includes response transfer.
        $null = $response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()

        $timer.Stop()

        return [PSCustomObject]@{
            LatencyMs = $timer.Elapsed.TotalMilliseconds
            StatusCode = [int]$response.StatusCode
        }
    }
    finally {
        if ($null -ne $response) {
            $response.Dispose()
        }
    }
}

function Clear-ProductCache {
    $output = docker exec $redisContainer redis-cli --raw DEL $cacheKey

    if ($LASTEXITCODE -ne 0) {
        throw "Could not delete Redis key '$cacheKey'. Check Docker and Redis."
    }
}

function Test-ProductCacheExists {
    $output = docker exec $redisContainer redis-cli --raw EXISTS $cacheKey

    if ($LASTEXITCODE -ne 0) {
        throw "Could not verify Redis key '$cacheKey'."
    }

    return ([string]($output | Out-String)).Trim() -eq "1"
}

function Get-Stats {
    param(
        [string]$Mode,
        [double[]]$Values
    )

    $sorted = @($Values | Sort-Object)
    $count = $sorted.Count

    if ($count -eq 0) {
        throw "No measurements recorded for $Mode."
    }

    $median = if ($count % 2 -eq 1) {
        $sorted[[int][Math]::Floor($count / 2)]
    } else {
        ($sorted[($count / 2) - 1] + $sorted[$count / 2]) / 2
    }

    $p95Index = [Math]::Max(0, [Math]::Ceiling(0.95 * $count) - 1)

    return [PSCustomObject]@{
        Mode       = $Mode
        Count      = $count
        AverageMs  = ($Values | Measure-Object -Average).Average
        MedianMs   = $median
        P95Ms      = $sorted[$p95Index]
        MinMs      = $sorted[0]
        MaxMs      = $sorted[$count - 1]
    }
}

try {
    Write-Host "Checking Product Service..." -ForegroundColor Cyan

    $check = Invoke-TimedRequest -Url $uri
    if ($check.StatusCode -ne 200) {
        throw "Product endpoint returned HTTP $($check.StatusCode), expected 200."
    }

    Write-Host "Checking Redis container..."
    $ping = docker exec $redisContainer redis-cli --raw PING

    if ($LASTEXITCODE -ne 0 -or ([string]($ping | Out-String)).Trim() -ne "PONG") {
        throw "Redis health check failed."
    }

    # Confirm the endpoint really populates the expected cache key.
    Clear-ProductCache
    $null = Invoke-TimedRequest -Url $uri

    if (-not (Test-ProductCacheExists)) {
        throw "The product-list cache key was not created. Stop: verify caching before benchmarking."
    }

    Write-Host "API and Redis checks passed." -ForegroundColor Green
    Write-Host "Warm-up pairs: $WarmupPairs"
    Write-Host "Measured pairs: $Iterations"

    # Warm up the application and cache paths without recording results.
    for ($i = 1; $i -le $WarmupPairs; $i++) {
        Clear-ProductCache

        $miss = Invoke-TimedRequest -Url $uri
        if ($miss.StatusCode -ne 200) {
            throw "Warm-up cache-miss request returned HTTP $($miss.StatusCode)."
        }

        if (-not (Test-ProductCacheExists)) {
            throw "Cache key was not created during warm-up."
        }

        $hit = Invoke-TimedRequest -Url $uri
        if ($hit.StatusCode -ne 200) {
            throw "Warm-up cache-hit request returned HTTP $($hit.StatusCode)."
        }

        if (($i % 10) -eq 0) {
            Write-Progress -Activity "Warming up" -Status "$i of $WarmupPairs pairs"
        }
    }

    # Measured pairs: one confirmed cache miss, followed by one confirmed hit.
    for ($i = 1; $i -le $Iterations; $i++) {
        Clear-ProductCache

        if (Test-ProductCacheExists) {
            throw "Cache key still exists before iteration $i."
        }

        $miss = Invoke-TimedRequest -Url $uri
        if ($miss.StatusCode -ne 200) {
            throw "Cache-miss request $i returned HTTP $($miss.StatusCode)."
        }

        if (-not (Test-ProductCacheExists)) {
            throw "Cache key was not created after cache-miss request $i."
        }

        $hit = Invoke-TimedRequest -Url $uri
        if ($hit.StatusCode -ne 200) {
            throw "Cache-hit request $i returned HTTP $($hit.StatusCode)."
        }

        $now = (Get-Date).ToString("o")

        $measurements.Add([PSCustomObject]@{
            RunId      = $runId
            Iteration  = $i
            CacheMode  = "Miss"
            LatencyMs  = [Math]::Round($miss.LatencyMs, 3)
            StatusCode = $miss.StatusCode
            Timestamp  = $now
        })

        $measurements.Add([PSCustomObject]@{
            RunId      = $runId
            Iteration  = $i
            CacheMode  = "Hit"
            LatencyMs  = [Math]::Round($hit.LatencyMs, 3)
            StatusCode = $hit.StatusCode
            Timestamp  = $now
        })

        if (($i % 100) -eq 0) {
            Write-Progress -Activity "Benchmarking" -Status "$i of $Iterations pairs" -PercentComplete (($i / $Iterations) * 100)
        }
    }

    $measurements | Export-Csv -Path $rawFile -NoTypeInformation -Encoding UTF8

    $missValues = @(
        $measurements |
            Where-Object { $_.CacheMode -eq "Miss" } |
            ForEach-Object { [double]$_.LatencyMs }
    )

    $hitValues = @(
        $measurements |
            Where-Object { $_.CacheMode -eq "Hit" } |
            ForEach-Object { [double]$_.LatencyMs }
    )

    $missStats = Get-Stats -Mode "CacheMiss" -Values $missValues
    $hitStats = Get-Stats -Mode "CacheHit" -Values $hitValues

    $avgReduction = (($missStats.AverageMs - $hitStats.AverageMs) / $missStats.AverageMs) * 100
    $medianReduction = (($missStats.MedianMs - $hitStats.MedianMs) / $missStats.MedianMs) * 100

    $summary = @(
        [PSCustomObject]@{
            RunId = $runId
            Mode = $missStats.Mode
            Count = $missStats.Count
            AverageMs = [Math]::Round($missStats.AverageMs, 3)
            MedianMs = [Math]::Round($missStats.MedianMs, 3)
            P95Ms = [Math]::Round($missStats.P95Ms, 3)
            MinMs = [Math]::Round($missStats.MinMs, 3)
            MaxMs = [Math]::Round($missStats.MaxMs, 3)
            AverageLatencyReductionPercent = ""
            MedianLatencyReductionPercent = ""
        },
        [PSCustomObject]@{
            RunId = $runId
            Mode = $hitStats.Mode
            Count = $hitStats.Count
            AverageMs = [Math]::Round($hitStats.AverageMs, 3)
            MedianMs = [Math]::Round($hitStats.MedianMs, 3)
            P95Ms = [Math]::Round($hitStats.P95Ms, 3)
            MinMs = [Math]::Round($hitStats.MinMs, 3)
            MaxMs = [Math]::Round($hitStats.MaxMs, 3)
            AverageLatencyReductionPercent = [Math]::Round($avgReduction, 2)
            MedianLatencyReductionPercent = [Math]::Round($medianReduction, 2)
        }
    )

    $summary | Export-Csv -Path $summaryFile -NoTypeInformation -Encoding UTF8

    Write-Host "`n========== BENCHMARK SUMMARY ==========" -ForegroundColor Cyan
    $summary | Format-Table Mode, Count, AverageMs, MedianMs, P95Ms, MinMs, MaxMs -AutoSize
    Write-Host "Average latency reduction: $([Math]::Round($avgReduction, 2))%"
    Write-Host "Median latency reduction:  $([Math]::Round($medianReduction, 2))%"
    Write-Host "`nRaw measurements: $rawFile"
    Write-Host "Summary:           $summaryFile"
}
finally {
    $client.Dispose()
}
