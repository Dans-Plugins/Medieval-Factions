@echo off
REM Medieval Factions Build Script for Windows
REM This script builds the Medieval Factions plugin jar
REM
REM Usage:
REM   build.bat           - Build in current directory (requires repository)
REM   build.bat standalone - Clone repository and build

setlocal enabledelayedexpansion

echo ================================================
echo Medieval Factions Build Script
echo ================================================
echo.

REM Check if running in standalone mode
set STANDALONE_MODE=false
if "%1"=="standalone" (
    set STANDALONE_MODE=true
    echo Running in STANDALONE mode
    echo.
)

REM If standalone mode, check if we need to clone the repository
if "%STANDALONE_MODE%"=="true" (
    REM Check if git is installed
    git --version >nul 2>&1
    if !errorlevel! neq 0 (
        echo ERROR: Git is not installed or not in PATH
        echo Git is required for standalone mode to clone the repository.
        echo.
        echo Please install Git from: https://git-scm.com/download/win
        echo.
        echo If you continue to experience issues, please report them at:
        echo https://github.com/Dans-Plugins/Medieval-Factions/issues
        echo.
        pause
        exit /b 1
    )
    
    REM Check if we're already in the repository
    if not exist "build.gradle" (
        echo Cloning Medieval-Factions repository...
        set REPO_URL=https://github.com/Dans-Plugins/Medieval-Factions.git
        set REPO_DIR=Medieval-Factions
        
        if exist "!REPO_DIR!" (
            echo Directory !REPO_DIR! already exists. Using existing directory.
            cd "!REPO_DIR!"
        ) else (
            git clone "!REPO_URL!" "!REPO_DIR!"
            if !errorlevel! neq 0 (
                echo.
                echo ERROR: Failed to clone repository
                echo Please check your internet connection and try again.
                echo.
                echo If the problem persists, please report it at:
                echo https://github.com/Dans-Plugins/Medieval-Factions/issues
                echo.
                pause
                exit /b 1
            )
            cd "!REPO_DIR!"
        )
        echo Repository cloned successfully!
        echo.
    )
)

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or later from: https://adoptium.net/
    echo.
    echo If you continue to experience issues, please report them at:
    echo https://github.com/Dans-Plugins/Medieval-Factions/issues
    echo.
    pause
    exit /b 1
)

echo Checking Java version...
REM Parse Java version - handle both old (1.8) and new (9+) formats
for /f tokens^=2-5^ delims^=.-_^" %%j in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "jver1=%%j"
    set "jver2=%%k"
)
REM For Java 8 and earlier, version is 1.8.0_xxx (jver1=1, jver2=8)
REM For Java 9+, version is 11.0.x (jver1=11, jver2=0)
if "%jver1%"=="1" (
    set "jver=%jver2%"
) else (
    set "jver=%jver1%"
)

if %jver% lss 17 (
    echo ERROR: Java 17 or later is required
    echo Current Java version: %jver%
    echo Please install Java 17 or later from: https://adoptium.net/
    echo.
    echo If you continue to experience issues, please report them at:
    echo https://github.com/Dans-Plugins/Medieval-Factions/issues
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
    echo If the problem persists, please report it at:
    echo https://github.com/Dans-Plugins/Medieval-Factions/issues
    echo.
    pause
    exit /b 1
)

REM Check if the jar was created
dir build\libs\medieval-factions-*-all.jar >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Build completed but jar file not found
    echo Expected location: build\libs\medieval-factions-*-all.jar
    echo.
    echo Please report this issue at:
    echo https://github.com/Dans-Plugins/Medieval-Factions/issues
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
dir /b build\libs\medieval-factions-*-all.jar
echo.
echo You can now copy this jar file to your server's plugins folder.
echo.
if "%STANDALONE_MODE%"=="false" pause
