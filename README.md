# Launcher TOS

Ein Homescreen für Menschen, die von einem anderen Smartphone-System zu Android wechseln:
vertraut, aufgeräumt und einfach zu bedienen – mit eigener Optik. Phase 1.

## Funktionen

- **Standard-Launcher**: als Startbildschirm registrierbar (HOME). Die Home-Taste führt immer zurück
  auf Seite 1; „Zurück“ schließt nur Ordner und den Bearbeitungsmodus. Beim ersten Start erscheint
  eine Hinweiskarte, falls Launcher TOS noch nicht der Standard ist.
- **Mehrere Seiten** mit Raster (3–7 Spalten, 4–9 Reihen) und Seitenindikator
  (Punkte, Balken, Zahl oder aus).
- **Dock** unten als Glas-Fläche mit 3–6 Apps (Standard 4), abschaltbar.
- **Original-Icons** aller Apps (auch aus dem Arbeitsprofil), einheitlich zugeschnitten auf eine
  Superellipse; Rundung von eckig bis Kreis frei einstellbar (Standard 62 %). Ältere Icons ohne
  eigene Form liegen verkleinert auf Weiß oder auf der Hauptfarbe des Icons.
- **Icongröße** (70–130 %), **Beschriftungen** (Homescreen, Dock, Ordner einzeln) und
  **Schriftgröße** (80–140 %).
- **Bearbeitungsmodus** durch langes Drücken: Icons wackeln, lassen sich zwischen Seiten und ins
  bzw. aus dem Dock ziehen (am Rand verweilen wechselt die Seite). „–“ nimmt eine App vom
  Homescreen (sie wird nicht deinstalliert), „+“ zeigt alle nicht platzierten Apps, das Zahnrad
  öffnet die Einstellungen, „Fertig“ beendet den Modus. Haptik und Wackeln sind abschaltbar.
- **Ordner**: App auf App ziehen. Der Name ist antippbar und änderbar, Apps lassen sich im Ordner
  umsortieren, herausziehen oder entfernen. Bleibt nur eine App übrig, löst sich der Ordner auf.
- **Neue Apps** erscheinen automatisch auf der nächsten freien Stelle (wenn nötig auf einer neuen
  Seite). Per „–“ entfernte Apps kommen nicht wieder. Deinstallierte Apps verschwinden aus Raster,
  Ordnern und Dock.
- **Speichern**: Das Layout wird nur lokal gespeichert. Wird das Raster verkleinert, rücken
  überzählige Elemente auf freie Stellen oder neue Seiten – nichts geht verloren.
- **Einstellungen** mit Live-Vorschau, erreichbar über den Bearbeitungsmodus oder den eigenen
  Eintrag „Launcher TOS“ in der App-Liste: Raster, Icons, Beschriftung, Dock und Ordner,
  Hell/Dunkel/System, Haptik, Wackeln, Layout sperren, große Touch-Flächen, Layout exportieren,
  importieren (JSON) und zurücksetzen, Standard-Launcher festlegen, Über und Lizenzen.

## Bauen

Voraussetzungen: JDK 17 oder neuer, Android SDK mit Plattform 37 (`local.properties` mit `sdk.dir`).

```sh
./gradlew assembleDebug
```

Tests und Prüfungen:

```sh
./gradlew testDebugUnitTest lint
```

## Installieren

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Als Standard-Launcher festlegen

- Home-Taste drücken und „Launcher TOS“ wählen, oder
- in Launcher TOS: lange drücken → Zahnrad → „Standard-Launcher“, oder
- in den Android-Einstellungen unter „Apps“ → „Standard-Apps“ → „Startbildschirm-App“.

## Berechtigungen

Keine. Launcher TOS hat keinen Internetzugriff, zeigt keine Werbung und sammelt keine Daten. Sichtbar sind
nur Apps mit einem Eintrag in der App-Liste (Manifest-Abfrage `MAIN`/`LAUNCHER`); zusätzlich fragt
Launcher TOS beim ersten Start die Standard-Apps für Telefon, Nachrichten, Browser und Kamera ab, um das
Dock vorzubelegen. Haptisches Feedback läuft ohne Vibrationsberechtigung.

## Lizenz

GNU General Public License Version 3, siehe [LICENSE](LICENSE).
Schrift Inter (SIL OFL 1.1), Symbole Lucide (ISC) sowie AndroidX/Kotlin-Bibliotheken
(Apache 2.0): siehe [THIRD_PARTY_LICENSES](THIRD_PARTY_LICENSES).
