#!/bin/bash

# Script to rebuild and hot-reload the Medieval Factions plugin during development
# This script requires the test server to be running with ServerUtils plugin

# Build the plugin
echo "Building Medieval Factions plugin..."
./gradlew build

if [ $? -ne 0 ]; then
    echo "Build failed. Exiting."
    exit 1
fi

# Get the container ID
CONTAINER_ID=$(docker ps -qf "name=mf-test-mc-server")

if [ -z "$CONTAINER_ID" ]; then
    echo "Error: Test server container not found. Make sure to start it with './up.sh'"
    exit 1
fi

# Copy the new plugin jar to the server
echo "Copying new plugin jar to test server..."
LATEST_JAR=$(find build/libs -name "*-all.jar" -type f -print -quit)

if [ -z "$LATEST_JAR" ]; then
    echo "Error: No plugin jar found in build/libs"
    exit 1
fi

docker cp "$LATEST_JAR" "$CONTAINER_ID":/testmcserver/plugins/

# Execute the reload command via ServerUtils
echo "Reloading Medieval Factions plugin..."
docker exec "$CONTAINER_ID" /bin/bash -c "echo 'serverutils reload MedievalFactions' >> /tmp/reload_command"
echo "Plugin reload initiated. Check server console for confirmation."

echo ""
echo "To manually reload the plugin in-game or via console, use:"
echo "  /serverutils reload MedievalFactions"
echo ""
echo "Other useful ServerUtils commands:"
echo "  /serverutils list - List all plugins"
echo "  /serverutils unload MedievalFactions - Unload the plugin"
echo "  /serverutils load MedievalFactions - Load the plugin"