# PLAN – Launcher TOS, Phase 1

App-Name: „Launcher TOS“ (`app_name`).
Package/applicationId: `de.umstieg.launcher` (vorläufig).
Lizenz: GPLv3. Oberfläche Deutsch, alle Texte in `res/values/strings.xml`.

## Ziel
Homescreen-App (Kotlin + Jetpack Compose) für Menschen, die von einem anderen Smartphone-System
kommen: vertraut und einfach, eigenständige Optik (kein Klon). Design-Vorlagen als PNG:
`design/export/screens/` (01 Homescreen, 02 Ordner, 12 Bearbeitungsmodus,
14/15/11 Einstellungen). Design-Werte stehen in `design/assets/kit.css`
(Akzent #2EC4A6, Glas-Flächen, Radien) und `kit.js` (Squircle-Formel).
Vollständiger Einstellungs-Katalog: `docs/EINSTELLUNGEN.md` (Phase 1 = Einträge „P1“).

## Rechtliche Vorgaben (zwingend)
- Keine fremden Inhalte: keine fremden Icons, Schriften, Symbole, Wallpaper, Sounds, Emoji.
- Marken- und Produktnamen anderer Smartphone-Plattformen kommen weder im Code, in Strings, Kommentaren,
  Dateinamen, im App-Namen noch im Icon vor (Sperrliste liegt lokal außerhalb des Repos und wird vor jedem Commit geprüft).
- Schrift Inter (SIL OFL 1.1) als TTF in `res/font` (aus rsms/inter v4.1 Release-Zip,
  https://github.com/rsms/inter/releases/download/v4.1/Inter-4.1.zip, statische TTFs Regular/Medium/SemiBold/Bold;
  Dateinamen klein mit Unterstrich, mit `file` prüfen). Lizenz beilegen.
- Symbole: Lucide (ISC) als VectorDrawable/ImageVector; SVGs liegen in
  `design/assets/icons/`, weitere von
  https://cdn.jsdelivr.net/npm/lucide-static@0.460.0/icons/<name>.svg. Lizenz beilegen.
- Keine Werbung, kein Tracking, KEINE INTERNET-Berechtigung (im gemergten Manifest prüfen).
- Eigenes App-Icon (Adaptive Icon, eigenes Motiv, z. B. 2×2-Raster aus Rundquadraten in Mint auf Dunkelblau).

## Technik
- Gradle-Gerüst übernehmen aus `/home/zeus/TitanOS/TitanOS-Android` (nur lesen, dort nichts ändern):
  `gradlew`, `gradlew.bat`, `gradle/wrapper/*` (Gradle 9.9.1 im Cache), `local.properties`
  (sdk.dir=/home/zeus/.local/share/titanos-dev/android), `gradle.properties`.
  Versionen: AGP 9.5.1, Kotlin 2.4.21, Compose-BOM 2026.09.00, activity-compose 1.13.0, core-ktx 1.19.1,
  lifecycle 2.11.1, coroutines 1.11.0, kotlinx-serialization 1.13.0, DataStore 1.3.0 (androidx.datastore:datastore),
  junit 4.13.2. compileSdk = targetSdk = 37, minSdk = 26. Java 17.
  Plugins nur `com.android.application`, `org.jetbrains.kotlin.plugin.compose`, `kotlin.plugin.serialization`
  (AGP 9 hat Kotlin eingebaut, kein kotlin-android). Kein roborazzi, keine lint-rules, kein TitanOS-Namespace.
- Ein Modul `:app`. MVVM, Coroutines/Flow. Kein Hilt, kein Room, kein KSP.
- Speichern: DataStore mit eigenem Serializer auf kotlinx-serialization-JSON (Layout + Einstellungen).
  Export/Import nutzt dasselbe JSON (Storage Access Framework, ACTION_CREATE_DOCUMENT/OPEN_DOCUMENT).

## Funktionen Phase 1
1. Als Standard-Launcher registrierbar: HOME/DEFAULT-Intent-Filter, `singleTask`, `onNewIntent` → Seite 0,
   Zurück schließt nur Ordner/Bearbeitungsmodus. „Als Standard festlegen“: ab API 29 `RoleManager.ROLE_HOME`,
   sonst `Settings.ACTION_HOME_SETTINGS`. Hinweiskarte beim ersten Start, falls nicht Standard.
2. Homescreen mit mehreren Seiten (HorizontalPager), Raster mit einstellbaren Spalten (3–7) und Reihen (4–9),
   Seitenindikator (Punkte, aktive Seite breiter).
3. Festes Dock unten mit bis zu 4 Apps (Glas-Fläche), an/aus.
4. Original-Icons über `LauncherApps` (`getActivityList`, `startMainActivity`, `LauncherApps.Callback`).
   Adaptive Icons: Hinter- und Vordergrund mit `setBounds(-S/4,-S/4,S*5/4,S*5/4)` in eine Bitmap S×S zeichnen
   (sichtbarer Bereich 72 von 108 dp). Volle Bitmap cachen (LruCache), Maske NICHT einbacken, sondern beim
   Zeichnen mit einer Compose-`Shape` aus der Superellipse zuschneiden: |x|^n+|y|^n=1, n = 12 − r·10, r = 0..1
   (Regler 0–100 %, Standard 62 %). Live-Vorschau in den Einstellungen. Legacy-Icons: verkleinert (ca. 70 %)
   mittig auf Hintergrundfarbe (weiß oder dominante Farbe aus dem Icon, einstellbar).
   Persistenz-Schlüssel je App: package + activity + userSerial (Arbeitsprofil).
5. Icongröße (70–130 %), Beschriftung an/aus, Schriftgröße (80–140 %).
6. Lange drücken → Bearbeitungsmodus: Icons wackeln (Animation, abschaltbar), Drag & Drop mit
   `detectDragGesturesAfterLongPress`, gezogenes Icon in einem Overlay über dem Pager, Ablage per
   Zellen-Trefferprüfung, am linken/rechten Rand verweilen → Seitenwechsel (Timer + animateScrollToPage),
   auch ins und aus dem Dock. „–“-Badge entfernt vom Homescreen (nicht deinstallieren). „Fertig“ beendet.
   Im Bearbeitungsmodus außerdem: „+“ (Liste aller nicht platzierten Apps zum Hinzufügen) und Zahnrad
   (Einstellungen). Haptik beim Start des Modus und beim Ablegen (abschaltbar).
7. App auf App ziehen → Ordner; Name editierbar (Standard „Ordner“). Ordner geöffnet als Glas-Panel
   (Vorlage 02), darin ziehen/entfernen. Bleibt nur eine App, löst sich der Ordner auf.
8. Neu installierte Apps erscheinen automatisch auf der nächsten freien Stelle (neue Seite, wenn voll).
   Nur wirklich neue Pakete: Menge „bekannter Apps“ speichern, damit per „–“ entfernte Apps nicht
   zurückkommen. Deinstallierte Apps verschwinden aus Raster, Ordnern und Dock.
9. Layout wird lokal gespeichert. Raster verkleinert → überzählige Elemente auf die nächste freie Stelle
   bzw. neue Seiten (nichts geht verloren). Leere Seiten am Ende werden entfernt (außer Seite 1).

## Einstellungen Phase 1
Spalten/Reihen, Icongröße, Icon-Rundung (mit Vorschau und Schnellwahl Eckig/Weich/Squircle/Kreis),
Beschriftungen + Schriftgröße, Hell/Dunkel/System (für Einstellungen und Panels), Dock an/aus,
Haptik an/aus, Wackeln an/aus, Layout sperren, Legacy-Icon-Hintergrund, Layout zurücksetzen (mit Rückfrage),
Layout exportieren/importieren (JSON), „Als Standard-Launcher festlegen“, Über/Lizenzen.
Erreichbar über den Bearbeitungsmodus und über einen eigenen Launcher-Eintrag (activity-alias „Klar-Einstellungen“).

## Qualität
- Manifest: `<queries><intent><action MAIN/><category LAUNCHER/></intent></queries>`, kein QUERY_ALL_PACKAGES.
- Theme: `android:windowShowWallpaper=true`, transparenter Fensterhintergrund, Edge-to-Edge
  (Systemhintergrund sichtbar). Statusleisten-Icons hell.
- Flüssig: Icons asynchron laden und cachen, keine Arbeit im Main-Thread, stabile Keys.
- Barrierefreiheit: Touch-Flächen ≥ 48 dp, contentDescription/TalkBack-Labels für Icons, Buttons, Badges,
  Bearbeitungsaktionen (z. B. „Kalender vom Homescreen entfernen“).
- Unit-Tests (JUnit, reine Kotlin-Logik): Raster/freie Stelle finden, Verschieben, Ordner bilden/auflösen,
  Auto-Add nur für neue Pakete, Deinstallation entfernt überall, Raster verkleinern, JSON-Rundlauf,
  Superellipse-Formel (r=1 ≈ Kreis, r=0 ≈ Quadrat).
- Build NUR mit `tgradle` (z. B. `cd "/home/zeus/Android Launcher" && tgradle assembleDebug testDebugUnitTest lint`),
  NIE `./gradlew` direkt, keine Gradle-Daemons, keine weiteren Subagenten. Pfad enthält ein Leerzeichen: immer quoten.
- Am Ende: Suche nach den Sperrbegriffen über den ganzen Projektordner (case-insensitive) muss leer sein.
- Dateien: `LICENSE` (GPLv3-Volltext), `THIRD_PARTY_LICENSES` (Inter OFL, Lucide ISC, AndroidX/Kotlin Apache-2.0),
  `README.md` (Deutsch: Funktionen, `./gradlew assembleDebug`, `adb install -r app/build/outputs/apk/debug/app-debug.apk`,
  als Standard-Launcher festlegen, Berechtigungen: keine).
