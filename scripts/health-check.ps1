. "$PSScriptRoot\common.ps1"
Import-LeadnewsEnvironment
$services = [ordered]@{
    Gateway=51601; User=51801; Article=51802; Wemedia=51803; Behavior=51804; Search=51805; Schedule=51806; Admin=51807; AI=51808
}
$failed = @()
foreach ($service in $services.GetEnumerator()) {
    try { $status=(Invoke-RestMethod "http://localhost:$($service.Value)/actuator/health" -TimeoutSec 4).status; $ok=$status -eq 'UP' }
    catch { $ok=$false }
    Write-Host ("{0,-12} {1,-5} http://localhost:{2}/actuator/health" -f $service.Key, $(if($ok){'UP'}else{'DOWN'}), $service.Value)
    if (-not $ok) { $failed += $service.Key }
}
if ($failed) { throw "Java health checks failed: $($failed -join ', ')" }

$expectedNames = @('leadnews-gateway','leadnews-user-service','leadnews-article-service','leadnews-wemedia-service','leadnews-behavior-service','leadnews-search-service','leadnews-schedule-service','leadnews-admin-service','leadnews-ai-service')
$query = 'http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=100&namespaceId=public&groupName=LEADNEWS_GROUP' +
    '&username=' + [uri]::EscapeDataString($env:NACOS_USERNAME) + '&password=' + [uri]::EscapeDataString($env:NACOS_PASSWORD)
$registered = (Invoke-RestMethod $query -TimeoutSec 10).doms
$missing = @($expectedNames | Where-Object { $_ -notin $registered })
if ($missing) { throw "Services missing from Nacos LEADNEWS_GROUP: $($missing -join ', ')" }
Write-Host "Nacos       UP    all $($expectedNames.Count) local services registered in LEADNEWS_GROUP"
