@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Script for Windows
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set "JAVA_HOME_DEFAULT=C:\Program Files\Java\jdk-27.0.1.0.3"
if "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME_DEFAULT%" (
        set "JAVA_HOME=%JAVA_HOME_DEFAULT%"
    )
)

set "MAVEN_CMD_M2=C:\Users\machtrunghao\.m2\wrapper\dists\apache-maven-3.9.6\0f95e7798d182e3371f3fee6d8202d3a56e6d71dfd3a2402139882ef2fbe7476\bin\mvn.cmd"
if exist "%MAVEN_CMD_M2%" (
    "%MAVEN_CMD_M2%" %*
    goto :end
)

where mvn >nul 2>&1
if %ERRORLEVEL% equ 0 (
    mvn %*
    goto :end
)

echo [ERROR] Maven not found. Please install Maven or check MAVEN_HOME.
exit /b 1

:end
endlocal

