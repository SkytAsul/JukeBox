# JukeBox
JukeBox is a plugin that allows you to listen to music on your Minecraft server.

## How to download?
The official page for this project is on [SpigotMC](https://www.spigotmc.org/resources/jukebox-music-plugin.40580/).

## Documentation
You can find various tutorials in the [documentation page on SpigotMC](https://www.spigotmc.org/resources/jukebox-music-plugin.40580/field?field=documentation).

## Maven repository
Add this to your `repositories` section:
```xml
<repository>
	<id>codemc-repo</id>
	<url>https://repo.codemc.org/repository/maven-public/</url>
</repository>
```
And this to your `dependencies` section:
```xml
<dependency>
  <groupId>fr.skytasul</groupId>
  <artifactId>jukebox</artifactId>
  <version>VERSION</version>
  <scope>provided</scope>
</dependency>
```