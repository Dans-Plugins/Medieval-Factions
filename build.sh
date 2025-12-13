#!/bin/bash
# Build script for Medieval Factions plugin
# This script builds the plugin JAR file with all dependencies included

set -e

echo "========================================="
echo "Medieval Factions - Build Script"
echo "========================================="
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 17 or higher from:"
    echo "  - https://adoptium.net/ (recommended)"
    echo "  - https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ -z "$JAVA_VERSION" ]; then
    # Try alternative version check for newer Java versions
    JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "\K[0-9]+')
fi

echo "Detected Java version: $JAVA_VERSION"

if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "ERROR: Java 17 or higher is required"
    echo "Current version: $JAVA_VERSION"
    echo "Please install Java 17 or higher from:"
    echo "  - https://adoptium.net/ (recommended)"
    echo "  - https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

echo "✓ Java version is compatible"
echo ""

# Build the plugin
echo "Building Medieval Factions plugin..."
echo "This may take a few minutes on the first run as dependencies are downloaded..."
echo ""

# Use the Gradle wrapper to build the shadowJar (which contains all dependencies)
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew clean shadowJar --no-daemon
else
    echo "ERROR: gradlew wrapper not found!"
    exit 1
fi

# Check if build was successful
if [ -f "build/libs/Medieval-Factions-"*"-all.jar" ]; then
    echo ""
    echo "========================================="
    echo "✓ BUILD SUCCESSFUL!"
    echo "========================================="
    echo ""
    echo "The plugin JAR file has been created at:"
    ls -1 build/libs/*-all.jar | head -1
    echo ""
    echo "To use the plugin:"
    echo "1. Copy the JAR file to your server's 'plugins' folder"
    echo "2. Restart your server"
    echo ""
else
    echo ""
    echo "========================================="
    echo "✗ BUILD FAILED"
    echo "========================================="
    echo ""
    echo "The build completed but the JAR file was not found."
    echo "Please check the output above for errors."
    exit 1
fi
