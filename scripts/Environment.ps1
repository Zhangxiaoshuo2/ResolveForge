Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Import-ProjectEnv {
    param([string]$ProjectRoot)
    $envFile = Join-Path $ProjectRoot '.env'
    if (!(Test-Path -LiteralPath $envFile)) { throw 'Missing .env. Run ./scripts/Initialize-Local.ps1 first.' }
    foreach ($line in Get-Content -LiteralPath $envFile -Encoding UTF8) {
        $trimmed = $line.Trim()
        if (!$trimmed -or $trimmed.StartsWith('#')) { continue }
        $pair = $trimmed -split '=', 2
        if ($pair.Length -ne 2 -or $pair[0] -notmatch '^[A-Z][A-Z0-9_]*$') { throw 'Invalid .env line. Use KEY=value without shell expressions.' }
        $value = $pair[1].Trim()
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($pair[0], $value, 'Process')
    }
}
