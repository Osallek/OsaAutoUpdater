# Osa Auto Updater

Small project to allow auto update of our other projects using Github Releases.

## To build
mvn clean package -DEXECUTABLE_NAME=OsaSaveExtractor.jar -DREPO_NAME=OsaSaveExtractor -DUPDATER_NAME=OsaSaveExtractorUpdater -DskipTests -DskipNativeTests
