. "$PSScriptRoot\common.ps1"
$frontendRuntime = Join-Path $script:RuntimeRoot 'frontend'
if (-not (Test-Path -LiteralPath $frontendRuntime)) { Write-Host 'No Stage 14 frontend PID directory.'; return }
foreach ($pidFile in Get-ChildItem -LiteralPath $frontendRuntime -Filter '*.json') {
    $record = Get-Content -Raw -LiteralPath $pidFile.FullName | ConvertFrom-Json
    $process = Get-Process -Id $record.pid -ErrorAction SilentlyContinue
    if (-not $process) { Remove-Item -LiteralPath $pidFile.FullName -Force; continue }
    $recordedStart = ([datetime] $record.startedAt).ToString('o')
    if ($process.Path -ne $record.executable -or $process.StartTime.ToString('o') -ne $recordedStart) {
        Write-Warning "PID $($record.pid) no longer matches $($pidFile.BaseName); not stopping it."
        continue
    }
    & taskkill.exe /PID $record.pid /T | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to stop $($pidFile.BaseName) process tree; PID record was retained. Run this script from the same privilege level used to start it."
    }
    Remove-Item -LiteralPath $pidFile.FullName -Force
    Write-Host "Stopped $($pidFile.BaseName) process tree, PID $($record.pid)."
}
