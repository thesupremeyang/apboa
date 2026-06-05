@echo off
echo ========================================
echo Apboa System Status
echo ========================================
echo.

REM Check Redis
echo [Redis]
netstat -an | findstr ":6379" > nul
if %errorlevel%==0 (
    echo Status: RUNNING on port 6379
) else (
    echo Status: STOPPED
)
echo.

REM Check Backend
echo [Backend]
netstat -an | findstr ":3060" > nul
if %errorlevel%==0 (
    echo Status: RUNNING on port 3060
) else (
    echo Status: STOPPED
)
echo.

REM Check Frontend
echo [Frontend]
netstat -an | findstr ":3000" > nul
if %errorlevel%==0 (
    echo Status: RUNNING on port 3000
) else (
    echo Status: STOPPED
)
echo.

REM Check MySQL
echo [MySQL]
netstat -an | findstr ":3306" > nul
if %errorlevel%==0 (
    echo Status: RUNNING on port 3306
) else (
    echo Status: STOPPED
)
echo.

REM Check PostgreSQL
echo [PostgreSQL]
netstat -an | findstr ":5432" > nul
if %errorlevel%==0 (
    echo Status: RUNNING on port 5432
) else (
    echo Status: STOPPED
)
echo.

echo ========================================
echo Access URLs:
echo Frontend: http://localhost:3000
echo Backend:  http://localhost:3060
echo ========================================
