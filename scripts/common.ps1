$ErrorActionPreference = 'Stop'

$script:ProjectRoot = Split-Path -Parent $PSScriptRoot
$script:RuntimeRoot = Join-Path $script:ProjectRoot '.stage14-runtime'
$script:LogRoot = Join-Path $script:ProjectRoot '.stage14-logs'

function Initialize-LeadnewsDirectories {
    New-Item -ItemType Directory -Force -Path $script:RuntimeRoot, $script:LogRoot | Out-Null
}

function Import-LeadnewsEnvironment {
    $envFile = Join-Path $script:ProjectRoot '.env'
    if (-not (Test-Path -LiteralPath $envFile)) {
        throw 'Missing .env. Copy .env.example to .env and set local development values first.'
    }
    foreach ($line in Get-Content -LiteralPath $envFile) {
        if ($line -match '^\s*([^#][A-Za-z0-9_]+)=(.*)$') {
            $name = $matches[1]
            if ($name -eq 'API-KEY') { continue }
            [Environment]::SetEnvironmentVariable($name, $matches[2].Trim('"'), 'Process')
        }
    }
    $env:DB_PASSWORD = $env:MYSQL_ROOT_PASSWORD
    $env:MINIO_ACCESS_KEY = $env:MINIO_ROOT_USER
    $env:MINIO_SECRET_KEY = $env:MINIO_ROOT_PASSWORD
    $env:NACOS_SERVER_ADDR = 'localhost:8848'
    $env:DB_HOST = 'localhost'
    $env:DB_PORT = '3307'
    $env:REDIS_HOST = 'localhost'
    $env:REDIS_PORT = '6379'
    $env:KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
    $env:ELASTICSEARCH_URL = 'http://localhost:9201'
    $env:MINIO_ENDPOINT = 'http://localhost:9000'
    $env:MINIO_PUBLIC_ENDPOINT = 'http://localhost:9000'
    $env:XXL_JOB_ADMIN_ADDRESSES = 'http://localhost:8088/xxl-job-admin'
}

function Resolve-JavaExecutable([string] $JavaHome) {
    if (-not $JavaHome) { $JavaHome = $env:LEADNEWS_JAVA_HOME }
    if (-not $JavaHome) { throw 'Set LEADNEWS_JAVA_HOME to a Temurin JDK 21 directory or pass -JavaHome.' }
    $java = Join-Path $JavaHome 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $java)) { throw "java.exe not found under $JavaHome" }
    $version = (& $java -version 2>&1 | Select-Object -First 1)
    if ($version -notmatch 'version "21\.') { throw "JDK 21 is required; detected: $version" }
    return (Resolve-Path -LiteralPath $java).Path
}

function Test-TcpPort([int] $Port) {
    return [System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners().Port -contains $Port
}
