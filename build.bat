@echo off
REM Build script for Medieval Factions plugin
REM This script builds the plugin JAR file with all dependencies included

setlocal enabledelayedexpansion

echo =========================================
echo Medieval Factions - Build Script
echo =========================================
echo.

REM Check if Java is installed
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or higher from:
    echo   - https://adoptium.net/ ^(recommended^)
    echo   - https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

REM Check Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION_STRING=%%g
)
set JAVA_VERSION_STRING=%JAVA_VERSION_STRING:"=%
for /f "delims=. tokens=1" %%v in ("%JAVA_VERSION_STRING%") do (
    set JAVA_MAJOR_VERSION=%%v
)

echo Detected Java version: %JAVA_MAJOR_VERSION%

if %JAVA_MAJOR_VERSION% LSS 17 (
    echo ERROR: Java 17 or higher is required
    echo Current version: %JAVA_MAJOR_VERSION%
    echo Please install Java 17 or higher from:
    echo   - https://adoptium.net/ ^(recommended^)
    echo   - https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

echo [OK] Java version is compatible
echo.

REM Build the plugin
echo Building Medieval Factions plugin...
echo This may take a few minutes on the first run as dependencies are downloaded...
echo.

REM Use the Gradle wrapper to build the shadowJar (which contains all dependencies)
if exist "gradlew.bat" (
    call gradlew.bat clean shadowJar --no-daemon
    set BUILD_RESULT=!ERRORLEVEL!
) else (
    echo ERROR: gradlew.bat wrapper not found!
    pause
    exit /b 1
)

REM Check if build was successful
if !BUILD_RESULT! NEQ 0 (
    echo.
    echo =========================================
    echo [X] BUILD FAILED
    echo =========================================
    echo.
    echo Please check the output above for errors.
    pause
    exit /b 1
)

REM Find the built JAR file
set JAR_FILE=
for %%f in (build\libs\*-all.jar) do (
    set JAR_FILE=%%f
    goto :found
)

:found
if defined JAR_FILE (
    echo.
    echo =========================================
    echo [OK] BUILD SUCCESSFUL!
    echo =========================================
    echo.
    echo The plugin JAR file has been created at:
    echo %JAR_FILE%
    echo.
    echo To use the plugin:
    echo 1. Copy the JAR file to your server's 'plugins' folder
    echo 2. Restart your server
    echo.
) else (
    echo.
    echo =========================================
    echo [X] BUILD FAILED
    echo =========================================
    echo.
    echo The build completed but the JAR file was not found.
    echo Please check the output above for errors.
)

pause
