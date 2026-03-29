FROM ubuntu

# Install dependencies
RUN apt update
RUN DEBIAN_FRONTEND=noninteractive apt install -y wget git openjdk-21-jdk openjdk-21-jre

# Build Ponder dependency and publish to Maven local
WORKDIR /tmp/ponder-build
RUN git clone https://github.com/Dans-Plugins/Ponder.git .
RUN git checkout 2.0.0
RUN chmod +x gradlew
RUN ./gradlew publishToMavenLocal

# Build the Medieval Factions plugin
WORKDIR /tmp/mf-build
COPY . .
RUN chmod +x gradlew
RUN ./gradlew clean shadowJar

# Build server
WORKDIR /testmcserver-build
RUN wget -O BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
RUN git config --global --unset core.autocrlf || :
RUN java -jar BuildTools.jar --rev 1.21.11

# Copy plugin jar from build output
RUN cp -r /tmp/mf-build/build/libs /testmcserver-build/MedievalFactions/build/libs

# Copy resources and make post-create.sh executable
COPY ./.testcontainer /resources
RUN chmod +x /resources/post-create.sh

# Run server
WORKDIR /testmcserver
EXPOSE 25565
ENTRYPOINT /resources/post-create.sh
