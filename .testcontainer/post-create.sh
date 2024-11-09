echo "Running 'post-create.sh' script..."
if [ -z "$(ls -A /testmcserver)" ]; then
    echo "Setting up server..."
    # Copy server JAR
    cp /testmcserver-build/spigot-1.20.4.jar /testmcserver/spigot-1.20.4.jar

    # Create plugins directory
    mkdir /testmcserver/plugins

    # Install Medieval Factions
    cp /testmcserver-build/MedievalFactions/build/libs/*-all.jar /testmcserver/plugins

    # Copy config files
    cp /resources/ops.json /testmcserver

    # Accept EULA
    cd /testmcserver && echo "eula=true" > eula.txt
else
    echo "Server is already set up."
fi

java -jar /testmcserver/spigot-1.20.4.jar
