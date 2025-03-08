echo "Running 'post-create.sh' script..."
if [ -z "$(ls -A /testmcserver)" ]; then
    echo "Setting up server..."
    # Copy server JAR
    cp /testmcserver-build/spigot-1.20.4.jar /testmcserver/spigot-1.20.4.jar

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
    echo "Server is already set up."
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

# Copy or delete Dynmap JAR based on environment variable
if [ "$DYNMAP_ENABLED" = "true" ]; then
      echo "Dynmap enabled. Copying Dynmap plugin from /resources/jars..."
      cp /resources/jars/Dynmap-*.jar /testmcserver/plugins
else
    echo "Dynmap disabled. Deleting Dynmap plugin if it exists..."
    rm -f /testmcserver/plugins/Dynmap-*.jar
fi

echo "Starting server..."
java -jar /testmcserver/spigot-1.20.4.jar
