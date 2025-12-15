#!/bin/bash
# Medieval Factions Build Script for Linux/Mac
# This script builds the Medieval Factions plugin jar
#
# Usage:
#   ./build.sh           - Build in current directory (requires repository)
#   ./build.sh standalone - Clone repository and build

set -e  # Exit on error (we'll handle errors explicitly where needed)

echo "================================================"
echo "Medieval Factions Build Script"
echo "================================================"
echo ""

# Check if running in standalone mode
STANDALONE_MODE=false
if [ "$1" == "standalone" ]; then
    STANDALONE_MODE=true
    echo "Running in STANDALONE mode"
    echo ""
fi

# If standalone mode, check if we need to clone the repository
if [ "$STANDALONE_MODE" = true ]; then
    # Check if git is installed
    if ! command -v git &> /dev/null; then
        echo "ERROR: Git is not installed or not in PATH"
        echo "Git is required for standalone mode to clone the repository."
        echo ""
        echo "On Ubuntu/Debian: sudo apt install git"
        echo "On Fedora/RHEL:   sudo dnf install git"
        echo "On macOS:         brew install git"
        echo ""
        echo "If you continue to experience issues, please report them at:"
        echo "https://github.com/Dans-Plugins/Medieval-Factions/issues"
        echo ""
        exit 1
    fi
    
    # Check if we're already in the repository
    if [ ! -f "build.gradle" ] || [ ! -f "settings.gradle" ]; then
        echo "Cloning Medieval-Factions repository..."
        REPO_URL="https://github.com/Dans-Plugins/Medieval-Factions.git"
        REPO_DIR="Medieval-Factions"
        
        if [ -d "$REPO_DIR" ]; then
            echo "Directory $REPO_DIR already exists. Using existing directory."
            cd "$REPO_DIR"
        else
            git clone "$REPO_URL" "$REPO_DIR"
            if [ $? -ne 0 ]; then
                echo ""
                echo "ERROR: Failed to clone repository"
                echo "Please check your internet connection and try again."
                echo ""
                echo "If the problem persists, please report it at:"
                echo "https://github.com/Dans-Plugins/Medieval-Factions/issues"
                echo ""
                exit 1
            fi
            cd "$REPO_DIR"
        fi
        echo "Repository cloned successfully!"
        echo ""
    fi
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 17 or later."
    echo ""
    echo "On Ubuntu/Debian: sudo apt install openjdk-17-jdk"
    echo "On Fedora/RHEL:   sudo dnf install java-17-openjdk-devel"
    echo "On macOS:         brew install openjdk@17"
    echo ""
    echo "If you continue to experience issues, please report them at:"
    echo "https://github.com/Dans-Plugins/Medieval-Factions/issues"
    echo ""
    exit 1
fi

echo "Checking Java version..."
# Get Java version - handle both old (1.8) and new (9+) version formats
java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
# Extract major version: for "1.8.0_xxx" get 8, for "11.0.x" get 11
if [[ $java_version == 1.* ]]; then
    # Old versioning scheme (Java 8 and earlier): 1.8.0_xxx -> 8
    java_major=$(echo $java_version | cut -d. -f2)
else
    # New versioning scheme (Java 9+): 11.0.x -> 11
    java_major=$(echo $java_version | cut -d. -f1)
fi

if [ "$java_major" -lt 17 ]; then
    echo "ERROR: Java 17 or later is required"
    echo "Current Java version: $java_version"
    echo "Please install Java 17 or later."
    echo ""
    echo "If you continue to experience issues, please report them at:"
    echo "https://github.com/Dans-Plugins/Medieval-Factions/issues"
    echo ""
    exit 1
fi

echo "Java version check passed (Java $java_major)"
echo ""

# Make gradlew executable if it isn't already
if [ ! -x "./gradlew" ]; then
    echo "Making gradlew executable..."
    chmod +x ./gradlew
fi

# Clean previous builds
echo "Cleaning previous builds..."
set +e  # Don't exit on clean failure
./gradlew clean --no-daemon
clean_result=$?
set -e  # Re-enable exit on error
if [ $clean_result -ne 0 ]; then
    echo "WARNING: Clean task failed, continuing anyway..."
fi
echo ""

# Build the project
echo "Building Medieval Factions..."
echo "This may take a few minutes on first run..."
echo ""
set +e  # Don't exit immediately, we want to show a custom error message
./gradlew shadowJar --no-daemon
build_result=$?
set -e

if [ $build_result -ne 0 ]; then
    echo ""
    echo "================================================"
    echo "BUILD FAILED"
    echo "================================================"
    echo ""
    echo "The build failed. This could be due to:"
    echo "  1. Network issues downloading dependencies"
    echo "  2. Missing Java Development Kit (JDK)"
    echo "  3. Compilation errors"
    echo ""
    echo "Try running the build again, as some dependencies"
    echo "may have been cached and could work on retry."
    echo ""
    echo "If the problem persists, please report it at:"
    echo "https://github.com/Dans-Plugins/Medieval-Factions/issues"
    echo ""
    exit 1
fi

# Check if the jar was created
if ! ls build/libs/medieval-factions-*-all.jar 1> /dev/null 2>&1; then
    echo ""
    echo "ERROR: Build completed but jar file not found"
    echo "Expected location: build/libs/medieval-factions-*-all.jar"
    echo ""
    echo "Please report this issue at:"
    echo "https://github.com/Dans-Plugins/Medieval-Factions/issues"
    echo ""
    exit 1
fi

echo ""
echo "================================================"
echo "BUILD SUCCESSFUL"
echo "================================================"
echo ""
echo "The plugin jar has been built successfully!"
echo ""
echo "Location: build/libs/"
ls -lh build/libs/medieval-factions-*-all.jar
echo ""
echo "You can now copy this jar file to your server's plugins folder."
echo ""
