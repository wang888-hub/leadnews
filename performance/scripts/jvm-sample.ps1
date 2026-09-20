param([int]$DurationSeconds=60,[int]$IntervalSeconds=5,[string]$Output='performance/results/jvm.csv')
$ErrorActionPreference='Stop'
$root=Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$jstat=Join-Path 'D:\jdkk' 'bin\jstat.exe'
$records=Get-ChildItem (Join-Path $root '.stage14-runtime\backend') -Filter '*.json' | ForEach-Object {Get-Content -Raw -LiteralPath $_.FullName | ConvertFrom-Json}
$rows=@();$until=(Get-Date).AddSeconds($DurationSeconds)
while((Get-Date)-lt $until){
  foreach($record in $records){
    $p=Get-Process -Id $record.pid -ErrorAction SilentlyContinue
    if(!$p){continue}
    $gc=((& $jstat -gcutil $record.pid 2>$null | Select-Object -Last 1) -join '')
    $rows += [pscustomobject]@{timestamp=(Get-Date).ToString('o');service=[IO.Path]::GetFileNameWithoutExtension($record.jar);pid=$record.pid;cpuSeconds=[math]::Round($p.CPU,2);rssMiB=[math]::Round($p.WorkingSet64/1MB,2);threads=$p.Threads.Count;gcutil=($gc -replace '\s+',' ').Trim()}
  }
  Start-Sleep -Seconds $IntervalSeconds
}
$path=Join-Path $root $Output;New-Item -ItemType Directory -Force (Split-Path $path)|Out-Null;$rows|Export-Csv -NoTypeInformation -Encoding utf8 $path
Write-Host "Wrote $($rows.Count) JVM samples to $path"
