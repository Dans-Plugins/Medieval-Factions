#!/bin/bash
# Standalone Build Script for Medieval Factions plugin
# This script handles everything: checking dependencies, cloning the repo, and building the JAR
# Usage: ./build.sh [version|branch]
#   - No argument: builds the latest main branch
#   - With argument: builds the specified version tag or branch (e.g., v5.7.0 or develop)

set -e

REPO_URL="https://github.com/Dans-Plugins/Medieval-Factions.git"
BUILD_DIR="MedievalFactions-build"
VERSION_OR_BRANCH="${1:-main}"

echo "========================================="
echo "Medieval Factions - Standalone Build Script"
echo "========================================="
echo ""
echo "This script will:"
echo "  1. Check for required dependencies (Java 17+, Git)"
echo "  2. Clone the Medieval Factions repository"
echo "  3. Build the plugin JAR file"
echo ""

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo "ERROR: Git is not installed"
    echo ""
    echo "Please install Git:"
    echo "  - Ubuntu/Debian: sudo apt-get install git"
    echo "  - CentOS/RHEL: sudo yum install git"
    echo "  - macOS: brew install git (or install Xcode Command Line Tools)"
    echo "  - Or download from: https://git-scm.com/downloads"
    exit 1
fi

echo "✓ Git is installed"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 17 or higher from:"
    echo "  - https://adoptium.net/ (recommended)"
    echo "  - https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

echo "✓ Java is installed"

# Check Java version
# This extracts the major version number from various Java version formats
JAVA_VERSION_OUTPUT=$(java -version 2>&1 | head -n 1)

# Extract version string from quotes
VERSION_STRING=$(echo "$JAVA_VERSION_OUTPUT" | sed -n 's/.*version "\(.*\)".*/\1/p')

# Remove leading '1.' for old Java versions (e.g., 1.8.0 -> 8.0)
VERSION_STRING=$(echo "$VERSION_STRING" | sed 's/^1\.//')

# Extract major version number (first part before '.' or '-' or '+')
JAVA_VERSION=$(echo "$VERSION_STRING" | cut -d'.' -f1 | cut -d'-' -f1 | cut -d'+' -f1)

# Remove any non-numeric characters
JAVA_VERSION=$(echo "$JAVA_VERSION" | tr -cd '0-9')

# Validate that we extracted a numeric version and check if it meets requirements
if [ -z "$JAVA_VERSION" ]; then
    echo "ERROR: Could not determine Java version"
    echo "Please install Java 17 or higher from:"
    echo "  - https://adoptium.net/ (recommended)"
    echo "  - https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

if [ "$JAVA_VERSION" -lt 17 ] 2>/dev/null; then
    echo "ERROR: Java 17 or higher is required"
    echo "Current version: $JAVA_VERSION"
    echo "Please install Java 17 or higher from:"
    echo "  - https://adoptium.net/ (recommended)"
    echo "  - https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

echo "✓ Java version $JAVA_VERSION is compatible"
echo ""

# Clone or update the repository
if [ -d "$BUILD_DIR" ]; then
    echo "Build directory already exists. Cleaning up..."
    rm -rf "$BUILD_DIR"
fi

echo "Cloning Medieval Factions repository..."
echo "Repository: $REPO_URL"
echo "Version/Branch: $VERSION_OR_BRANCH"
echo ""

git clone --depth 1 --branch "$VERSION_OR_BRANCH" "$REPO_URL" "$BUILD_DIR" 2>&1 || {
    echo ""
    echo "Failed to clone branch/tag '$VERSION_OR_BRANCH'"
    echo "Trying to clone and checkout instead..."
    git clone "$REPO_URL" "$BUILD_DIR"
    cd "$BUILD_DIR"
    git checkout "$VERSION_OR_BRANCH" 2>&1 || {
        echo ""
        echo "ERROR: Could not find version/branch '$VERSION_OR_BRANCH'"
        echo ""
        echo "Please specify a valid branch or tag, for example:"
        echo "  ./build.sh main          # Latest development version"
        echo "  ./build.sh develop       # Development branch"
        echo "  ./build.sh v5.7.0        # Specific version tag"
        cd ..
        rm -rf "$BUILD_DIR"
        exit 1
    }
    cd ..
}

cd "$BUILD_DIR"
echo "✓ Repository cloned successfully"
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
JAR_FILE=$(find build/libs -name "*-all.jar" -type f 2>/dev/null | head -n 1)
if [ -n "$JAR_FILE" ] && [ -f "$JAR_FILE" ]; then
    # Copy JAR to parent directory for easy access
    JAR_NAME=$(basename "$JAR_FILE")
    cp "$JAR_FILE" "../$JAR_NAME"
    
    cd ..
    
    echo ""
    echo "========================================="
    echo "✓ BUILD SUCCESSFUL!"
    echo "========================================="
    echo ""
    echo "The plugin JAR file has been created at:"
    echo "  $(pwd)/$JAR_NAME"
    echo ""
    echo "The source code is in: $(pwd)/$BUILD_DIR"
    echo ""
    echo "To use the plugin:"
    echo "  1. Copy the JAR file to your server's 'plugins' folder"
    echo "  2. Restart your server"
    echo ""
    echo "To clean up the build directory:"
    echo "  rm -rf $BUILD_DIR"
    echo ""
else
    cd ..
    echo ""
    echo "========================================="
    echo "✗ BUILD FAILED"
    echo "========================================="
    echo ""
    echo "The build completed but the JAR file was not found."
    echo "Please check the output above for errors."
    echo ""
    echo "Build directory: $(pwd)/$BUILD_DIR"
    exit 1
fi
