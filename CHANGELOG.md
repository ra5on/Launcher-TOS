# Änderungen

## 1.2.2 (versionCode 122)

- **Langes Drücken nahm die App eine Reihe darüber auf (seit 1.2.0):** Die Trefferprüfung rechnete mit der
  ganzen Pager-Höhe, obwohl das Raster seit 1.2.0 unten Platz für Seitenpunkte und Dock freihält. Die Zellen
  waren dadurch rechnerisch zu hoch, und je tiefer man drückte, desto eher traf es die Reihe darüber. Jetzt wird
  die Rasterfläche genau so berechnet, wie sie gezeichnet wird (GridHit, mit Tests); das gilt für Aufnehmen,
  Ablegen und Ordnerbilden.

## 1.2.1 (versionCode 121)

- **Suche und Panels decken die Statusleiste wieder vollständig ab:** Der Homescreen nutzte für die Tastatur
  „adjustPan“; beim Öffnen der Tastatur konnte Android das ganze Fenster verschieben (heller, nicht
  weichgezeichneter Balken oben, Inhalt um die Statusleistenhöhe zu tief). Jetzt „adjustResize“ mit eigener
  Behandlung der Tastatur-Insets, und das Fenster passt sich ausdrücklich nicht an Systemleisten an
  (auch mit Fenster-Unschärfe).

## 1.2.0 (versionCode 120)

- **Ordner per Ziehen repariert:** Ziel ist die Mitte des gezogenen Icons, nach kurzem Verweilen (400 ms)
  erscheint die Ordner-Vorschau mit Glasrahmen; währenddessen weicht nichts mehr aus.
- **Benachrichtigungszugriff bei eingeschränkten Einstellungen:** eindeutige Erkennung über die Paketquelle,
  immer zuerst die Hilfe (Schritt 1/2) und die App-Info, danach der Zugriff – auch aus Einstellungen und Panels.
- **Schnellzugriff neu:** kompakt, Seiten untereinander und senkrecht blätterbar mit Punkten rechts,
  flache Regler, kompakte Medienzeile.
- **Benachrichtigungen neu:** nach App gestapelt, Wisch links entfernt, rechts/lange Optionen, Aktionen und
  Direktantwort, große Bilder, Medienkarte oben.
- **Gesten:** interaktives Handy-Bild mit allen Zonen und Auswahlkarten; optionale Bedienungshilfe für Wische
  direkt auf der Statusleiste (links+rechts oder ganze Breite, nur Homescreen oder überall), Bildschirm sperren.
- **Suche:** Kurzbefehle der Apps, Android-Einstellungen, optional Kontakte und Dateien, Websuche mit wählbarer
  Suchmaschine, Play Store, Adressen öffnen, Enter öffnet den ersten Treffer.
- **App-Übersicht:** quadratische Kacheln, feinere Kategorien (Finanzen, Browser, KI, Werkzeuge, Einkaufen,
  Smart Home, Auto & Verkehr, Gesundheit), „Kategorie ändern“ im App-Menü; Namen im Kategorie-Panel zweizeilig.
- **Glas:** der Standard 50 % ist jetzt deutlich sichtbar, auf hellen Fotos kräftiger; Vorlagen Klar/Ausgewogen/Kräftig.
- Auswahlseiten mit Karten und Vorschau, Einrichtung um „Von ganz oben wischen“ ergänzt.

## 1.1.0 (versionCode 110)

- **Einrichtung beim ersten Start:** Standard-Startbildschirm, Benachrichtigungen (mit Hilfe zu „Eingeschränkten
  Einstellungen“), Nicht stören, Helligkeit/Drehsperre, Updates, Gesten; erledigte Schritte werden übersprungen,
  Karte „Einrichtung fortsetzen“, „Einrichtung erneut starten“.
- **Updates über GitHub:** wöchentliche Prüfung (abschaltbar), Prüfsumme und Signatur werden vor der Installation
  geprüft, Versionshinweise in den Einstellungen. Dafür neu: Internet (nur GitHub) und „Unbekannte Apps installieren“.
- **Gesten:** Wisch nach unten funktioniert überall auf dem Raster, auch auf Icons; Panels folgen dem Finger und
  schließen per Gegenwisch; Randzone oben 25 % (einstellbar); eigene Benachrichtigungen auch ohne Zugriff mit
  Erklärkarte.
- **Seitenpunkte:** im Ruhezustand ausgeblendet, beim Blättern sichtbar, Tipp öffnet die Suche.
- **App-Übersicht als Kacheln** (4/6/9 Apps je Kachel, „+N“ öffnet die Kategorie mit Zoom), alte Ansichten bleiben wählbar.
- **Seiten links/rechts/aus** für App-Übersicht und Widget-Seite (Widget-Seite neu standardmäßig aus).
- **Einstellungen „Einfach / Alle“**, jede Einstellung mit erklärendem Satz und verständlicheren Namen.
- **Kontrast auf hellen Fotos**, stärker getöntes Glas als Standard.
- **Bewegung:** Ordner und Kategorien öffnen mit Zoom aus der Herkunft, Icons drücken sich ein, weicheres Wackeln,
  Baseline Profile für flüssigeren Start.

## 1.0.0 (versionCode 100)

- Umbenennung in „Launcher TOS“.
- Suche als Overlay: Tippfehler-tolerant, Umlaute/ß, Hervorhebung, Rechner, Einstellungen als Treffer,
  zuletzt gesucht/gestartet.
- App-Übersicht als letzte Seite: Kategorien, A–Z mit Buchstaben-Leiste, Raster; App-Menü
  (Zum Homescreen, Anpassen, App-Info, Badge, Ausblenden, Deinstallieren); versteckte Apps mit Geräte-Sperre.
- Widget-Seite links und Widgets im Raster (mehrere Zellen, Größe ändern, verschieben, entfernen).
- Badges über den optionalen Benachrichtigungszugriff; eigene Benachrichtigungsliste.
- Schnellzugriff mit Seiten und Elementen in S/M/L, Mediensteuerung, Bearbeiten-Modus.
- Gesten getrennt nach oberem Rand (geteilt/ungeteilt) und Bildschirmmitte, System-Panels als Aktion,
  Vorlagen „Launcher TOS“, „Android-Standard“, „Android + Suche“, „Eigen“.
- Unschärfe hinter Panels (ab Android 12), Glas getönt/durchsichtig, Hintergrund abdunkeln, Akzentfarbe,
  Kontrast, Seitenübergänge, Endlos blättern, Animationsgeschwindigkeit, App-Öffnen-Animation,
  einfacher Modus, Bewegung reduzieren, Statusleiste ausblenden.
- Beim Ziehen weichen Icons aus; Ablegen schiebt ein statt zu tauschen; volles Dock verdrängt ins Raster.
- Ordner scrollen, Vorschau 2×2/3×3, Spalten und Hintergrund einstellbar.
- Feste Seitenanzahl, Auto-Add-Ziel, eigene Farbe für ältere Icons, Themen-Icons, Name/Icon je App.
- Pausiertes Arbeitsprofil: Dialog zum Fortsetzen.
- Einstellungen mit Suche, „Auf Standard zurücksetzen“ je Seite, Berechtigungs-Übersicht,
  „Anleitung erneut zeigen“.
- Release-Build mit R8.

## 0.1.0 (Phase 1)

- Homescreen mit Seiten, Raster, Dock, Seitenindikator; Original-Icons mit Squircle-Maske.
- Bearbeitungsmodus mit Ziehen über Seiten und ins Dock, Ordner, „–“ und „+“.
- Neue Apps automatisch, deinstallierte verschwinden; Raster verkleinern ohne Verlust.
- Einstellungen mit Live-Vorschau, Export/Import als JSON, Layout zurücksetzen.
