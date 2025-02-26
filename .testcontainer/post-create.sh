echo "Running 'post-create.sh' script..."
if [ -z "$(ls -A /testmcserver)" ]; then
    echo "Setting up server..."
    # Copy server JAR
    cp /testmcserver-build/spigot-1.20.4.jar /testmcserver/spigot-1.20.4.jar

    # Create plugins directory
    mkdir /testmcserver/plugins

    # Create ops.json file on the fly
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

# Always copy the latest plugin JAR
cp /testmcserver-build/MedievalFactions/build/libs/*-all.jar /testmcserver/plugins

java -jar /testmcserver/spigot-1.20.4.jar
