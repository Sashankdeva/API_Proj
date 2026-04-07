# SquadUp — Start Server Script
# Run this every time you open the project.
# It kills any old process on port 5500 and starts fresh.

Write-Host "Checking for processes on port 5500..." -ForegroundColor Cyan

$port = 5500
$connections = netstat -ano | Select-String ":$port\s+.*LISTENING"

foreach ($line in $connections) {
    $parts = $line -split '\s+' | Where-Object { $_ -ne '' }
    $pid = $parts[-1]
    if ($pid -match '^\d+$' -and $pid -ne '0') {
        Write-Host "Killing process $pid on port $port..." -ForegroundColor Yellow
        taskkill /PID $pid /F 2>$null
    }
}

Write-Host ""
Write-Host "Starting SquadUp server..." -ForegroundColor Green
node server.js