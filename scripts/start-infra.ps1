. "$PSScriptRoot\common.ps1"
Initialize-LeadnewsDirectories
Push-Location $script:ProjectRoot
try {
    docker compose config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'docker compose config failed.' }
    docker compose up -d
    if ($LASTEXITCODE -ne 0) { throw 'docker compose up failed.' }
    docker compose ps
} finally { Pop-Location }
