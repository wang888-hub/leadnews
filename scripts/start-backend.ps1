param([string] $JavaHome)
. "$PSScriptRoot\common.ps1"
Initialize-LeadnewsDirectories
Import-LeadnewsEnvironment
$java = Resolve-JavaExecutable $JavaHome
$backendRuntime = Join-Path $script:RuntimeRoot 'backend'
New-Item -ItemType Directory -Force -Path $backendRuntime | Out-Null

$services = @(
    @{ Name='leadnews-user-service'; Port=51801 },
    @{ Name='leadnews-article-service'; Port=51802 },
    @{ Name='leadnews-wemedia-service'; Port=51803 },
    @{ Name='leadnews-behavior-service'; Port=51804 },
    @{ Name='leadnews-search-service'; Port=51805 },
    @{ Name='leadnews-schedule-service'; Port=51806 },
    @{ Name='leadnews-admin-service'; Port=51807 },
    @{ Name='leadnews-ai-service'; Port=51808 },
    @{ Name='leadnews-gateway'; Port=51601 }
)

foreach ($service in $services) {
    $pidFile = Join-Path $backendRuntime "$($service.Name).json"
    if (Test-Path -LiteralPath $pidFile) {
        $record = Get-Content -Raw -LiteralPath $pidFile | ConvertFrom-Json
        if (Get-Process -Id $record.pid -ErrorAction SilentlyContinue) {
            Write-Host "$($service.Name) already recorded as PID $($record.pid); skipping."
            continue
        }
        Remove-Item -LiteralPath $pidFile -Force
    }
    if (Test-TcpPort $service.Port) { throw "Port $($service.Port) is already occupied; not starting $($service.Name)." }
    $jar = Join-Path $script:ProjectRoot "$($service.Name)\target\$($service.Name)-1.0.0-SNAPSHOT.jar"
    if (-not (Test-Path -LiteralPath $jar)) { throw "Missing $jar. Run Maven verify first." }
    $stdout = Join-Path $script:LogRoot "$($service.Name).out.log"
    $stderr = Join-Path $script:LogRoot "$($service.Name).err.log"
    $process = Start-Process -FilePath $java -ArgumentList @('-jar', $jar, '--spring.cloud.nacos.discovery.ip=127.0.0.1') -WorkingDirectory (Split-Path $jar) -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    [ordered]@{ pid=$process.Id; startedAt=$process.StartTime.ToString('o'); executable=$java; jar=$jar; port=$service.Port } | ConvertTo-Json | Set-Content -LiteralPath $pidFile -Encoding utf8
    Write-Host "Started $($service.Name), PID $($process.Id), port $($service.Port)."
}

$deadline = (Get-Date).AddMinutes(2)
do {
    $pending = @()
    foreach ($service in $services) {
        try { if ((Invoke-RestMethod "http://localhost:$($service.Port)/actuator/health" -TimeoutSec 3).status -ne 'UP') { $pending += $service.Name } }
        catch { $pending += $service.Name }
    }
    if (-not $pending) { break }
    Start-Sleep -Seconds 3
} while ((Get-Date) -lt $deadline)
if ($pending) { throw "Backend health timeout: $($pending -join ', '). Check .stage14-logs." }
Write-Host 'All nine local Java services are UP.'
