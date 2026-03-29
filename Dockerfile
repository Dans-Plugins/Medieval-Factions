FROM ubuntu

# Install dependencies: JDK 17 for Gradle compilation toolchain, JDK 21 for MC 1.21 server runtime and BuildTools
RUN apt update
RUN DEBIAN_FRONTEND=noninteractive apt install -y wget git openjdk-17-jdk openjdk-21-jdk openjdk-21-jre

# Set JDK 21 as default for BuildTools and server runtime; Gradle auto-detects JDK 17 via toolchain
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

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
RUN mkdir -p /testmcserver-build/MedievalFactions/build && cp -r /tmp/mf-build/build/libs /testmcserver-build/MedievalFactions/build/libs

# Copy resources and make post-create.sh executable
COPY ./.testcontainer /resources
RUN chmod +x /resources/post-create.sh

# Run server
WORKDIR /testmcserver
EXPOSE 25565
ENTRYPOINT /resources/post-create.sh
