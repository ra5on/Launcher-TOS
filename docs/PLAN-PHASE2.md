# PLAN – Launcher TOS, Phase 2 und Fertigstellung

Grundlage: PLAN.md (Rechtsvorgaben, Technik, Qualität gelten unverändert), Design-Vorlagen in
`design/export/screens/` (03 Benachrichtigungen, 04–08 Schnellzugriff, 09 App-Übersicht, 10 Suche, 11/14/15
Einstellungen, 13 Widget-Seite), Einstellungs-Katalog `docs/EINSTELLUNGEN.md` (P1 + P2 vollständig umsetzen).

## 0. Umbenennung
- App-Name überall „Launcher TOS“: `app_name`, activity-alias der Einstellungen (Label „Launcher TOS“),
  README, THIRD_PARTY_LICENSES, Über-Seite, Hinweistexte. Kotlin-Objekt `Klar` → `Tos` (bzw. sinnvoller Name),
  keine Reste von „Klar“ im Repo außer historischen Commits.
- applicationId bleibt `de.umstieg.launcher`.

## 1. Suche (Funktion 10)
- Wisch nach unten in der Mitte des Homescreens (und Tipp auf die Lupe im Seitenindikator) → Suche als
  Overlay mit Blur-Hintergrund, Tastatur öffnet sofort (einstellbar).
- Unscharfe Suche über alle Apps: Präfix, Wortanfang, Teilstring, Tippfehler (Damerau-Levenshtein ≤ 1–2 je nach
  Länge), Umlaute/ß normalisiert; Ranking mit Unit-Tests. Trefferbuchstaben hervorheben (Vorlage 10).
- Optional (Einstellung, Standard an): Rechner (einfache Ausdrücke) und Einstellungsseiten von Launcher TOS als Treffer.
  Kontakte NICHT (keine Berechtigung).
- „Zuletzt gesucht/gestartet“ (lokal, abschaltbar, löschbar).

## 2. App-Übersicht (Funktion 11)
- Letzte Seite rechts (abschaltbar). Oben Suchfeld (öffnet Suche). Kategorien automatisch aus
  `ApplicationInfo.category` (Spiele, Audio, Video, Bilder, Soziales, Nachrichten, Karten, Produktivität,
  Barrierefreiheit, Sonstige) + „Zuletzt installiert“ (firstInstallTime). Darstellung wie Vorlage 09
  (breite Glas-Karten, Reihe Icons, „Alle“ öffnet die Kategorie).
- Umschaltbar auf A–Z-Liste mit Buchstaben-Leiste rechts (Vorlage 10, Ziehen über die Leiste springt).
- Lange drücken auf eine App: „Zum Homescreen“, „App-Info“, „Deinstallieren“, „Ausblenden“.
- Versteckte Apps (Einstellungen → Liste; optional mit Geräte-Sperre über BiometricPrompt/KeyguardManager).

## 3. Widget-Seite (Funktion 12)
- Seite ganz links (abschaltbar) mit Android-AppWidgets: `AppWidgetHost`, Auswahl-Liste der Provider,
  Binden über `ACTION_APPWIDGET_BIND`, Konfigurations-Activity starten falls vorhanden, Größe aus
  minWidth/minHeight, Größe ändern, entfernen, sortieren (Bearbeiten-Modus der Seite). Persistenz der widgetIds.
- Leer-Zustand mit „Widget hinzufügen“. Glas-Hintergrund für Widgets einstellbar.
- Widgets auch auf dem Homescreen-Raster (belegen mehrere Zellen) – Katalog P2.

## 4. Badges (Funktion 13)
- `NotificationListenerService` (optional). Beim ersten Aktivieren erklärt eine Seite klar, wofür der Zugriff dient
  (Zahlen an Icons, eigene Benachrichtigungsliste, Mediensteuerung), dass nichts das Gerät verlässt, und führt zu
  `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`. Widerruf jederzeit.
- Badge-Stil Zahl/Punkt, Farbe Rot/Akzent/aus Icon, pro App abschaltbar; Ordner zeigen die Summe.

## 5. Panels oben links/oben rechts (Funktion 14)
- Wisch von oben links → eigene Benachrichtigungsliste (Vorlage 03): Karten mit App-Icon, Titel, Text, Zeit,
  Gruppierung „Neu“/„Früher“, Tippen = contentIntent, Wischen = entfernen (`cancelNotification`), „Alle löschen“,
  Blur-Hintergrund. Ohne Berechtigung: System-Benachrichtigungsleiste öffnen (StatusBarManager über
  EXPAND_STATUS_BAR, per Reflection mit try/catch) – sauberer Fallback, nie Absturz.
- Wisch von oben rechts → Schnellzugriff (Vorlagen 04–08): Seiten Steuerung/Verbindungen/Medien als Tabs,
  Seiten hinzufügen/entfernen/sortieren, Elemente hinzufügen/entfernen/Größe S·M·L, Persistenz.
  Direkt möglich: Taschenlampe (CameraManager.setTorchMode), Lautstärke (AudioManager), Nicht stören
  (ACCESS_NOTIFICATION_POLICY, mit Erklärung), Helligkeit und Drehsperre (WRITE_SETTINGS, optional mit Erklärung,
  sonst Fallback auf System-Dialog), Mediensteuerung (MediaSessionManager.getActiveSessions mit dem
  NotificationListener-Component; Cover, Titel, Play/Pause/Weiter/Zurück, Ausgabe-Auswahl über das System),
  Rechner/Kamera/Wecker/QR (App bzw. Intent starten). Nicht direkt erlaubt (WLAN, Mobile Daten, Bluetooth ab API 33,
  Flugmodus, Hotspot, NFC, Standort): Status anzeigen und `Settings.Panel`/passende Einstellungsseite öffnen,
  mit kurzem Hinweis in der Oberfläche. Ohne Berechtigungen: System-Schnelleinstellungen öffnen.
- Breite der Wisch-Bereiche oben einstellbar (20–50 % je Seite).
- Alle Gesten frei belegbar (Katalog §8): oben links, oben rechts, Mitte nach unten, nach oben, Doppeltipp leer,
  Doppeltipp unten (Standard: Assistent über `Intent.ACTION_ASSIST`/`ACTION_VOICE_COMMAND`), Aktionen:
  Benachrichtigungen, Schnellzugriff, Suche, App-Übersicht, App öffnen, Assistent, nichts.
- Nachtrag Inhaber: Wisch vom oberen RAND und Wisch nach unten in der BILDSCHIRMMITTE sind getrennte Gesten:
  „Oberer Rand links/Mitte/rechts“ (geteilt) bzw. „Oberer Rand“ (ungeteilt), „Nach unten wischen (Bildschirmmitte)“,
  „Nach oben wischen“, „Doppeltipp leer“, „Doppeltipp unten“. Randzone geteilt/ungeteilt, Höhe 24–64 dp.
  Zusätzliche Aktionen: „System-Benachrichtigungen“ und „System-Schnelleinstellungen“ (StatusBarManager
  expandNotificationsPanel/expandSettingsPanel per Reflection mit EXPAND_STATUS_BAR, try/catch; kein
  AccessibilityService – sonst Hinweis). Vorlagen als Schnellwahl: „Launcher TOS“ (Standard), „Android-Standard“,
  „Android + Suche“, „Eigen“ (eine einzelne Änderung springt auf „Eigen“). Die Suche bleibt immer über die Lupe im
  Seitenindikator erreichbar (Hinweis auf der Gesten-Seite). Unit-Tests für Vorlage → Gesten und das Umspringen.

## 6. Blur (Funktion 15)
- Ordner, Suche, Panels, Widget-/App-Übersicht-Overlays: Homescreen-Inhalt dahinter mit `RenderEffect`/
  `Modifier.blur` ab API 31 weichzeichnen, zusätzlich Fenster-Blur (`window.setBackgroundBlurRadius` bzw.
  `FLAG_BLUR_BEHIND`) wenn verfügbar und vom Gerät erlaubt (`WindowManager.isCrossWindowBlurEnabled`);
  darunter halbtransparenter Fallback. Einstellungen: Glas-Effekt Klar↔Getönt, Blur-Stärke, Hintergrund abdunkeln.

## 7. Haptik (Funktion 16) – prüfen und vervollständigen
- Bearbeitungsmodus, Aufnehmen, Seitenwechsel beim Ziehen, Ablegen, Ordner bilden; Stärke leicht/mittel/stark.

## 8. Phase-1-Lücken schließen
- Beim Ziehen weichen die anderen Icons aus (Vorschau der Zielposition), Ablegen auf belegte Zelle: Mitte des
  Icons = Ordner, sonst einschieben (nicht tauschen); volles Dock: Einschieben verdrängt ins Raster.
- Ordner-Panel scrollt; Ordner-Vorschau 2×2/3×3, Spalten im Ordner einstellbar.
- Katalog-P1-Reste: Seitenanzahl fest/automatisch, Auto-Add-Ziel, eigene Legacy-Hintergrundfarbe, Suche in den
  Einstellungen, „Auf Standard zurücksetzen“ je Seite, „Anleitung erneut zeigen“ (kurze Einführung beim ersten Start:
  Gesten oben links/rechts, Mitte, Bearbeiten).
- Pausiertes Arbeitsprofil: Dialog zum Fortsetzen (`UserManager.requestQuietModeEnabled(false, …)`).
- Querformat und Tablets: Raster sinnvoll, nichts abgeschnitten.

## 9. Restlicher Katalog (P2, so weit technisch sinnvoll)
Seitenindikator-Varianten, Seitenübergänge, Endlos blättern, Statusleiste ausblenden, Schatten, Themen-Icons
(Monochrom ab API 33), Icon/Namen pro App ändern, Schriftstärke/Zeilen/Textschatten/Schriftfarbe, Dock-Hintergrund/
Rundung/Anzahl, Akzentfarbe (Vorgaben, Material You aus Hintergrund, eigene), Kontrast erhöhen, Animations-
geschwindigkeit, App-Öffnen-Animation, Einfacher Modus, Bewegung reduzieren, Berechtigungs-Übersicht mit Erklärung.
P3-Einträge (Icon-Pakete, Profile, automatische Sicherung, mehrere Dock-Seiten, Zwei-Finger-Zoom) nur wenn Zeit
bleibt; sonst in README unter „Geplant“ aufführen.

## 10. Abschluss
- `tgradle assembleDebug assembleRelease testDebugUnitTest lint` grün (Release unsigniert, R8 an mit passenden Keep-
  Regeln, prüfen dass der Release-Build startet ist meine Aufgabe danach).
- Unit-Tests für Suche/Ranking, Kategorien, Gesten-Zuordnung, Schnellzugriff-Layout, Badge-Zählung, Widget-Layout.
- Berechtigungen im Manifest minimal und jede in der Oberfläche erklärt; weiterhin KEIN INTERNET.
- versionName 1.0.0, versionCode 100. README (Funktionen, Gesten, Berechtigungen mit Begründung, Bauen/Installieren,
  Grenzen der Plattform), CHANGELOG.md, THIRD_PARTY_LICENSES aktuell, Datenschutzerklärung `docs/DATENSCHUTZ.md`
  (keine Datenerhebung, keine Übertragung) für den Play Store.
- Keine Sperrbegriffe im Repo (Prüfung wie in Phase 1).
- Nach jeder größeren Scheibe `git add -A && git commit` (deutsche Commit-Messages, Trailer wie im ersten Commit:
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` und `Claude-Session: https://claude.ai/code/session_017qpbYm68QTraCox1CgWHBe`).
  Kein Push.
