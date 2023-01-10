# Osa Auto Updater

Small project to allow auto update of our other projects using GitHub Releases.

## To build
### Native
```
mvn clean package -DEXECUTABLE_NAME=OsaSaveExtractor.jar -DREPO_NAME=OsaSaveExtractor -DUPDATER_NAME=OsaSaveExtractorUpdater -DJAVA_VERSION=17 -DskipTests -Pnative
```
### Jar
```
mvn clean package -DEXECUTABLE_NAME=OsaSaveExtractor.jar -DREPO_NAME=OsaSaveExtractor -DUPDATER_NAME=OsaSaveExtractorUpdater -DskipTests
```
