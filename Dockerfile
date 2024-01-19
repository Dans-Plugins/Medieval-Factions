FROM ubuntu

# Install dependencies
RUN apt-get update
RUN apt-get install -y git \
    openjdk-17-jdk \
    openjdk-17-jre \
    wget \
    locales

# Create server directory
WORKDIR /testmcserver

# Build server
RUN wget -O BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
RUN git config --global --unset core.autocrlf || :
RUN java -jar BuildTools.jar --rev 1.20.4
RUN echo "eula=true" > eula.txt
RUN mkdir plugins

# set locale to support us, de, fr, & br
RUN locale-gen en_US.UTF-8 de_DE.UTF-8 fr_FR.UTF-8 pt_BR.UTF-8 && \
    update-locale LANG=en_US.UTF-8 LANGUAGE=en_US.UTF-8 LC_ALL=en_US.UTF-8 && \
    dpkg-reconfigure --frontend=noninteractive locales

# Build plugin
COPY . .
RUN ./gradlew build
RUN cp build/libs/*-all.jar plugins

# Run server
EXPOSE 25565
ENTRYPOINT java -jar spigot-1.20.4.jar