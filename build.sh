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
echo "  1. Check for required dependencies (Java 21+, Git)"
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
    echo "Please install Java 21 or higher from:"
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

# Validate that we extracted a numeric version
if [ -z "$JAVA_VERSION" ] || ! [[ "$JAVA_VERSION" =~ ^[0-9]+$ ]]; then
    echo "ERROR: Could not determine Java version"
    echo "Please install Java 21 or higher from:"
    echo "  - https://adoptium.net/ (recommended)"
    echo "  - https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

echo "Detected Java version: $JAVA_VERSION"

# Check if Java 21 or higher is installed
if [ "$JAVA_VERSION" -ge 21 ] 2>/dev/null; then
    echo "✓ Java version $JAVA_VERSION is compatible"
    echo ""
else
    echo "ERROR: Java 21 or higher is required to build Medieval Factions"
    echo "Current version: Java $JAVA_VERSION"
    echo ""
    echo "This project requires Java 21 due to dependencies and build tooling."
    echo ""
    
    # Offer to install Java 21 automatically
    echo "Would you like to install Java 21 automatically? (y/n)"
    echo "(Note: This will use your system's package manager and may require sudo/administrator privileges)"
    read -r INSTALL_JAVA
    
    if [ "$INSTALL_JAVA" = "y" ] || [ "$INSTALL_JAVA" = "Y" ]; then
        echo ""
        echo "Installing Java 21..."
        echo "(You may be prompted for your password)"
        echo ""
        
        # Detect OS and install accordingly
        if [[ "$OSTYPE" == "linux-gnu"* ]]; then
            # Linux
            if command -v apt-get &> /dev/null; then
                # Debian/Ubuntu
                echo "Detected Debian/Ubuntu system"
                echo "Installing OpenJDK 21 via apt..."
                sudo apt-get update
                sudo apt-get install -y openjdk-21-jdk
            elif command -v yum &> /dev/null; then
                # CentOS/RHEL
                echo "Detected CentOS/RHEL system"
                echo "Installing OpenJDK 21 via yum..."
                sudo yum install -y java-21-openjdk-devel
            elif command -v dnf &> /dev/null; then
                # Fedora
                echo "Detected Fedora system"
                echo "Installing OpenJDK 21 via dnf..."
                sudo dnf install -y java-21-openjdk-devel
            else
                echo "Could not detect package manager."
                echo "Please install Java 21 manually from:"
                echo "  - https://adoptium.net/temurin/releases/?version=21"
                exit 1
            fi
        elif [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            echo "Detected macOS system"
            if command -v brew &> /dev/null; then
                echo "Installing OpenJDK 21 via Homebrew..."
                brew install openjdk@21
                echo ""
                echo "Adding Java 21 to PATH..."
                
                # Get the actual Homebrew prefix (handles both Intel and Apple Silicon)
                BREW_PREFIX=$(brew --prefix openjdk@21)
                
                # Determine user's shell and update appropriate config file
                USER_SHELL=$(basename "$SHELL")
                case "$USER_SHELL" in
                    zsh)
                        echo "export PATH=\"$BREW_PREFIX/bin:\$PATH\"" >> ~/.zshrc
                        echo "Updated ~/.zshrc"
                        ;;
                    bash)
                        # macOS bash uses .bash_profile
                        if [ -f ~/.bash_profile ]; then
                            echo "export PATH=\"$BREW_PREFIX/bin:\$PATH\"" >> ~/.bash_profile
                            echo "Updated ~/.bash_profile"
                        else
                            echo "export PATH=\"$BREW_PREFIX/bin:\$PATH\"" >> ~/.bashrc
                            echo "Updated ~/.bashrc"
                        fi
                        ;;
                    *)
                        echo "Detected shell: $USER_SHELL"
                        echo "Please manually add to your shell profile:"
                        echo "  export PATH=\"$BREW_PREFIX/bin:\$PATH\""
                        ;;
                esac
                
                # Set PATH for current session
                export PATH="$BREW_PREFIX/bin:$PATH"
            else
                echo "Homebrew not found. Please install it first:"
                echo "  /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
                echo ""
                echo "Or install Java 21 manually from:"
                echo "  - https://adoptium.net/temurin/releases/?version=21"
                exit 1
            fi
        else
            echo "Unsupported operating system: $OSTYPE"
            echo "Please install Java 21 manually from:"
            echo "  - https://adoptium.net/temurin/releases/?version=21"
            exit 1
        fi
        
        # Verify installation
        echo ""
        echo "Verifying Java installation..."
        if command -v java &> /dev/null; then
            NEW_JAVA_VERSION_OUTPUT=$(java -version 2>&1 | head -n 1)
            NEW_VERSION_STRING=$(echo "$NEW_JAVA_VERSION_OUTPUT" | sed -n 's/.*version "\(.*\)".*/\1/p')
            NEW_VERSION_STRING=$(echo "$NEW_VERSION_STRING" | sed 's/^1\.//')
            NEW_JAVA_VERSION=$(echo "$NEW_VERSION_STRING" | cut -d'.' -f1 | cut -d'-' -f1 | cut -d'+' -f1)
            NEW_JAVA_VERSION=$(echo "$NEW_JAVA_VERSION" | tr -cd '0-9')
            
            if [ "$NEW_JAVA_VERSION" -ge 21 ] 2>/dev/null; then
                echo "✓ Java $NEW_JAVA_VERSION installed successfully!"
                echo ""
            else
                echo "✗ Java installation may have failed. Detected version: $NEW_JAVA_VERSION"
                echo "Please install Java 21 manually and try again."
                exit 1
            fi
        else
            echo "✗ Java installation failed."
            echo "Please install Java 21 manually from:"
            echo "  - https://adoptium.net/temurin/releases/?version=21"
            exit 1
        fi
    else
        echo ""
        echo "Java 21 installation declined."
        echo "Please install Java 21 manually from:"
        echo "  - https://adoptium.net/temurin/releases/?version=21 (recommended)"
        echo "  - https://www.oracle.com/java/technologies/downloads/#java21"
        echo ""
        echo "After installation, ensure Java 21 is in your PATH:"
        echo "  - Run: java -version"
        echo "  - It should show version 21 or higher"
        exit 1
    fi
fi

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
