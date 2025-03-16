#!/bin/bash

# Function: Setup server
setup_server() {
    if [ -z "$(ls -A /testmcserver)" ] || [ "$OVERWRITE_EXISTING_SERVER" = "true" ]; then
        echo "Setting up server..."
        
        if [ "$OVERWRITE_EXISTING_SERVER" = "true" ]; then
            echo "Deleting existing server contents..."
            rm -rf /testmcserver/*
        fi

        echo "Copying server JAR..."
        cp /testmcserver-build/spigot-"${MINECRAFT_VERSION}".jar /testmcserver/spigot-"${MINECRAFT_VERSION}".jar

        echo "Creating plugins directory..."
        mkdir /testmcserver/plugins
    else
        echo "Server is already set up."
    fi
}

# Function: Setup ops.json file
setup_ops_file() {
    echo "Creating ops.json file..."
    cat <<EOF > /testmcserver/ops.json
    [
      {
        "uuid": "0a9fa342-3139-49d7-8acb-fcf4d9c1f0ef",
        "name": "DanTheTechMan",
        "level": 4,
        "bypassesPlayerLimit": false
      }
    ]
EOF
}

# Function: Accept EULA
accept_eula() {
    echo "Accepting Minecraft EULA..."
    echo "eula=true" > /testmcserver/eula.txt
}

# Function: Delete lang directory
delete_lang_directory() {
    echo "Deleting lang directory..."
    rm -rf /testmcserver/plugins/MedievalFactions/lang
}

# Function: Copy the latest plugin JAR with timestamp check
copy_latest_plugin_jar() {
    local nameOfJar=$(ls /testmcserver-build/MedievalFactions/build/libs/*-all.jar)
    local currentDate=$(date +%s)
    local jarDate=$(date -r "$nameOfJar" +%s)
    local diff=$((currentDate - jarDate))

    if [ $diff -gt 300 ]; then
        echo "WARNING: The plugin JAR is older than 5 minutes."
    fi

    echo "Copying plugin JAR..."
    cp "$nameOfJar" /testmcserver/plugins
}

# Function: Generic plugin manager for enabling or disabling
manage_plugin_dependencies() {
    local plugin_name="$1"
    local enabled_var="$2"

    if [ "${!enabled_var}" = "true" ]; then
        echo "${plugin_name} enabled. Copying plugin JAR..."
        cp /resources/jars/${plugin_name}-*.jar /testmcserver/plugins
    else
        echo "${plugin_name} disabled. Deleting plugin JAR if it exists..."
        rm -f /testmcserver/plugins/${plugin_name}-*.jar
    fi
}

# Function: Update Bluemap configuration
update_bluemap_config() {
    echo "Updating Bluemap configuration..."
    sed -i 's/accept-download: false/accept-download: true/g' /testmcserver/plugins/bluemap/core.conf
}

# Function: Start server
start_server() {
    echo "Starting server..."
    java -jar /testmcserver/spigot-"${MINECRAFT_VERSION}".jar
}

# Main Process
echo "Running 'post-create.sh' script..."
setup_server
setup_ops_file
accept_eula
delete_lang_directory
copy_latest_plugin_jar

# Manage plugins
manage_plugin_dependencies "currencies" "CURRENCIES_ENABLED"
manage_plugin_dependencies "Dynmap" "DYNMAP_ENABLED"
manage_plugin_dependencies "bluemap" "BLUEMAP_ENABLED"
if [ "$BLUEMAP_ENABLED" = "true" ]; then
    update_bluemap_config
fi
manage_plugin_dependencies "PlaceholderAPI" "PLACEHOLDER_API_ENABLED"

# Start Server
start_server