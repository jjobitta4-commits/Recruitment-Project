@echo off
title Online Recruitment Management System
echo ===================================================
echo   Compiling and Starting Recruitment System...
echo ===================================================

if not exist bin mkdir bin

javac -encoding UTF-8 -cp lib/mysql-connector-j.jar;src -d bin src/com/recruitment/Main.java src/com/recruitment/model/*.java src/com/recruitment/dao/*.java src/com/recruitment/util/*.java src/com/recruitment/server/*.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Starting server on http://localhost:8080 ...
    java -cp bin;lib/* com.recruitment.Main
) else (
    echo.
    echo [ERROR] Compilation failed! Please check your JDK installation.
    pause
)
