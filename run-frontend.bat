@echo off
title HESTORA Frontend Server
where node >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [HESTORA] Khoi chay bang Node.js Server...
    node "%~dp0frontend\server.js"
) else (
    echo [HESTORA] Khoi chay bang PowerShell Server...
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-frontend.ps1" %*
)
pause
