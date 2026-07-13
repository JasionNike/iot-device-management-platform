$ErrorActionPreference = "Stop"
Set-Location "d:\work\简历\智能物联设备管理平台"

Write-Host "Pushing to GitHub..." -ForegroundColor Green
git push origin master 2>&1 | Write-Host

if ($LASTEXITCODE -eq 0) {
    Write-Host "SUCCESS! CI/CD triggered." -ForegroundColor Green
} else {
    Write-Host "Push failed. Exit code: $LASTEXITCODE" -ForegroundColor Red
}

Read-Host "Press Enter to close"
