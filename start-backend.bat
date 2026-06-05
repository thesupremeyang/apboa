@echo off
cd /d C:\JAVA\apboa
set JAVA_HOME=C:\JAVA\Java Tool\jdk-21
start "Apboa Backend" /MIN "%JAVA_HOME%\bin\java.exe" -jar C:\JAVA\apboa\console\target\console-1.0-SNAPSHOT.jar
