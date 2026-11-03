# HotelBooking - Start Backend Service (Spring Boot)
param(
    [string]$DbUser = "",
    [string]$DbPassword = "",
    [string]$DbName = "",
    [string]$DbHost = "",
    [int]$DbPort = 0,
    [int]$Port = 0
)

$backendDir = Join-Path $PSScriptRoot "backend"
$envFile = Join-Path $PSScriptRoot ".env"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " Starting HotelBooking Spring Boot Backend " -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Load variables from .env if present
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $k = $parts[0].Trim()
            $v = $parts[1].Trim()
            if (-not [string]::IsNullOrEmpty($k) -and -not [System.Environment]::GetEnvironmentVariable($k)) {
                [System.Environment]::SetEnvironmentVariable($k, $v, "Process")
            }
        }
    }
}

# 2. Override with CLI parameters if supplied
if ($DbUser)     { $env:DB_USERNAME = $DbUser }
if ($DbPassword) { $env:DB_PASSWORD = $DbPassword }
if ($DbName)     { $env:DB_NAME = $DbName }
if ($DbHost)     { $env:DB_HOST = $DbHost }
if ($DbPort -gt 0) { $env:DB_PORT = $DbPort }
if ($Port -gt 0) { $env:SERVER_PORT = $Port }

# Defaults if still empty
if (-not $env:DB_HOST)     { $env:DB_HOST = "localhost" }
if (-not $env:DB_PORT)     { $env:DB_PORT = "5432" }
if (-not $env:DB_NAME)     { $env:DB_NAME = "hotel_booking_db" }
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = "hotel_user" }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = "hotel_secure_password_2025" }
if (-not $env:SERVER_PORT) { $env:SERVER_PORT = "8080" }

# Construct DB_URL if not set
$env:DB_URL = "jdbc:postgresql://$($env:DB_HOST):$($env:DB_PORT)/$($env:DB_NAME)"

Write-Host ">>> Cấu hình Database kết nối:" -ForegroundColor Gray
Write-Host "    Host:     $($env:DB_HOST):$($env:DB_PORT)" -ForegroundColor White
Write-Host "    Database: $($env:DB_NAME)" -ForegroundColor White
Write-Host "    Username: $($env:DB_USERNAME)" -ForegroundColor White
Write-Host "    URL:      $($env:DB_URL)" -ForegroundColor Gray
Write-Host "------------------------------------------" -ForegroundColor DarkGray
Write-Host " [MẸO SỬA LỖI MẬT KHẨU POSTGRESQL]" -ForegroundColor Yellow
Write-Host " Nếu gặp lỗi 'password authentication failed':" -ForegroundColor Gray
Write-Host " • Sửa mật khẩu trong file .env hoặc chạy với tham số:" -ForegroundColor Gray
Write-Host "   .\start-backend.ps1 -DbPassword <mat_khau_cua_ban>" -ForegroundColor Cyan
Write-Host " • Nếu dùng Docker (hotel_user):" -ForegroundColor Gray
Write-Host "   .\start-backend.ps1 -DbUser hotel_user -DbPassword hotel_secure_password_2025" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 3. Discover JDK 27
if (-not $env:JAVA_HOME -and (Test-Path "C:\Program Files\Java\jdk-27.0.1.0.3")) {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-27.0.1.0.3"
}

Set-Location $backendDir

if (Test-Path ".\mvnw.cmd") {
    Write-Host "Running with Maven Wrapper..." -ForegroundColor Green
    .\mvnw.cmd spring-boot:run
} elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
    Write-Host "Running with Maven CLI..." -ForegroundColor Green
    mvn spring-boot:run
} else {
    Write-Host "Java/Maven not found in PATH." -ForegroundColor Red
    Write-Host "Vui lòng cài đặt JDK 17+ và Maven để khởi chạy backend." -ForegroundColor Yellow
}
