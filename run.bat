@echo off
title Online Recruitment Management System
cd /d "%~dp0"

echo ===================================================
echo   Online Recruitment Management System Launcher
echo ===================================================
echo.

:: 1. Verify Java Installation
where java >nul 2>&1
if errorlevel 1 goto :check_java_home
goto :java_ok

:check_java_home
if not defined JAVA_HOME goto :no_java
set "PATH=%JAVA_HOME%\bin;%PATH%"
where java >nul 2>&1
if errorlevel 1 goto :no_java
goto :java_ok

:no_java
echo [ERROR] Java runtime (java.exe) was not found.
echo Please ensure JDK 17 or higher is installed and added to your PATH.
echo.
pause
exit /b 1

:java_ok
:: 2. Ensure bin directory exists
if not exist "bin" mkdir "bin"

:: 3. Compile sources
echo [*] Compiling Java source files...
where javac >nul 2>&1
if errorlevel 1 goto :skip_compile

javac -encoding UTF-8 -cp "lib/mysql-connector-j.jar;src" -d "bin" src/com/recruitment/Main.java src/com/recruitment/model/*.java src/com/recruitment/dao/*.java src/com/recruitment/util/*.java src/com/recruitment/server/*.java
if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed. Please check the errors above.
    pause
    exit /b 1
)
echo [*] Compilation completed successfully.
goto :launch

:skip_compile
echo [!] 'javac' not found in PATH. Checking for pre-compiled classes...
if not exist "bin\com\recruitment\Main.class" (
    echo [ERROR] No compiled classes found in bin\ and javac is not available.
    echo Please install JDK 17+ and add it to your PATH.
    pause
    exit /b 1
)

:launch
:: 4. Launch the application
echo.
echo [*] Starting server on http://localhost:8080 ...
echo.
java -cp "bin;lib/*" com.recruitment.Main

echo.
echo Server process has stopped.
pause
