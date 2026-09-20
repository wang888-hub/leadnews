. "$PSScriptRoot\common.ps1"
Import-LeadnewsEnvironment

$checks = [ordered]@{
    MySQL = { Test-TcpPort 3307 }
    Redis = { Test-TcpPort 6379 }
    Nacos = { (Invoke-RestMethod 'http://localhost:8080/v3/console/health/readiness' -TimeoutSec 5).code -eq 0 }
    Kafka = { Test-TcpPort 9092 }
    Elasticsearch = { (Invoke-RestMethod 'http://localhost:9201/' -TimeoutSec 5).version.number -ne $null }
    'MinIO API' = { (Invoke-WebRequest -UseBasicParsing 'http://localhost:9000/minio/health/ready' -TimeoutSec 5).StatusCode -eq 200 }
    'XXL-Job Admin' = { (Invoke-WebRequest -UseBasicParsing 'http://localhost:8088/xxl-job-admin/' -TimeoutSec 5).StatusCode -eq 200 }
}
$failed = @()
foreach ($item in $checks.GetEnumerator()) {
    try { $ok = [bool](& $item.Value) } catch { $ok = $false }
    Write-Host ("{0,-18} {1}" -f $item.Key, $(if ($ok) { 'READY' } else { 'FAILED' }))
    if (-not $ok) { $failed += $item.Key }
}
if ($failed) { throw "Infrastructure checks failed: $($failed -join ', ')" }
