param([ValidateSet('local','postgres')][string]$Profile = 'local')
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'Environment.ps1')
Import-ProjectEnv $projectRoot
foreach ($name in @('java','mvn')) {
    if (!(Get-Command $name -ErrorAction SilentlyContinue)) { throw "Required command unavailable: $name" }
}
Push-Location $projectRoot
try {
    if ($Profile -eq 'postgres') { & mvn -B -ntp spring-boot:run '-Dspring-boot.run.profiles=postgres' }
    else { & mvn -B -ntp spring-boot:run }
    if ($LASTEXITCODE -ne 0) { throw "Agent process exited: $LASTEXITCODE" }
} finally { Pop-Location }
