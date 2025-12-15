#!/bin/bash
# Medieval Factions Build Script for Linux/Mac
# This script builds the Medieval Factions plugin jar

set -e  # Exit on error (we'll handle errors explicitly where needed)

echo "================================================"
echo "Medieval Factions Build Script"
echo "================================================"
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 17 or later."
    echo ""
    echo "On Ubuntu/Debian: sudo apt install openjdk-17-jdk"
    echo "On Fedora/RHEL:   sudo dnf install java-17-openjdk-devel"
    echo "On macOS:         brew install openjdk@17"
    echo ""
    exit 1
fi

echo "Checking Java version..."
# Get Java version
java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [ "$java_version" -lt 17 ]; then
    echo "ERROR: Java 17 or later is required"
    echo "Current Java version: $java_version"
    echo "Please install Java 17 or later."
    echo ""
    exit 1
fi

echo "Java version check passed (Java $java_version)"
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
    exit 1
fi

# Check if the jar was created
if ! ls build/libs/medieval-factions-*-all.jar 1> /dev/null 2>&1; then
    echo ""
    echo "ERROR: Build completed but jar file not found"
    echo "Expected location: build/libs/medieval-factions-*-all.jar"
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
ls -lh build/libs/*-all.jar
echo ""
echo "You can now copy this jar file to your server's plugins folder."
echo ""
