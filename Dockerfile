FROM ubuntu

# Install dependencies: JDK 17 for Gradle builds, JDK 21 for MC 1.21 server runtime and BuildTools
RUN apt update
RUN DEBIAN_FRONTEND=noninteractive apt install -y wget git openjdk-17-jdk openjdk-21-jdk openjdk-21-jre

# Use JDK 17 for Gradle builds (both Ponder and MF use Gradle 7.x which doesn't support JDK 21)
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
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

# Switch to JDK 21 for BuildTools and MC 1.21 server runtime
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Build server
WORKDIR /testmcserver-build
RUN wget -O BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
RUN git config --global --unset core.autocrlf || :
RUN java -jar BuildTools.jar --rev 1.21.1

# Copy plugin jar from build output
RUN mkdir -p /testmcserver-build/MedievalFactions/build && cp -r /tmp/mf-build/build/libs /testmcserver-build/MedievalFactions/build/libs

# Copy resources and make post-create.sh executable
COPY ./.testcontainer /resources
RUN chmod +x /resources/post-create.sh

# Slimefun (GPL-3.0) is downloaded rather than committed: dev build 1160, which supports 1.21.x.
# The checksum pins the exact file; the jar is only copied into the server when SLIMEFUN_ENABLED=true.
RUN wget -O /resources/jars/Slimefun-dev-1160.jar https://blob.build/dl/Slimefun4/Dev/1160 \
    && echo "8aa3ca4bcda767f04713511f2d2fb2be3be0a5a08a8f72543f1fbc4cbd773caa  /resources/jars/Slimefun-dev-1160.jar" | sha256sum -c -

# Run server
WORKDIR /testmcserver
EXPOSE 25565
ENTRYPOINT /resources/post-create.sh
