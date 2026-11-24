# Vorlagen & Play-Store-Bilder

- `screens/` – 15 Bildschirm-Vorlagen (HTML, 360 × 800 CSS-px → 1080 × 2400 PNG)
- `store/list.tsv` – Auswahl, Überschriften und Hintergrund der 8 Store-Bilder
- `store/frame.html` – Store-Rahmen (1080 × 1920), `store/feature-graphic.html` (1024 × 500)
- `assets/kit.css`, `assets/kit.js` – Design-System (Farben, Glas, Squircle-Maske, Platzhalter-Apps)
- `export/` – fertige PNGs (`store/` = Play-Store-Bilder 1080 × 1920 und Feature-Grafik)

Rendern:
    python3 -m http.server 8765 --bind 127.0.0.1 &   # im Ordner store-screenshots
    ./render.sh            # alles
    ./render.sh 04         # nur Bildschirme mit „04“ im Namen
    ./render.sh store      # nur Store-Bilder + Feature-Grafik

App-Name ändern: `store/feature-graphic.html` (Text „Launcher TOS“) und Überschrift in `store/overview.html`.
Später echte Screenshots einsetzen: PNG unter `export/screens/<name>.png` ablegen und `./render.sh store` laufen lassen.

Store-Bilder (Stand 1.0.0) nutzen echte Aufnahmen aus dem Emulator: `export/app/*.png`
(Auswahl und Texte in `store/list.tsv`). Die HTML-Vorlagen unter `screens/` sind die ursprünglichen Design-Entwürfe.
