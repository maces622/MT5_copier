param(
    [string]$RedisCliPath = "redis-cli",
    [string]$RedisHost = "127.0.0.1",
    [int]$Port = 6379,
    [string]$Password = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-RedisRaw {
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

function Get-ConfigValue {
    param([string]$Name)

    $lines = @(Invoke-RedisRaw @("CONFIG", "GET", $Name))
    if ($lines.Count -ge 2) {
        return $lines[1]
    }
    return $null
}

$dir = Get-ConfigValue -Name "dir"
$dbFilename = Get-ConfigValue -Name "dbfilename"
$appendOnly = Get-ConfigValue -Name "appendonly"
$appendDir = Get-ConfigValue -Name "appenddirname"
$appendFilename = Get-ConfigValue -Name "appendfilename"

Write-Host "Triggering Redis background snapshot..."
Invoke-RedisRaw @("BGSAVE") | Out-Host

$lastSave = Invoke-RedisRaw @("LASTSAVE")
$persistenceInfo = Invoke-RedisRaw @("INFO", "persistence")

Write-Host ""
Write-Host "Redis persistence summary"
Write-Host "dir               = $dir"
Write-Host "dbfilename        = $dbFilename"
Write-Host "appendonly        = $appendOnly"
Write-Host "appenddirname     = $appendDir"
Write-Host "appendfilename    = $appendFilename"
Write-Host "lastsave(epoch)   = $lastSave"
Write-Host ""
Write-Host "INFO persistence"
$persistenceInfo | Out-Host
Write-Host ""
Write-Host "Next step:"
Write-Host "1. Copy the RDB/AOF files from the Redis persistence directory."
Write-Host "2. Backup MariaDB together with Redis; MariaDB remains the business source of truth."
