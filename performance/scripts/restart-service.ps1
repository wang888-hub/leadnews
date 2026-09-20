param([Parameter(Mandatory=$true)][string]$Service,[Parameter(Mandatory=$true)][string]$JavaHome)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
. (Join-Path $root 'scripts\common.ps1')
Import-LeadnewsEnvironment
$recordPath=Join-Path $root ".stage14-runtime\backend\$Service.json"
$record=Get-Content -Raw $recordPath|ConvertFrom-Json
$process=Get-Process -Id $record.pid -ErrorAction SilentlyContinue
$sw=[Diagnostics.Stopwatch]::StartNew()
if($process){
  if($process.Path-ne$record.executable-or$process.StartTime.ToString('o')-ne([datetime]$record.startedAt).ToString('o')){throw 'PID safety check failed'}
  Stop-Process -Id $record.pid
  $deadline=(Get-Date).AddSeconds(20);while((Get-Process -Id $record.pid -ErrorAction SilentlyContinue)-and(Get-Date)-lt$deadline){Start-Sleep -Milliseconds 200}
  if(Get-Process -Id $record.pid -ErrorAction SilentlyContinue){throw 'Service did not stop within 20 seconds'}
} elseif(Test-TcpPort $record.port){throw 'Recorded PID is gone but its port is occupied; refusing to continue'}
Remove-Item $recordPath -Force
$query='http://localhost:8848/nacos/v1/ns/instance/list?serviceName='+[uri]::EscapeDataString($Service)+'&groupName=LEADNEWS_GROUP&namespaceId=public&healthyOnly=true&username='+[uri]::EscapeDataString($env:NACOS_USERNAME)+'&password='+[uri]::EscapeDataString($env:NACOS_PASSWORD)
$deregistered=$false;$deadline=(Get-Date).AddSeconds(30)
do{Start-Sleep -Seconds 1;try{$instances=(Invoke-RestMethod $query -TimeoutSec 3).hosts;$deregistered=@($instances).Count-eq 0}catch{}}while(!$deregistered-and(Get-Date)-lt$deadline)
& (Join-Path $root 'scripts\start-backend.ps1') -JavaHome $JavaHome | Out-Null
$newRecord=Get-Content -Raw $recordPath|ConvertFrom-Json
$health=(Invoke-RestMethod "http://localhost:$($newRecord.port)/actuator/health" -TimeoutSec 5).status
$registered=@((Invoke-RestMethod $query -TimeoutSec 5).hosts).Count-gt 0
[pscustomobject]@{service=$Service;oldPid=$record.pid;newPid=$newRecord.pid;deregistered=$deregistered;registered=$registered;health=$health;recoverySeconds=[math]::Round($sw.Elapsed.TotalSeconds,2)}|ConvertTo-Json -Compress
