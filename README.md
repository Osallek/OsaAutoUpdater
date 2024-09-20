# Osa Auto Updater

Small project to allow auto update of our other projects using GitHub Releases.

## To build

### Osa Save Extractor
#### Native
```
mvn clean package -DEXECUTABLE_NAME=OsaSaveExtractor.jar -DREPO_NAME=OsaSaveExtractor -DUPDATER_NAME=OsaSaveExtractorUpdater -DMIN_JAVA_VERSION=21 -DskipTests -Pnative
```
#### Jar
```
mvn clean package -DEXECUTABLE_NAME=OsaSaveExtractor.jar -DREPO_NAME=OsaSaveExtractor -DUPDATER_NAME=OsaSaveExtractorUpdater -DskipTests
```

### Osa Save Editor
#### Native
```
mvn clean package -DEXECUTABLE_NAME=OsaSaveEditor.jar -DREPO_NAME=OsaSaveEditor -DUPDATER_NAME=OsaSaveEditorUpdater -DMIN_JAVA_VERSION=21 -DskipTests -Pnative
```
#### Jar
```
mvn clean package -DEXECUTABLE_NAME=OsaSaveEditor.jar -DREPO_NAME=OsaSaveEditor -DUPDATER_NAME=OsaSaveEditorUpdater -DskipTests
```
