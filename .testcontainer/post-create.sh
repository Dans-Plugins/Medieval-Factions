echo "Running 'post-create.sh' script..."
if [ -z "$(ls -A /testmcserver)" ] || [ "$OVERWRITE_EXISTING_SERVER" = "true" ]; then
    echo "Setting up server..."

    # if OVERWRITE_EXISTING_SERVER is true, delete the server directory
    if [ "$OVERWRITE_EXISTING_SERVER" = "true" ]; then
        echo "OVERWRITE_EXISTING_SERVER is set to 'true'. Deleting contents of /testmcserver..."
        rm -rf /testmcserver/*
    fi

    # Copy server JAR
cp /testmcserver-build/spigot-"${MINECRAFT_VERSION}".jar /testmcserver/spigot-"${MINECRAFT_VERSION}".jar

    # Create plugins directory
    mkdir /testmcserver/plugins

    # Create ops.json file on the fly
    echo "Creating ops.json file..."
    echo '[
      {
        "uuid": "0a9fa342-3139-49d7-8acb-fcf4d9c1f0ef",
        "name": "DanTheTechMan",
        "level": 4,
        "bypassesPlayerLimit": false
      }
    ]' >> /testmcserver/ops.json

    # Accept EULA
    cd /testmcserver && echo "eula=true" > eula.txt
else
  echo "Server is already set up. To overwrite the existing server, set the 'OVERWRITE_EXISTING_SERVER' environment variable to 'true'."
fi

# Always delete lang directory to get the latest translations
echo "Deleting lang directory..."
rm -rf /testmcserver/plugins/MedievalFactions/lang

# Always copy the latest plugin JAR
nameOfJar=$(ls /testmcserver-build/MedievalFactions/build/libs/*-all.jar)
currentDate=$(date +%s)
jarDate=$(date -r "$nameOfJar" +%s)
diff=$((currentDate - jarDate))

if [ $diff -gt 300 ]; then
    echo "WARNING: The plugin JAR is older than 5 minutes. It may be necessary to rebuild the plugin."
fi

echo "Copying plugin JAR... (created $diff seconds ago)"
cp "$nameOfJar" /testmcserver/plugins

# Copy or delete Currencies JAR based on environment variable
if [ "$CURRENCIES_ENABLED" = "true" ]; then
      echo "Currencies enabled. Copying Currencies plugin from /resources/jars..."
      cp /resources/jars/currencies-*.jar /testmcserver/plugins
else
    echo "Currencies disabled. Deleting Currencies plugin if it exists..."
    rm -f /testmcserver/plugins/currencies-*.jar
fi

# Copy or delete Dynmap JAR based on environment variable
if [ "$DYNMAP_ENABLED" = "true" ]; then
      echo "Dynmap enabled. Copying Dynmap plugin from /resources/jars..."
      cp /resources/jars/Dynmap-*.jar /testmcserver/plugins
else
    echo "Dynmap disabled. Deleting Dynmap plugin if it exists..."
    rm -f /testmcserver/plugins/Dynmap-*.jar
fi

# Copy or delete Bluemap JAR based on environment variable
if [ "$BLUEMAP_ENABLED" = "true" ]; then
      echo "Bluemap enabled. Copying Bluemap plugin from /resources/jars..."
      cp /resources/jars/bluemap-*.jar /testmcserver/plugins

      # update /testmcserver/plugins/bluemap/core.conf to have accept-download: true
      echo "Updating /testmcserver/plugins/bluemap/core.conf to have accept-download: true..."
      sed -i 's/accept-download: false/accept-download: true/g' /testmcserver/plugins/bluemap/core.conf
else
    echo "Bluemap disabled. Deleting Bluemap plugin if it exists..."
    rm -f /testmcserver/plugins/bluemap-*.jar
fi

# Copy or delete PlaceholderAPI JAR based on environment variable
if [ "$PLACEHOLDER_API_ENABLED" = "true" ]; then
      echo "PlaceholderAPI enabled. Copying PlaceholderAPI plugin from /resources/jars..."
      cp /resources/jars/PlaceholderAPI-*.jar /testmcserver/plugins
else
    echo "PlaceholderAPI disabled. Deleting PlaceholderAPI plugin if it exists..."
    rm -f /testmcserver/plugins/PlaceholderAPI-*.jar
fi

echo "Starting server..."
java -jar /testmcserver/spigot-"${MINECRAFT_VERSION}".jar
