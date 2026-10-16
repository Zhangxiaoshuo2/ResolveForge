Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
foreach ($name in @('java','javac','mvn','git','docker')) {
    $command = Get-Command $name -ErrorAction SilentlyContinue
    if ($command) { Write-Host "$name available: $($command.Source.Replace('\','/'))" }
    else { Write-Host "$name unavailable" }
}
if (Get-Command java -ErrorAction SilentlyContinue) { & java -version }
if (Get-Command mvn -ErrorAction SilentlyContinue) { & mvn -version }
if (Get-Command docker -ErrorAction SilentlyContinue) { & docker compose version }
Write-Host 'Docker is optional for P0. Default database is local H2.'
