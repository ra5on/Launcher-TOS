# Datenschutzerklärung – Launcher TOS

Stand: 24. November 2026 (Version 1.2.0)

**Verantwortlich:** ra5on (https://github.com/ra5on)

## Kurzfassung

Launcher TOS erhebt keine personenbezogenen Daten. Die einzige Verbindung ins Internet ist die
Update-Prüfung bei GitHub (abschaltbar). Keine Werbung, keine Analyse- oder Tracking-Werkzeuge, keine
Bibliotheken Dritter, die Daten senden.

## Update-Prüfung (einzige Internetverbindung)

Ist „Automatisch prüfen“ eingeschaltet (höchstens einmal pro Woche) oder tippt man auf „Jetzt suchen“,
ruft die App `https://api.github.com/repos/ra5on/Launcher-TOS/releases/latest` ab und lädt bei einem Update
die APK und die Datei `SHA256SUMS` von `github.com`. Dabei werden keine Daten der App gesendet – nur die
normale Anfrage, die GitHub wie bei jedem Abruf erhält (z. B. IP-Adresse und eine allgemeine
Programmkennung „LauncherTOS-Updater“); für deren Verarbeitung gilt die Datenschutzerklärung von GitHub.
Die Prüfung lässt sich unter „Einstellungen → Updates“ abschalten; dann baut die App keine Verbindung auf.

## Welche Daten die App auf dem Gerät verarbeitet

- **Homescreen-Layout und Einstellungen** (Seiten, Ordner, Dock, Widgets, eigene App-Namen,
  ausgeblendete Apps): nur lokal im App-Speicher. Keine Cloud-Sicherung; die Geräteübertragung von
  Android ist ausgeschlossen.
- **Suchverlauf und zuletzt gestartete Apps:** nur lokal, in den Einstellungen abschaltbar und löschbar.
- **Liste der installierten Apps:** wird zur Anzeige gelesen, nicht gespeichert außer als Teil des Layouts.
- **Benachrichtigungen und Medien-Informationen** (nur wenn der Nutzer den Benachrichtigungszugriff
  erteilt): werden ausschließlich im Arbeitsspeicher gehalten, um Badges, die Benachrichtigungsliste
  und die Mediensteuerung anzuzeigen. Sie werden nicht gespeichert und nicht weitergegeben. Der Zugriff
  lässt sich jederzeit in den Android-Einstellungen widerrufen.
- **Geräte-Sperre** (optional, für versteckte Apps): die Prüfung übernimmt Android; die App erhält
  keine biometrischen Daten.

## Bedienungshilfe „Systemweite Wischgesten“ (optional)

Nur wenn der Nutzer sie selbst in den Android-Bedienungshilfen einschaltet. Der Dienst legt schmale,
unsichtbare Streifen auf die Statusleiste und erkennt dort einen Wisch nach unten. Er liest keine
Bildschirminhalte (`canRetrieveWindowContent` ist aus), keine Eingaben und keine Tastenereignisse. Er erfährt
nur den Paketnamen der App im Vordergrund, um auf Wunsch nur auf dem Homescreen aktiv zu sein. Nichts davon
wird gespeichert oder übertragen.

## Suche in Kontakten und Dateien (optional)

Standardmäßig aus. Schaltet der Nutzer „Kontakte“ bzw. „Dateien“ in den Sucheinstellungen ein, fragt Android
nach der Berechtigung. Gesucht wird nur auf dem Gerät, während man tippt; Ergebnisse werden nicht
gespeichert. Ein Treffer in „Im Web suchen“ öffnet den Browser erst beim Antippen; dabei erhält die gewählte
Suchmaschine den Suchbegriff.

## Export

Beim Exportieren schreibt die App das Layout als JSON-Datei an einen Ort, den der Nutzer selbst
auswählt. Weitere Weitergabe findet nicht statt.

## Berechtigungen

Alle Berechtigungen sind optional und in der App unter „Einstellungen → Berechtigungen“ erklärt
(Benachrichtigungszugriff, „Nicht stören“, Systemeinstellungen ändern, Statusleiste aufklappen,
Netzwerkstatus lesen, Geräte-Sperre, Deinstallation anfragen, Bedienungshilfe, Kontakte, Mediendateien).
Internet nutzt die App nur für die Update-Prüfung bei GitHub (siehe oben).

## Kontakt

Fragen zum Datenschutz: https://github.com/ra5on/Launcher-TOS/issues
