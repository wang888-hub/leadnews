param([int] $AppPort=5173, [int] $WemediaPort=5174, [int] $AdminPort=5175)
. "$PSScriptRoot\common.ps1"
Initialize-LeadnewsDirectories
$frontendRuntime = Join-Path $script:RuntimeRoot 'frontend'
New-Item -ItemType Directory -Force -Path $frontendRuntime | Out-Null
$apps = @(
    @{ Name='leadnews-app'; Port=$AppPort },
    @{ Name='leadnews-wemedia'; Port=$WemediaPort },
    @{ Name='leadnews-admin'; Port=$AdminPort }
)
foreach ($app in $apps) {
    if (Test-TcpPort $app.Port) { throw "Port $($app.Port) is occupied; choose another explicit Stage 14 port." }
    $pidFile = Join-Path $frontendRuntime "$($app.Name).json"
    $stdout = Join-Path $script:LogRoot "$($app.Name).out.log"
    $stderr = Join-Path $script:LogRoot "$($app.Name).err.log"
    $args = "pnpm --filter $($app.Name) exec vite --port $($app.Port) --strictPort --host 127.0.0.1"
    $process = Start-Process -FilePath 'corepack.cmd' -ArgumentList $args -WorkingDirectory (Join-Path $script:ProjectRoot 'frontend') -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    [ordered]@{ pid=$process.Id; startedAt=$process.StartTime.ToString('o'); executable=$process.Path; port=$app.Port } | ConvertTo-Json | Set-Content -LiteralPath $pidFile -Encoding utf8
    Write-Host "Started $($app.Name), PID $($process.Id), port $($app.Port)."
}
