# Osa Auto Updater

Small project to allow auto update of our other projects using GitHub Releases.

## To build

### Osa Save Extractor
#### Native
```
mvn clean package -DEXECUTABLE_NAME=OsaSaveExtractor.jar -DREPO_NAME=OsaSaveExtractor -DUPDATER_NAME=OsaSaveExtractorUpdater -DJAVA_VERSION=17 -DskipTests -Pnative
```
#### Jar
```
mvn clean package -DEXECUTABLE_NAME=OsaSaveExtractor.jar -DREPO_NAME=OsaSaveExtractor -DUPDATER_NAME=OsaSaveExtractorUpdater -DskipTests
```

### Osa Save Editor
#### Native
```
mvn clean package -DEXECUTABLE_NAME=OsaSaveEditor.jar -DREPO_NAME=OsaSaveEditor -DUPDATER_NAME=OsaSaveEditorUpdater -DJAVA_VERSION=17 -DskipTests -Pnative
```
#### Jar
```
mvn clean package -DEXECUTABLE_NAME=OsaSaveEditor.jar -DREPO_NAME=OsaSaveEditor -DUPDATER_NAME=OsaSaveEditorUpdater -DskipTests
```
