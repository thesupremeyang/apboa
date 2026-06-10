@echo off
node create_gongwen.js > output.txt 2>&1
echo Exit code: %errorlevel%
type output.txt
pause
