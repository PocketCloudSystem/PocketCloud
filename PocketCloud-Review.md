# PocketCloud (java-Branch) – Gesamtreview

Zusammenfassung aus drei Analyse-Runden am aktuellen HEAD (Stand der Tests: Commit-Bereich um `5cf1f05` / `4a5675f`).  
Etwa 30.000 Zeilen Java in 495 Dateien. Getestet mit selbst gebautem JAR unter JDK 25, Live-Starts, gezielten Harness-Tests und Code-Inspektion.

---

## Gesamtbild – die gravierendsten Punkte (nach Schwere)

1. **`reload` crasht die Cloud und verwaist alle Server** — immer (auch ohne laufende Server).
2. **Netzwerk-Deserialisierung vor der Auth angreifbar** (Zip-Bombe → OOM, abgeschnittener Stream → Endlosschleife, tiefe Rekursion → StackOverflow), alles mit Paketen unter dem 1-MB-Limit.
3. **Save-Race löscht reproduzierbar Weltdaten** (9/20), plus zweiter Save bricht ohne `REPLACE_EXISTING` ab.
4. **MySQL `editTemplate`/`editServerGroup` korrumpieren jede Zeile** (Parameter/Spalten vertauscht, Anzahl stimmt nicht).
5. **Ein Tippfehler im Setup beendet die Cloud** (unbehandelte Exception im Tick).
6. **Path Traversal + Punkt im Template-Namen** macht die Cloud dauerhaft unstartbar.
7. **SIGTERM hängt für immer** (Deadlock im Shutdown-Hook).
8. **Pipe-Blocking** friert Server mit Konsolen-Logging ein.
9. **PNX-Server unter 1024 MB starten nie** (`SoftMaxHeapSize` hart verdrahtet).
10. **`transfer()` meldet immer Misserfolg.**

Die meisten davon sind mit wenig Code behebbar (siehe Abschnitt „Empfohlene Fixes“).

---

## 1. Kritische Bugs – live oder mit Harness bewiesen (fixed)

### 1.1 `reload` ist ein Crash-Befehl (immer)

`Loader.reload()` ruft `unloadAll()` + `loadAll()`, aber **nie** `preloadAll()`.  
Die Software wird ausschließlich in `preload()` geladen (`ServerSoftwareManager.load()` ist leer). Nach dem Unload ist die Software-Liste leer → `ServerPropertiesGenerator.load()` wirft `RuntimeException: Required software not found`.

Das löst `shutdown()` aus, das **seinerseits** scheitert (`IllegalStateException: Template should not be null`, weil die Templates schon entladen sind). Die Server werden nie gestoppt → **verwaiste Prozesse**.

Live getestet auf leerer und laufender Cloud (`procs after reload: 4` – beide PNX-Server verwaisen). Der Befehl `reload`/`rl` ist aktuell unbenutzbar.

### 1.2 Netzwerk-DoS vor der Authentifizierung (fixed)

In `PacketSerializer` (vor Auth-Prüfung), alle drei mit Paket **unter** dem 1-MB-Limit:

| Angriff                        | Ergebnis                                      |
|--------------------------------|-----------------------------------------------|
| Zip-Bombe (523 KB → 120 MB)    | `OutOfMemoryError`                            |
| Abgeschnittener Deflate-Stream | Endlosschleife, Netty-Worker permanent blockiert |
| Tief verschachteltes Array     | `StackOverflowError`                          |

Alle drei sind `Error` (nicht `Exception`) und rutschen durch beide `catch (Exception)`-Blöcke im Codec und in `CloudPacketDecoder.decode`.  
Das Binding auf `127.0.0.1` mildert ab, lokal reicht jeder Prozess.

### 1.3 Save-Race + unvollständiger zweiter Save (fixed)

In allen Exit-Pfaden (`CloudServersHandler`):

- `remove()` startet `save()` **asynchron**
- direkt danach löscht `deleteTmpDir()` den Ordner **synchron**

Mit echten PNX-Serverdaten: In **9 von 20 Läufen** ging die Welt verloren (`NoSuchFileException`).  
Sequentiell (`save().then(deleteTmpDir)`): 20/20 heil.

Zusätzlich: `Files.copy` ohne `REPLACE_EXISTING` → ab dem zweiten Save bricht es bei `banned-ips.json` mit `FileAlreadyExistsException` ab und sichert keine weiteren Dateien mehr (ohne geloggten Fehler).

### 1.4 MySQL `editTemplate` / `editServerGroup` komplett kaputt (fixed)

`buildUpdate` sortiert die SET-Spalten alphabetisch (`.sorted()`).  
`buildUpdateParams` liefert die Werte in Feld-Reihenfolge (`data.values()`) und hängt den Primary Key (`name`) ein **zweites Mal** an.

Ergebnis mit echter `write()`-Ausgabe eines Templates:

- 5 Parameter für 4 Platzhalter
- Zuordnung komplett vertauscht (`serverSoftware` bekommt `"Lobby"`, `settings` das JSON, `WHERE name` bekommt den Software-Namen)

Jedes `template edit` (und analog `editServerGroup`) korrumpiert die Zeile oder trifft die falsche/keine Zeile.

### 1.5 Unbehandelte Exception im Tick beendet die Cloud (fixed)

Eingabe `99999999999` bei „Max players“ passt auf `\d+`, sprengt aber `Integer.parseInt`.  
Die Exception läuft durch `Ticker.tick()` (kein try/catch pro Tickable) bis zum globalen Uncaught-Handler in `PocketCloud.java:169` → `shutdown()`.  
**Jede** Exception in *irgendeinem* Tickable hat denselben Effekt.

### 1.6 Path Traversal + Punkt im Template-Namen (fixed)

- `template create` mit `../evil` legt den Ordner außerhalb von `templates/` an.
- Name mit Punkt (z. B. `BedWars.4x4` oder `a.b`) → `Config.set()` behandelt Punkte als Pfad → kaputte `templates.json`.  
  Beim nächsten Start: NPE in `TemplateManager.tick()` (`settings()` ist null) → Cloud fährt sofort wieder herunter. Ohne manuelles JSON-Editieren startet sie nie wieder.

### 1.7 SIGTERM hängt für immer (fixed)

`CloudShutdownHook` ruft `shutdown()` auf, das mit `System.exit(0)` endet.  
Laut JDK-Spezifikation blockiert `System.exit` innerhalb eines laufenden Hooks → klassischer Deadlock auf dem Shutdown-Lock.  
`jstack` bestätigt: JVM reagiert 40+ s nicht.

Nebenbei: Beim Herunterfahren läuft `loader().unloadAll()` parallel zum eintreffenden `DisconnectPacket` → `InvocationTargetException` im Netty-Thread.

### 1.8 Pipe-Blocking (`proc`-Startmodus) (fixed)

`ProcessServerStartMethod` nutzt `redirectErrorStream(true)`, aber niemand liest stdout.  
Nach ca. 64 KB Konsolenausgabe blockiert der Server beim Loggen und friert ein.

Live-Beweis mit kontrolliertem Kind-Prozess (viel stdout, exakt das Muster der Cloud): Kind hängt nach 15 s auf vollem stdout-Puffer.  
Mit `redirectOutput(appendTo(file))`: 0,75 s, 44 MB geschrieben, sauber beendet.

PNX schreibt sein Log selbst in eine Datei (deshalb tritt der Fall dort langsamer auf), aber jede Software, die auf die Konsole loggt, ist betroffen.  
Zusätzlich: Sofortiger Crash wird nur über Timeout erkannt, weil `pidLookupDone` bei `proc` nie true wird.

### 1.9 PNX-Server unter 1024 MB starten nie (fixed, removed the JVM Flag)

In den Default-Flags ist `-XX:SoftMaxHeapSize=1024M` fest verdrahtet.  
Bei `-Xmx512M` bricht die JVM mit „SoftMaxHeapSize must be less than or equal to the maximum heap size“ ab.  
Das Setup erlaubt ab 256 MB. Zusätzlich ist `-XX:+ZGenerational` seit JDK 24 obsolet (Warnung).

### 1.10 `transfer()` gibt immer `false` zurück (fixed)

In `CloudPlayerExecutor.transfer()` wird das Transfer-Paket gesendet, aber Zeile 109 ist ein hartes `return false`.  
Aufrufer, die den Rückgabewert prüfen, sehen jeden Transfer als fehlgeschlagen und versuchen es ggf. erneut.

### 1.11 Erststart-Backup-Bug (fixed)

Selbst bei komplett leerem Verzeichnis meldet die Cloud „incompatible folder structure“ und legt ein Backup aus leeren Ordnern an.  
Ursache: `createDirectories()` (Zeile 104) läuft **vor** dem First-Run-Check und erzeugt genau die Ordner, die der Check dann als „alte Struktur“ sichert.

### 1.12 Fehlgeschlagener Software-Download beim Erststart beendet die Cloud

Default-Softwares laden ohne try/catch (`ServerSoftwareManager.java:140`).  
Beim zweiten Start liegt die JSON schon auf der Platte → Software wird nur deaktiviert (inkonsistent).  
Die eigentliche Ursache wird in `SoftwareService` komplett verschluckt (`catch (Exception e) { return false; }`).

---

## 2. Weitere wichtige Beobachtungen

### 2.1 PID-Handling (idk)

Die Bridge schickt im Handshake `ProcessHandle.current().pid()` (echte Server-JVM-PID). Die Cloud speichert diese in `data.processId()`.  
Ein verwaister Prozess durch falsche PID ist nur möglich, wenn der Server **vor** dem Handshake stirbt (dann steht der Launcher-PID oder nichts drin). Schmaleres Fenster als zunächst angenommen.

Bei `proc` gibt `lookupPid` immer `Optional.empty()` zurück. Die Cloud speichert `process.pid()` des direkten Childs (oft ein Zwischen-Launcher), nicht zwingend die eigentliche Server-JVM.

### 2.2 Player-Handling (fixed)

`players`, `playersByXuid` und `playersByUuid` sind drei getrennte `ConcurrentHashMap`s ohne gemeinsame Synchronisation.  
Bei schnellem Reconnect über Proxy können sie auseinanderlaufen (verwaiste XUID-/UUID-Einträge).  
Spielername als Primärschlüssel ist im Bedrock-Offline-Kontext kollisionsanfällig (gleicher Name überschreibt).

### 2.3 Sicherheitsbeobachtung – Auth-Token ()

Server-Identität hängt allein am Auth-Token + selbst gewähltem `serverName`.  
Das Token liegt im Klartext in jeder `.env` und (bei Default `encryption: false`) auch im Klartext im Netzwerk.  
Wer es kennt, kann sich als beliebiger Server ausgeben. Für reines `127.0.0.1` vertretbar; sobald Server auf anderen Maschinen laufen, sollte Verschlüsselung Pflicht und das Token pro Server unterschiedlich sein.

Zusätzlich: Token-Erzeugung über `ThreadLocalRandom` statt `SecureRandom`, enthält problematische Zeichen (`$ & § = /`). Vergleich nutzt `.equals` statt `StringUtils.secureEquals`. Bei ungültigen Paketen wird der Channel nicht geschlossen → Log-Flooding mit Stacktraces.

### 2.4 HTTP-API

Auth ist solide (`secureEquals`, kein Bypass über Groß-/Kleinschreibung, `..`, `%2f`, Doppel-Slash).  
Kleinere Punkte:

- Trailing-Slash liefert 404 statt zur selben Route zu matchen.
- `stats`-Endpoint gibt Version/Speicher/CPU **ohne Auth** preis (steht so in der Doku).

### 2.5 JSON-Provider (...)

Solide (atomare Save-Logik über `Config`).  
Der Punkt-im-Namen-Bug wurzelt hier: `Config.set(name, …)` behandelt Punkte als Pfad.

### 2.6 Whitelist / Maintenance (fixed)

Verwirrende Benennung (`maintenanceList.json` → `WhitelistCache`), aber konsistent und kein Bug.

### 2.7 Plugin-ClassLoader (...)

Sauber (parent-first für `java.`/`javax.`/`de.pocketcloud.`, sonst child-first), schließt bei Fehler den Loader.

### 2.8 Bridge-Plugin (...)

Braucht Java 25 auf dem PATH. Ohne das: `UnsupportedClassVersionError` (class file 69.0 vs 65.0) → Server meldet sich nie und läuft in „Failed to start“.  
Sollte in der Doku stehen oder die Cloud sollte die Java-Binary für Server explizit setzen.

### 2.9 Autoscaling / `serverCounts` (fixed)

- `remove()` benutzt `serverCounts.getOrDefault(template, 1) - 1` → Default `1` ist konzeptionell falsch.
- `remove()` dekrementiert immer, auch wenn `start()` beim Prepare-Fehler schon dekrementiert hat → doppelte Buchung möglich.
- Threshold-Scale-up prüft nur `getLatest` + `checkCapacity`, ohne Cooldown.

### 2.10 Downloads (`NetUtils.download`) (fixed)

- Prüft HTTP-Status nicht → 404-Seite kann als `.jar` gespeichert werden.
- Schreibt nicht atomar.
- Update-Erkennung nur über `Content-Length`. Offline: Größencheck -1 → erzwungener Download, der scheitert → Software wird deaktiviert, obwohl JAR lokal vorhanden ist.

---

## 3. Kleinere Punkte / Code-Qualität

- Java-Versions-Check in `PocketCloud` ist toter Code (unter Java < 25 scheitert schon das Laden der Klasse). Beendet mit `exit(0)` statt Fehlercode.
- Log-Archivnamen enthalten `:` → unter Windows ungültig. Dateien heißen `server.log.log`.
- `saveAndDeleteLogFiles()` löscht gar nichts (Name passt nicht zur Funktion).
- „Loading/Unloading server groups...“ wird doppelt geloggt.
- Status-Befehl: „accross“, Usage-Hilfe erscheint als WARN.
- Banner und README sprechen von „PocketMine-MP“, verwaltet werden aber PowerNukkitX und WaterdogPE.
- Es fehlt `Enable-Native-Access` im Manifest → JLine-Warnung beim Start.
- Lombok und `netty-pkitesting` landen im Runtime-JAR.
- `discord-webhook` liegt auf GitHub Packages (Auth nötig) → externe Contributor können das Projekt nicht bauen.
- Es gibt aktuell **keine Tests**.

---

## 4. Empfohlene Fixes (kurz)

| Problem | Fix |
|---------|-----|
| `reload` | `preloadAll()` in `Loader.reload()` aufrufen |
| Netzwerk-DoS | Inflater-Output-Limit + `needsInput()`-Check; Parser-Tiefen-/Größenlimit; `Error` mitfangen; Channel bei Fehlern schließen |
| Save-Race | `save().then(deleteTmpDir)` sequentiell + `REPLACE_EXISTING` + Fehler loggen |
| MySQL edit* | `buildUpdateParams` an die sortierte Spaltenreihenfolge koppeln und PK nicht doppelt anhängen |
| Tick-Exception | try/catch pro Tickable; globaler Handler nur loggen, nicht herunterfahren |
| Namen | Zentrale Validierung z. B. `^[A-Za-z0-9_-]{1,32}$` im Builder |
| Template-JSON | Ohne Punkt-Pfad-Semantik arbeiten, atomar schreiben (tmp + move); kaputtes Template überspringen statt Cloud stoppen |
| SIGTERM | Shutdown-Hook ohne `System.exit` |
| Pipe-Blocking | `redirectOutput(appendTo(logFile))`; Crashes über `process.onExit()` |
| SoftMaxHeapSize | Aus `MAX_MEMORY` ableiten oder streichen; `ZGenerational` entfernen |
| `transfer()` | `return true` bei erfolgreichem Transfer |
| Token | `SecureRandom` + URL-sicheres Alphabet; `secureEquals`; Channel bei Auth-Fehler schließen |
| Downloads | Nur Status 2xx akzeptieren; `.part`-Datei + atomarer Move; ETag/SHA-256 statt Content-Length; offline vorhandene JAR behalten |
| Java-Binary | Explizit aus `ProcessHandle.current().info().command()` oder pro Software konfigurierbar setzen |

---

## 5. Positiv bestätigt

- Ein Server läuft komplett durch (mit 1024 MB verbindet sich `Lobby-1` nach ~12 s, Status ONLINE, Memory-Reporting, Channel).
- `minServerCount=2` skaliert korrekt hoch.
- HTTP-Auth ist robust.
- Plugin-ClassLoader ist korrekt implementiert.
- JSON-Provider ist grundsätzlich solide.
- Bridge nutzt denselben Codec wie die Cloud (Fuzzing-Findings gelten symmetrisch).

---

## 6. Nicht getestet

- Autoscaling über den Player-Threshold (brauchte echte Spieler)
- Waterdog/Proxy-Betrieb
- Plugin-System mit einem echten Plugin
- MySQL-Bugs gegen eine laufende Datenbank (nur isoliert bewiesen)
- Vollständiges Player-Handling unter Last

---

*Review basierend auf drei Runden Code-Analyse + Live-Tests unter JDK 25.*
