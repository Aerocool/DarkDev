# Über

Dieses Projekt beinhaltet den Server und die GUI-Komponenten des Abschlusspraktikumsprojekt in Rechnernetze.
Das Programm startet ein Spiel für "Das verrückte Labyrinth" und wartet auf Spieler.
Nach jedem Zug wird überprüft, ob der Zug gültig ist, und im Fehlerfall wird erneut ein Zug angefragt.
Wenn ein Spieler gewinnt, endet die aktuelle Partie
Der Server kommuniziert über Sockets und benutzt XML um Objekte zu serialisieren.
Die XML-Syntax wird definiert von der Schemadatei `src/main/resources/xsd/mazeCom.xsd`

# Build & Run
Building:
```
mvn install
```
Anwendung starten:
```
mvn exec:java
```

# Download

Die jar-Dateien der aktuellen und vorherigen commits finden sie unter [Pipelines](https://git.noc.fh-aachen.de/mazenet/maze-server/pipelines)

# Konfiguration

Details zur Konfiguration findet man [hier](doc/config.md)

# SSL/TLS

Details zum Einsatz verschlüsselter Verbindungen findet man [hier](doc/ssl.md)

# Credits

* Application icon made by [Freepik](http://www.freepik.com) from [www.flaticon.com](http://www.flaticon.com) is licensed by [CC 3.0 BY](href="http://creativecommons.org/licenses/by/3.0/)
* A lot of the treasure-icons taken from [numix-Project](https://numixproject.org/)
* Gameidea orginally from [Ravensburger](https://www.ravensburger.de/produkte/spiele/familienspiele/das-verrueckte-labyrinth-26446/index.html)