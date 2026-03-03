# Java NIO Chat-Protokoll

Eine leistungsstarke, asynchrone Implementierung eines Chat-Protokolls basierend auf **Java NIO (Non-blocking I/O)** und einem **Distance-Vector-Routing-Algorithmus**. Dieses Projekt demonstriert Peer-to-Peer-Kommunikation, dynamische Routing-Updates und zuverlässige Nachrichtenverarbeitung.

## 🚀 Features

* **Asynchrones Networking**: Nutzt `Selector` und `SocketChannel` für hocheffiziente, nicht-blockierende Kommunikation.
* **Distance-Vector-Routing**: Implementiert eine eigene Routing-Logik (ähnlich dem RIP-Protokoll), um den besten Pfad zwischen den Knoten zu finden.
* **Automatische Pfaderkennung**: Knoten tauschen Routing-Tabellen aus, um die Netzwerktopologie dynamisch zu erlernen.
* **Integritätsprüfung**: Alle Nachrichten enthalten eine CRC32-Prüfsumme, um die Konsistenz der Daten während der Übertragung sicherzustellen.
* **TTL (Time-to-Live)**: Verhindert Endlosschleifen im Netzwerk, indem die maximale Anzahl an Sprüngen (Hops) begrenzt wird.

## 🛠 Projektstruktur

* `src/main/java/routing/`: Logik für die Routing-Tabelle und Pfadberechnung.
* `src/main/java/protocol/`: Definition des Nachrichtenformats, der Header und Paketstrukturen.
* `src/main/java/networking/`: Implementierung des NIO-Servers und -Clients sowie der Nachrichtenbehandlung.
* `lib/`: Externe Abhängigkeiten (z. B. GSON für die JSON-Serialisierung).
* `pom.xml`: Maven-Konfigurationsdatei für das Dependency-Management.

## 📋 Voraussetzungen

* **Java 17** oder höher
* **Maven** (optional, zur Verwaltung der Abhängigkeiten)
* **GSON Library** (im `/lib` Ordner enthalten oder via Maven)

## ⚙️ Konfiguration

Der Standard-Port für die Kommunikation ist auf `45054` festgelegt. Dies kann in der Klasse `SimpleNIOServer` angepasst werden.

## 🚦 Erste Schritte

### 1. Projekt bauen
Falls du Maven verwendest, führe folgenden Befehl aus:
```bash
mvn clean install