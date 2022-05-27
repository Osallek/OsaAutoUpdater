Osa Auto Updater

Small project to allow auto update of our other projects using Github Releases.

mvn clean package -DskipTests -DUPDATER_NAME=OsaModEditorAutoUpdater -DREPO_NAME=OsaModEditor -DEXECUTABLE_NAME=OsaModEditor-0.0.6.jar -P native
