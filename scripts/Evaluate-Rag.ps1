param([string[]]$Modes = @('BM25','VECTOR','HYBRID'))
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'Environment.ps1')
Import-ProjectEnv $projectRoot
$headers = @{Authorization="Bearer $env:ALICE_API_TOKEN"}
$baseUrl = "http://localhost:$env:SERVER_PORT"
function Invoke-Json([string]$Path, [object]$Body) {
    $requestOptions = @{Uri=($baseUrl+$Path); Headers=$headers; UseBasicParsing=$true; Method='Get'}
    if($null -ne $Body) { $requestOptions.Method='Post'; $requestOptions.ContentType='application/json; charset=utf-8'; $requestOptions.Body=[Text.Encoding]::UTF8.GetBytes(($Body|ConvertTo-Json)) }
    $response=Invoke-WebRequest @requestOptions
    return ([Text.Encoding]::UTF8.GetString($response.RawContentStream.ToArray())|ConvertFrom-Json)
}
$status=Invoke-Json '/api/knowledge/status' $null
# Direct assignment keeps a JSON root array flat in both Windows PowerShell 5.1 and PowerShell 7.
$cases=ConvertFrom-Json -InputObject (Get-Content -LiteralPath (Join-Path $projectRoot 'src/test/resources/evaluation/retrieval-cases.json') -Raw -Encoding UTF8)
$details=@(); $summaries=@()
foreach($mode in $Modes) {
    if($mode -notin @('BM25','VECTOR','HYBRID')) {throw 'Supported evaluation modes: BM25,VECTOR,HYBRID'}
    if($mode -ne 'BM25' -and !$status.vectorAvailable) {
        $summaries += [pscustomobject]@{mode=$mode;executed=$false;reason=$status.fallbackReason}
        continue
    }
    $recall3=0.0; $recall5=0.0; $mrr=0.0; $sourceRecall5=0.0; $known=0; $unknown=0; $unknownRejected=0; $knownRejected=0
    $valid=$true
    foreach($case in $cases) {
        $result=Invoke-Json '/api/knowledge/search' @{query=$case.query;topK=5;mode=$mode}
        $expectedMode=if($mode -eq 'BM25') {'BM25_ONLY'} else {$mode}
        if($result.mode -ne $expectedMode) { $valid=$false }
        $ids=@($result.evidence|ForEach-Object {$_.chunkId}); $sources=@($result.evidence|ForEach-Object {$_.sourceId}|Select-Object -Unique)
        $expected=@($case.expectedChunkIds)
        if($expected.Count -eq 0) {
            $unknown++; if($result.code -eq 'NO_RELEVANT_EVIDENCE') {$unknownRejected++}
        } else {
            $known++; if($result.code -eq 'NO_RELEVANT_EVIDENCE') {$knownRejected++}
            $top3=@($ids|Select-Object -First 3)
            $recall3 += @($expected|Where-Object {$_ -in $top3}).Count/[double]$expected.Count
            $recall5 += @($expected|Where-Object {$_ -in $ids}).Count/[double]$expected.Count
            $sourceRecall5 += @($case.expectedSourceIds|Where-Object {$_ -in $sources}).Count/[double]@($case.expectedSourceIds).Count
            for($i=0;$i -lt $ids.Count;$i++) {if($ids[$i] -in $expected) {$mrr+=1.0/($i+1);break}}
        }
        $details += [pscustomobject]@{id=$case.id;query=$case.query;requestedMode=$mode;actualMode=$result.mode;code=$result.code;expected=$expected;hits=$ids}
    }
    if(!$valid) { $summaries += [pscustomobject]@{mode=$mode;executed=$false;reason='FALLBACK_OCCURRED_NO_COMPARABLE_METRICS'};continue }
    $summaries += [pscustomobject]@{mode=$mode;executed=$true;answerable=$known;noAnswer=$unknown;recallAt3=$recall3/$known;recallAt5=$recall5/$known;mrr=$mrr/$known;sourceRecallAt5=$sourceRecall5/$known;noAnswerCorrect=$unknownRejected;noAnswerTotal=$unknown;falseAbstentions=$knownRejected}
}
$date=Get-Date -Format 'yyyyMMdd'
$outDir=Join-Path $projectRoot "output/${date}_P1开发记录_v01"
New-Item -ItemType Directory -Path $outDir -Force|Out-Null
$version=1
do {$report=Join-Path $outDir ("${date}_检索评测_v{0:D2}.md" -f $version);$version++} while(Test-Path -LiteralPath $report)
$lines=@('# RAG 检索评测实际结果', '', "Corpus hash: $($status.corpusHash)","Chunks: $($status.chunks); embedding model: $($status.model); dimensions: $($status.dimensions); vector available: $($status.vectorAvailable)","Cases: $($cases.Count); evaluated via actual HTTP API. Scores are not LLM answer-quality scores.",'', '| Mode | Executed | Recall@3 | Recall@5 | MRR | Source Recall@5 | No-answer correct | False abstentions |','|---|---|---|---|---|---|---|---|')
foreach($s in $summaries) {
    if($s.executed) {$lines += ('| {0} | yes | {1:F4} | {2:F4} | {3:F4} | {4:F4} | {5}/{6} | {7} |' -f $s.mode,$s.recallAt3,$s.recallAt5,$s.mrr,$s.sourceRecallAt5,$s.noAnswerCorrect,$s.noAnswerTotal,$s.falseAbstentions)}
    else {$lines += "| $($s.mode) | no: $($s.reason) | - | - | - | - | - | - |"}
}
$lines += @('', 'Recall denominators contain answerable cases only; multiple expected chunks use per-query fractional recall. MRR uses the first relevant chunk. No-answer cases are evaluated separately. This is a small, authored development set without a held-out test set.', '', '| Case | Mode | Code | Expected chunks | Retrieved chunks |','|---|---|---|---|---|')
foreach($d in $details) {$lines += "| $($d.id) | $($d.actualMode) | $($d.code) | $($d.expected -join ', ') | $($d.hits -join ', ') |"}
$lines|Set-Content -LiteralPath $report -Encoding UTF8
@{index=$status;summaries=$summaries;details=$details}|ConvertTo-Json -Depth 12|Set-Content -LiteralPath (Join-Path $outDir ('rag-evaluation-'+(Get-Date -Format 'yyyyMMddHHmmss')+'.json')) -Encoding UTF8
$summaries|ConvertTo-Json -Depth 5
Write-Host "Report: $($report.Replace('\','/'))"
