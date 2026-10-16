Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $projectRoot '.env'
function New-LocalSecret {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return ([BitConverter]::ToString($bytes)).Replace('-', '').ToLowerInvariant()
}
if (!(Test-Path -LiteralPath $envFile)) {
    $content = Get-Content -LiteralPath (Join-Path $projectRoot '.env.example') -Raw -Encoding UTF8
    foreach ($name in @('ALICE_API_TOKEN', 'BOB_API_TOKEN', 'COMMERCE_SERVICE_TOKEN', 'DB_PASSWORD')) {
        $content = [regex]::Replace($content, "(?m)^$name=\r?$", "$name=$(New-LocalSecret)")
    }
    [IO.File]::WriteAllText($envFile, $content, (New-Object System.Text.UTF8Encoding $false))
} else {
    Write-Host '.env already exists; existing configuration was preserved.'
}
. (Join-Path $PSScriptRoot 'Environment.ps1')
Import-ProjectEnv $projectRoot
$httpFile = Join-Path $projectRoot 'http-client.private.env.json'
if (!(Test-Path -LiteralPath $httpFile)) {
    $httpEnv = @{local=@{baseUrl="http://localhost:$env:SERVER_PORT"; aliceToken=$env:ALICE_API_TOKEN; bobToken=$env:BOB_API_TOKEN}}
    [IO.File]::WriteAllText($httpFile, ($httpEnv | ConvertTo-Json -Depth 4), (New-Object System.Text.UTF8Encoding $false))
}
Write-Host 'Local configuration and IntelliJ private HTTP environment are ready. Secrets were not printed.'
Write-Host 'Commerce API can start now. Set MODEL_API_KEY in .env for real LLM calls.'
