@echo off
echo Building Handlebars Email Template Testing App...
echo.

REM Check for Maven
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven not found. Please install Maven from https://maven.apache.org/download.cgi
    echo        and make sure it is on your PATH.
    pause
    exit /b 1
)

REM Check for Java
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java not found. Please install Java 11 or higher.
    pause
    exit /b 1
)

echo Maven found. Building...
call mvn clean package -q

if %errorlevel% neq 0 (
    echo.
    echo BUILD FAILED. See output above for errors.
    pause
    exit /b 1
)

echo.
echo ============================================
echo  BUILD SUCCESSFUL
echo  JAR created: target\handlebars-email-template-testing-app-1.0.jar
echo ============================================
echo.
echo To run the tester, use:
echo   java -jar target\handlebars-email-template-testing-app-1.0.jar
echo.
echo Place these files in the same folder as the JAR:
echo   - template.html
echo   - payload.json
echo   - customhandlebar.js
echo.
pause
