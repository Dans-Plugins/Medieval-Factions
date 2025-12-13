@echo off
REM Standalone Build Script for Medieval Factions plugin
REM This script handles everything: checking dependencies, cloning the repo, and building the JAR
REM Usage: build.bat [version|branch]
REM   - No argument: builds the latest main branch
REM   - With argument: builds the specified version tag or branch (e.g., v5.7.0 or develop)

setlocal enabledelayedexpansion

set REPO_URL=https://github.com/Dans-Plugins/Medieval-Factions.git
set BUILD_DIR=MedievalFactions-build
set VERSION_OR_BRANCH=%1
if "%VERSION_OR_BRANCH%"=="" set VERSION_OR_BRANCH=main

echo =========================================
echo Medieval Factions - Standalone Build Script
echo =========================================
echo.
echo This script will:
echo   1. Check for required dependencies (Java 21+, Git)
echo   2. Clone the Medieval Factions repository
echo   3. Build the plugin JAR file
echo.

REM Check if Git is installed
where git >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Git is not installed
    echo.
    echo Please install Git from: https://git-scm.com/downloads
    echo.
    pause
    exit /b 1
)

echo [OK] Git is installed

REM Check if Java is installed
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 21 or higher from:
    echo   - https://adoptium.net/ ^(recommended^)
    echo   - https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

echo [OK] Java is installed

REM Check Java version
REM Extract version from various Java version formats
set JAVA_VERSION_LINE=
for /f "usebackq tokens=*" %%i in (`java -version 2^>^&1 ^| findstr /i "version"`) do (
    set JAVA_VERSION_LINE=%%i
)

REM Extract the version string from the line (typically third token)
set JAVA_VERSION_STRING=
for /f "tokens=3" %%g in ("%JAVA_VERSION_LINE%") do (
    set JAVA_VERSION_STRING=%%g
)

REM Remove quotes
set JAVA_VERSION_STRING=%JAVA_VERSION_STRING:"=%

REM Handle both old format (1.8.0) and new format (21.0.1)
for /f "tokens=1 delims=." %%v in ("%JAVA_VERSION_STRING%") do (
    set JAVA_MAJOR_VERSION=%%v
)

REM If version starts with 1, take the second part (e.g., 1.8 -> 8)
if "%JAVA_MAJOR_VERSION%"=="1" (
    for /f "tokens=2 delims=." %%v in ("%JAVA_VERSION_STRING%") do (
        set JAVA_MAJOR_VERSION=%%v
    )
)

REM Remove any non-numeric suffixes
for /f "tokens=1 delims=-+" %%v in ("%JAVA_MAJOR_VERSION%") do (
    set JAVA_MAJOR_VERSION=%%v
)

echo Detected Java version: %JAVA_MAJOR_VERSION%

REM Check if version was extracted successfully
if "%JAVA_MAJOR_VERSION%"=="" (
    echo ERROR: Could not determine Java version
    echo Please install Java 21 or higher from:
    echo   - https://adoptium.net/ ^(recommended^)
    echo   - https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

if %JAVA_MAJOR_VERSION% LSS 21 (
    echo ERROR: Java 21 or higher is required to build Medieval Factions
    echo Current version: Java %JAVA_MAJOR_VERSION%
    echo.
    echo This project requires Java 21 due to dependencies and build tooling.
    echo.
    
    REM Offer to install Java 21 automatically
    echo NOTE: This will use your system's package manager to install Java 21.
    set /p INSTALL_JAVA="Would you like to download and install Java 21 automatically? (y/n): "
    
    if /i "%INSTALL_JAVA%"=="y" (
        echo.
        echo Installing Java 21...
        echo (This may take a few minutes)
        echo.
        
        REM Check if winget is available (Windows 10 1809+ / Windows 11)
        where winget >nul 2>&1
        if %ERRORLEVEL% EQU 0 (
            echo Detected winget package manager
            echo Installing Eclipse Temurin JDK 21 via winget...
            echo.
            winget install EclipseAdoptium.Temurin.21.JDK --silent --accept-package-agreements --accept-source-agreements
            
            if %ERRORLEVEL% EQU 0 (
                echo.
                echo Java 21 has been installed successfully.
                echo.
                echo IMPORTANT: Please close this command prompt and open a new one for the PATH changes to take effect.
                echo Then run this build script again.
                echo.
            ) else (
                echo.
                echo WARNING: Java installation may have failed.
                echo Please try installing manually from:
                echo   - https://adoptium.net/temurin/releases/?version=21
                echo.
            )
            pause
            exit /b 0
        ) else (
            REM Check if choco is available
            where choco >nul 2>&1
            if %ERRORLEVEL% EQU 0 (
                echo Detected Chocolatey package manager
                echo Installing Temurin JDK 21 via Chocolatey...
                echo.
                choco install temurin21 -y
                
                if %ERRORLEVEL% EQU 0 (
                    echo.
                    echo Java 21 has been installed successfully.
                    echo.
                    echo IMPORTANT: Please close this command prompt and open a new one for the PATH changes to take effect.
                    echo Then run this build script again.
                    echo.
                ) else (
                    echo.
                    echo WARNING: Java installation may have failed.
                    echo Please try installing manually from:
                    echo   - https://adoptium.net/temurin/releases/?version=21
                    echo.
                )
                pause
                exit /b 0
            ) else (
                echo Neither winget nor Chocolatey package manager detected.
                echo.
                echo Please install Java 21 manually:
                echo   1. Download from: https://adoptium.net/temurin/releases/?version=21
                echo   2. Run the installer
                echo   3. Ensure "Add to PATH" is checked during installation
                echo   4. Restart your command prompt
                echo   5. Run this script again
                echo.
                echo Alternatively, you can install a package manager:
                echo   - winget: Included in Windows 10 1809+ and Windows 11
                echo   - Chocolatey: https://chocolatey.org/install
                echo.
                pause
                exit /b 1
            )
        )
    ) else (
        echo.
        echo Java 21 installation declined.
        echo.
        echo Please install Java 21 manually from:
        echo   - https://adoptium.net/temurin/releases/?version=21 ^(recommended^)
        echo   - https://www.oracle.com/java/technologies/downloads/#java21
        echo.
        echo After installation, ensure Java 21 is in your PATH:
        echo   - Run: java -version
        echo   - It should show version 21 or higher
        echo.
        pause
        exit /b 1
    )
)

echo [OK] Java version %JAVA_MAJOR_VERSION% is compatible
echo.

REM Clone or update the repository
if exist "%BUILD_DIR%" (
    echo Build directory already exists. Cleaning up...
    rmdir /s /q "%BUILD_DIR%"
)

echo Cloning Medieval Factions repository...
echo Repository: %REPO_URL%
echo Version/Branch: %VERSION_OR_BRANCH%
echo.

git clone --depth 1 --branch "%VERSION_OR_BRANCH%" "%REPO_URL%" "%BUILD_DIR%" 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Failed to clone branch/tag '%VERSION_OR_BRANCH%'
    echo Trying to clone and checkout instead...
    git clone "%REPO_URL%" "%BUILD_DIR%"
    cd "%BUILD_DIR%"
    git checkout "%VERSION_OR_BRANCH%" 2>&1
    if !ERRORLEVEL! NEQ 0 (
        echo.
        echo ERROR: Could not find version/branch '%VERSION_OR_BRANCH%'
        echo.
        echo Please specify a valid branch or tag, for example:
        echo   build.bat main          # Latest development version
        echo   build.bat develop       # Development branch
        echo   build.bat v5.7.0        # Specific version tag
        cd ..
        rmdir /s /q "%BUILD_DIR%"
        pause
        exit /b 1
    )
    cd ..
)

cd "%BUILD_DIR%"
echo [OK] Repository cloned successfully
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
    cd ..
    echo.
    echo =========================================
    echo [X] BUILD FAILED
    echo =========================================
    echo.
    echo Please check the output above for errors.
    echo.
    echo Build directory: %CD%\%BUILD_DIR%
    pause
    exit /b 1
)

REM Find the built JAR file
set JAR_FILE=
for /f "delims=" %%f in ('dir /b /s build\libs\*-all.jar 2^>nul') do (
    set JAR_FILE=%%f
    goto :found
)

:found
if defined JAR_FILE (
    REM Copy JAR to parent directory for easy access
    for %%f in ("%JAR_FILE%") do set JAR_NAME=%%~nxf
    copy "%JAR_FILE%" "..\!JAR_NAME!" >nul
    
    cd ..
    
    echo.
    echo =========================================
    echo [OK] BUILD SUCCESSFUL!
    echo =========================================
    echo.
    echo The plugin JAR file has been created at:
    echo   %CD%\!JAR_NAME!
    echo.
    echo The source code is in: %CD%\%BUILD_DIR%
    echo.
    echo To use the plugin:
    echo   1. Copy the JAR file to your server's 'plugins' folder
    echo   2. Restart your server
    echo.
    echo To clean up the build directory:
    echo   rmdir /s /q %BUILD_DIR%
    echo.
) else (
    cd ..
    echo.
    echo =========================================
    echo [X] BUILD FAILED
    echo =========================================
    echo.
    echo The build completed but the JAR file was not found.
    echo Please check the output above for errors.
    echo.
    echo Build directory: %CD%\%BUILD_DIR%
)

pause
