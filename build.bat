@echo off
REM Medieval Factions Build Script for Windows
REM This script builds the Medieval Factions plugin jar

echo ================================================
echo Medieval Factions Build Script
echo ================================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or later from: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

echo Checking Java version...
for /f tokens^=2-5^ delims^=.-_^" %%j in ('java -version 2^>^&1 ^| findstr /i "version"') do set "jver=%%j"
if %jver% lss 17 (
    echo ERROR: Java 17 or later is required
    echo Current Java version: %jver%
    echo Please install Java 17 or later from: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

echo Java version check passed (Java %jver%)
echo.

REM Clean previous builds
echo Cleaning previous builds...
call gradlew.bat clean --no-daemon
if %errorlevel% neq 0 (
    echo WARNING: Clean task failed, continuing anyway...
)
echo.

REM Build the project
echo Building Medieval Factions...
echo This may take a few minutes on first run...
echo.
call gradlew.bat shadowJar --no-daemon
if %errorlevel% neq 0 (
    echo.
    echo ================================================
    echo BUILD FAILED
    echo ================================================
    echo.
    echo The build failed. This could be due to:
    echo   1. Network issues downloading dependencies
    echo   2. Missing Java Development Kit (JDK)
    echo   3. Compilation errors
    echo.
    echo Try running the build again, as some dependencies
    echo may have been cached and could work on retry.
    echo.
    pause
    exit /b 1
)

REM Check if the jar was created
if not exist "build\libs\medieval-factions-*-all.jar" (
    echo.
    echo ERROR: Build completed but jar file not found
    echo Expected location: build\libs\medieval-factions-*-all.jar
    echo.
    pause
    exit /b 1
)

echo.
echo ================================================
echo BUILD SUCCESSFUL
echo ================================================
echo.
echo The plugin jar has been built successfully!
echo.
echo Location: build\libs\
dir /b build\libs\*-all.jar
echo.
echo You can now copy this jar file to your server's plugins folder.
echo.
pause
