# install ponder
mvn install:install-file -Dfile=${PATH_TO_PONDER_JAR} -DgroupId=preponderous -DartifactId=ponder -Dversion=${PONDER_VERSION} -Dpackaging=Jar

# install fiefs
mvn install:install-file -Dfile=${PATH_TO_FIEFS_JAR} -DgroupId=dansplugins -DartifactId=fiefs -Dversion=${FIEFS_VERSION} -Dpackaging=Jar