@echo off
echo Starting Apboa System...
echo.

REM Wait for Redis to be ready
echo Starting Redis...
start "" "C:\JAVA\Java Tool\redis\redis-server.exe"
timeout /t 3 /nobreak > nul

REM Start Backend
echo Starting Backend...
cd /d C:\JAVA\apboa
set JAVA_HOME=C:\JAVA\Java Tool\jdk-21
start "Apboa Backend" /MIN "%JAVA_HOME%\bin\java.exe" -jar C:\JAVA\apboa\console\target\console-1.0-SNAPSHOT.jar
timeout /t 15 /nobreak > nul

REM Start Frontend
echo Starting Frontend...
cd /d C:\JAVA\apboa\ui
start "Apboa Frontend" /MIN cmd /c "pnpm run dev"

echo.
echo Apboa System Started!
echo Backend: http://localhost:3060
echo Frontend: http://localhost:3000
