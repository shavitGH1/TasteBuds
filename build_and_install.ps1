Write-Host "Building TasteBuds app..." -ForegroundColor Cyan
Set-Location C:\Users\shavi\TasteBuds

Write-Host "`nStopping Gradle daemons..." -ForegroundColor Yellow
.\gradlew --stop | Out-Null

Write-Host "`nBuilding debug APK..." -ForegroundColor Yellow
$buildOutput = .\gradlew assembleDebug -x lint -x lintVitalAnalyzeDebug -x lintVitalReportDebug 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Build successful!" -ForegroundColor Green

    Write-Host "`nInstalling to device..." -ForegroundColor Yellow
    $installOutput = .\gradlew installDebug -x lint -x lintVitalAnalyzeDebug -x lintVitalReportDebug 2>&1

    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Installation successful!" -ForegroundColor Green
        Write-Host "`n🎉 App is ready to test!" -ForegroundColor Green
        Write-Host "`nYou can now:" -ForegroundColor Cyan
        Write-Host "  1. Open the app on your device" -ForegroundColor White
        Write-Host "  2. Navigate to Add Recipe" -ForegroundColor White
        Write-Host "  3. Try adding a recipe with all the new fields!" -ForegroundColor White
    } else {
        Write-Host "❌ Installation failed" -ForegroundColor Red
        Write-Host $installOutput -ForegroundColor Red
    }
} else {
    Write-Host "❌ Build failed" -ForegroundColor Red
    Write-Host $buildOutput -ForegroundColor Red
}

