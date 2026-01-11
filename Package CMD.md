jpackage ^
--input staging ^
--name "MetadataViewer" ^
--main-jar MetadataViewer-1.1.0-SNAPSHOT.jar ^
--main-class com.nilsson.metadataviewer.Launcher ^
--type app-image ^
--icon src/main/resources/icon.ico ^
--app-version 1.1.0 ^
--vendor "Alexander Nilsson" ^
--add-modules java.se,jdk.unsupported,jdk.charsets