# ==============================================================================
# HOTEL BOOKING SYSTEM - INITIALIZE & START LOCAL POSTGRESQL (PORT 5433)
# ==============================================================================
$rootDir = (Get-Item $PSScriptRoot).Parent.FullName
$pgDataDir = Join-Path $rootDir "database\pgdata"
$initSql = Join-Path $rootDir "database\init.sql"
$psqlExe = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
$initdbExe = "C:\Program Files\PostgreSQL\18\bin\initdb.exe"
$pgctlExe = "C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Khoi tao va Khoi chay PostgreSQL cho HotelBooking System " -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Kiem tra thu muc data
if (-not (Test-Path $pgDataDir)) {
    Write-Host "[1/3] Khoi tao Cum du lieu moi tai: $pgDataDir" -ForegroundColor Green
    $pwFile = "$env:TEMP\pg_init_pw.txt"
    "hotel_secure_password_2025" | Set-Content -Path $pwFile -NoNewline
    & $initdbExe -D $pgDataDir -U hotel_user -A scram-sha-256 --pwfile=$pwFile -E UTF8 --no-locale | Out-Null
    Remove-Item $pwFile -Force -ErrorAction SilentlyContinue

    # Cap nhat pg_hba.conf de cho phep ket noi localhost
    $hbaFile = Join-Path $pgDataDir "pg_hba.conf"
    Add-Content -Path $hbaFile -Value "`nhost all all 127.0.0.1/32 scram-sha-256`nhost all all ::1/128 scram-sha-256`n"
} else {
    Write-Host "[1/3] Cum du lieu da ton tai tai: $pgDataDir" -ForegroundColor Green
}

# 2. Kiem tra xem port 5433 da chay chua
$conn = Get-NetTCPConnection -LocalPort 5433 -ErrorAction SilentlyContinue
if (-not $conn) {
    Write-Host "[2/3] Dang khoi chay PostgreSQL tai cong 5433..." -ForegroundColor Green
    $logFile = Join-Path $rootDir "database\postgres.log"
    & $pgctlExe -D $pgDataDir -l $logFile -o "-p 5433" start
    Start-Sleep -Seconds 3
} else {
    Write-Host "[2/3] PostgreSQL da dang chay tren cong 5433." -ForegroundColor Green
}

# 3. Kiem tra va tao CSDL hotel_booking_db
Write-Host "[3/3] Kiem tra Database hotel_booking_db va nap du lieu mau..." -ForegroundColor Green
$env:PGPASSWORD = "hotel_secure_password_2025"

# Kiem tra CSDL da ton tai chua
$dbCheck = & $psqlExe -h localhost -p 5433 -U hotel_user -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='hotel_booking_db';" 2>&1
if ($dbCheck -notmatch "1") {
    Write-Host "Creating database hotel_booking_db..." -ForegroundColor Yellow
    & $psqlExe -h localhost -p 5433 -U hotel_user -d postgres -c "CREATE DATABASE hotel_booking_db;" 2>&1 | Out-Null
}

# Chay init.sql de tao bang va du lieu mau
$runSql = & $psqlExe -h localhost -p 5433 -U hotel_user -d hotel_booking_db -f $initSql 2>&1
Write-Host "Ket qua nap Schema & Data:" -ForegroundColor Green
Write-Host $runSql

$env:PGPASSWORD = $null
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " PostgreSQL san sang tai localhost:5433 | DB: hotel_booking_db" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
