@echo off
echo Stopping Apboa System...

REM Stop Frontend
taskkill /FI "WINDOWTITLE eq Apboa Frontend*" /F 2>nul

REM Stop Backend
taskkill /FI "WINDOWTITLE eq Apboa Backend*" /F 2>nul

REM Stop Redis (optional, uncomment if needed)
REM taskkill /FI "WINDOWTITLE eq redis*" /F 2>nul

echo Apboa System Stopped!
