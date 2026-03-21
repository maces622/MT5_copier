param(
    [string]$RedisCliPath = "redis-cli",
    [string]$RedisHost = "127.0.0.1",
    [int]$Port = 6379,
    [string]$Password = "",
    [string]$KeyPrefix = "copy",
    [string]$RuntimeStatePrefix = "copy:runtime",
    [switch]$ClearWarmCaches,
    [switch]$ClearRuntimeState
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-Redis {
    param([string[]]$CommandArgs)

    $args = @("-h", $RedisHost, "-p", $Port)
    $args += @("--raw")
    $args += $CommandArgs

    $previousAuth = $env:REDISCLI_AUTH
    try {
        if ($Password) {
            $env:REDISCLI_AUTH = $Password
        }
        & $RedisCliPath @args
    } finally {
        if ($null -ne $previousAuth) {
            $env:REDISCLI_AUTH = $previousAuth
        } else {
            Remove-Item Env:REDISCLI_AUTH -ErrorAction SilentlyContinue
        }
    }
}

function Remove-KeysByPattern {
    param([string]$Pattern)

    $scanArgs = @("-h", $RedisHost, "-p", $Port)
    $scanArgs += @("--scan", "--pattern", $Pattern)

    $previousAuth = $env:REDISCLI_AUTH
    try {
        if ($Password) {
            $env:REDISCLI_AUTH = $Password
        }
        $keys = @(& $RedisCliPath @scanArgs | Where-Object { $_ })
    } finally {
        if ($null -ne $previousAuth) {
            $env:REDISCLI_AUTH = $previousAuth
        } else {
            Remove-Item Env:REDISCLI_AUTH -ErrorAction SilentlyContinue
        }
    }
    if (-not $keys -or $keys.Count -eq 0) {
        Write-Host "No keys matched pattern: $Pattern"
        return 0
    }

    $deleted = 0
    for ($index = 0; $index -lt $keys.Count; $index += 100) {
        $endIndex = [Math]::Min($index + 99, $keys.Count - 1)
        $batch = $keys[$index..$endIndex]
        $command = @("DEL") + $batch
        $result = Invoke-Redis -CommandArgs $command
        if ($result) {
            $deleted += [int]$result
        }
    }

    Write-Host "Deleted $deleted key(s) for pattern: $Pattern"
    return $deleted
}

$patterns = @(
    "${KeyPrefix}:ws:*",
    "${KeyPrefix}:signal:dedup:*"
)

if ($ClearWarmCaches) {
    $patterns += @(
        "${KeyPrefix}:route:*",
        "${KeyPrefix}:account:*"
    )
}

if ($ClearRuntimeState) {
    $patterns += @(
        "${RuntimeStatePrefix}:*"
    )
}

$totalDeleted = 0
foreach ($pattern in $patterns) {
    $totalDeleted += Remove-KeysByPattern -Pattern $pattern
}

Write-Host ""
Write-Host "Recovery cleanup completed. Deleted $totalDeleted key(s) in total."
Write-Host "Recommended next step:"
Write-Host "1. Restart the Java service."
Write-Host "2. Let warmup rebuild route/risk/account-binding caches."
Write-Host "3. Wait for fresh MT5 HELLO/HEARTBEAT before resuming ratio-copy if runtime-state was cleared or stale."
