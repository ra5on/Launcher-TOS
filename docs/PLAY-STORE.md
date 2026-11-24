# Google Play – alles für den Eintrag

Start: **https://play.google.com/console** → „Alle Apps“ → **„App erstellen“**.
Für alle Einstellungen danach gibt es oben in der Play Console ein Suchfeld; die Begriffe in
**fett** unten kann man dort direkt eingeben.

## 1. App erstellen

| Feld | Eintrag |
|---|---|
| App-Name | Launcher TOS |
| Standardsprache | Deutsch – de-DE |
| App oder Spiel | App |
| Kostenlos oder kostenpflichtig | Kostenlos |
| Erklärungen | Programmrichtlinien und US-Exportgesetze bestätigen |

## 2. Store-Eintrag (**Haupteintrag im Play Store**)

**Kurzbeschreibung** (max. 80 Zeichen):

> Vertrauter, aufgeräumter Homescreen – einfach umsteigen, fast alles einstellbar.

**Vollständige Beschreibung:**

> Launcher TOS ist ein Homescreen für alle, die von einem anderen Smartphone kommen und sich auf
> Android schnell zurechtfinden möchten: vertraut, übersichtlich und trotzdem bis ins Detail
> anpassbar.
>
> ● Übersichtlicher Homescreen mit Seiten, Dock und Ordnern
> ● Deine Original-App-Icons – Rundung stufenlos von eckig bis rund
> ● Lange drücken zum Bearbeiten: verschieben, Ordner per Ziehen, entfernen
> ● Neue Apps landen automatisch auf der nächsten freien Stelle
> ● Suche, die Tippfehler verzeiht, mit Rechner und Einstellungs-Treffern
> ● App-Übersicht mit automatischen Kategorien und A–Z-Liste
> ● Widget-Seite und Widgets direkt auf dem Homescreen
> ● Eigene Benachrichtigungsliste und Schnellzugriff mit mehreren Seiten (Taschenlampe,
>   Lautstärke, Helligkeit, Medien, Nicht stören …)
> ● Gesten frei belegen – oder ganz einfach den Android-Standard behalten
> ● Glas-Effekt mit Unschärfe, Hell/Dunkel, Akzentfarbe, Schrift, Raster, Haptik
> ● Zahlen-Badges an den Icons (optional)
> ● Layout exportieren und importieren
>
> Datenschutz: Launcher TOS hat keine Internet-Berechtigung, zeigt keine Werbung und sammelt keine
> Daten. Alle Einstellungen bleiben auf deinem Gerät.
>
> Open Source (GPLv3): https://github.com/ra5on/Launcher-TOS

**Grafiken** (Dateien im Repo unter `design/export/store/`):

| Feld | Datei |
|---|---|
| App-Symbol 512 × 512 | `icon-512.png` |
| Feature-Grafik 1024 × 500 | `feature-graphic.png` |
| Smartphone-Screenshots (2–8) | `01-store-homescreen.png` … `08-store-einstellungen.png` |

**Kategorie** (**Store-Einstellungen**): App → Personalisierung. Tags: Launcher, Homescreen.
**Kontakt:** E-Mail-Adresse des Entwicklers; Website: https://github.com/ra5on/Launcher-TOS

## 3. App-Inhalte (**App-Inhalte**)

| Abschnitt | Antwort |
|---|---|
| **Datenschutzerklärung** | https://github.com/ra5on/Launcher-TOS/blob/main/docs/DATENSCHUTZ.md |
| **App-Zugriff** | Alle Funktionen ohne besonderen Zugriff verfügbar (kein Login) |
| **Werbung** | Nein, die App enthält keine Werbung |
| **Einstufung des Inhalts** | Fragebogen: Kategorie „Alle anderen App-Typen“, überall „Nein“ → USK 0 / PEGI 3 |
| **Zielgruppe** | 18 und älter (vermeidet die Zusatzpflichten für Kinder-Apps) |
| **Datensicherheit** | „Erhebt oder teilt Ihre App erforderliche Nutzerdatentypen?“ → **Nein**. Keine Daten erhoben, keine geteilt |
| **Nachrichten-Apps** | Nein |
| **Behörden-App / Finanzfunktionen / Gesundheit** | Nein / keine / keine |

## 4. Release hochladen

- **Signatur:** Play App Signing (Standard) annehmen. Hochgeladen wird `app-release.aab`, signiert
  mit dem Upload-Schlüssel `launchertos-upload`
  (SHA-256 `21:42:79:1D:B9:EE:62:83:BB:E1:B7:C0:5B:52:05:AE:96:C4:33:E3:63:F4:FE:78:FE:71:A5:48:6C:4A:06:3C`).
- **Wichtig für neue private Entwicklerkonten:** Vor der Produktion verlangt Google einen
  **geschlossenen Test** mit mindestens 12 Testern über 14 Tage. Dafür **„Geschlossener Test“** →
  Track anlegen → Tester-Liste (E-Mail-Adressen) → **„Neuen Release erstellen“** → AAB hochladen.
  Danach unter **„Produktion“** den Zugriff beantragen.
- **Versionshinweise** (de-DE): „Erste Version von Launcher TOS.“

## 5. Hinweis zu GitHub und Play

Die APK auf GitHub ist mit dem Upload-Schlüssel signiert; die Play-Fassung signiert Google mit
seinem eigenen Schlüssel. Beide lassen sich deshalb **nicht übereinander aktualisieren** – wer von
der GitHub-APK zu Play wechselt, muss vorher deinstallieren (Layout vorher exportieren).
