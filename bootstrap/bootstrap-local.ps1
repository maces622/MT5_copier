param(
    [Parameter(Mandatory = $false)]
    [string]$ConfigPath = ".\bootstrap\local.example.json"
)

$resolvedConfig = (Resolve-Path $ConfigPath).Path
$env:SPRING_PROFILES_ACTIVE = "local"
$env:COPIER_BOOTSTRAP_CONFIG_FILE = $resolvedConfig
$env:COPIER_BOOTSTRAP_EXIT_ON_COMPLETE = "true"
$env:COPIER_ACCOUNT_CONFIG_ROUTE_CACHE_BACKEND = "log"

cmd /c ".\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=0"
