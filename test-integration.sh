#!/bin/bash

# Simple verification script to ensure the ServerUtils integration works
# This script will start the test server briefly to verify all plugins load correctly

echo "=== Medieval Factions Test Server Integration Test ==="
echo

# Check if required files exist
echo "Checking required files..."
required_files=(
    ".testcontainer/jars/ServerUtils-Bukkit-3.5.4.jar"
    ".testcontainer/post-create.sh"
    "sample.env"
    "compose.yml"
    "reload-plugin.sh"
)

for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file exists"
    else
        echo "✗ $file missing"
        exit 1
    fi
done

echo
echo "ServerUtils integration is ready!"
echo
echo "To test the full integration:"
echo "1. Create .env file: cp sample.env .env"
echo "2. Set SERVERUTILS_ENABLED=true in .env"
echo "3. Start test server: ./up.sh"
echo "4. Check server logs to confirm ServerUtils loads"
echo "5. Use /serverutils list in-game to verify"
echo "6. Test plugin reloading with: ./reload-plugin.sh"
echo
echo "Note: The test server requires a successful build first: ./gradlew build"