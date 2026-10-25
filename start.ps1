# SquadUp — Start Server Script
# Run this every time you open the project.
# It kills any old process on port 5500 and starts the Spring Boot app fresh.
#   .\start.ps1            -> embedded H2 database (no setup needed)
#   .\start.ps1 mysql      -> MySQL (see src/main/resources/application-mysql.properties)

param([string]$DbProfile = "")

Write-Host "Checking for processes on port 5500..." -ForegroundColor Cyan

$port = 5500
$connections = netstat -ano | Select-String ":$port\s+.*LISTENING"

foreach ($line in $connections) {
    $parts = $line -split '\s+' | Where-Object { $_ -ne '' }
    $procId = $parts[-1]
    if ($procId -match '^\d+$' -and $procId -ne '0') {
        Write-Host "Killing process $procId on port $port..." -ForegroundColor Yellow
        taskkill /PID $procId /F 2>$null
    }
}

Write-Host ""
Write-Host "Starting SquadUp (Spring Boot) on http://localhost:$port ..." -ForegroundColor Green
if ($DbProfile) {
    # "dev" keeps the demo data; listing it first lets the database profile win where they overlap.
    .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev,$DbProfile"
} else {
    .\mvnw.cmd spring-boot:run
}
