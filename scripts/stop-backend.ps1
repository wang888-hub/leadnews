. "$PSScriptRoot\common.ps1"
$backendRuntime = Join-Path $script:RuntimeRoot 'backend'
if (-not (Test-Path -LiteralPath $backendRuntime)) { Write-Host 'No Stage 14 backend PID directory.'; return }
foreach ($pidFile in Get-ChildItem -LiteralPath $backendRuntime -Filter '*.json') {
    $record = Get-Content -Raw -LiteralPath $pidFile.FullName | ConvertFrom-Json
    $process = Get-Process -Id $record.pid -ErrorAction SilentlyContinue
    if (-not $process) { Remove-Item -LiteralPath $pidFile.FullName -Force; continue }
    $expectedExecutable = (Resolve-Path -LiteralPath $record.executable).Path
    $recordedStart = ([datetime] $record.startedAt).ToString('o')
    if ($process.Path -ne $expectedExecutable -or $process.StartTime.ToString('o') -ne $recordedStart) {
        Write-Warning "PID $($record.pid) no longer matches $($pidFile.BaseName); not stopping it."
        continue
    }
    Stop-Process -Id $record.pid
    Remove-Item -LiteralPath $pidFile.FullName -Force
    Write-Host "Stopped $($pidFile.BaseName), PID $($record.pid)."
}
