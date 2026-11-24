# Erzeugt die Flavor-Quellen der Demo-Apps (Name, Adaptive Icon, Kategorie, Demo-Benachrichtigungen).
# Aufruf: python3 -I demoapps/tools/generate.py <Projektordner>
# Farben und Stile wie in design/assets/kit.js (eigene Werte), Glyphen aus Lucide (ISC).
import os, re, sys, xml.etree.ElementTree as ET

ROOT = sys.argv[1]
ICONS = os.path.join(ROOT, "design/assets/icons")
OUT = os.path.join(ROOT, "demoapps/src")

# flavor: (Name, Glyph, Farbe1, Farbe2, Stil, Kategorie oder None, [(Titel, Text), ...])
APPS = {
    "telefon": ("Telefon", "phone", "#3BA7F5", "#1D6FD8", "flat", "social", []),
    "browser": ("Browser", "globe", "#46D0C2", "#1A8F9E", "light", None, []),
    "chat": ("Chat", "message-circle", "#9C7CFF", "#6A44E0", "flat", "social", [
        ("Mara", "Kommst du heute Abend zum Essen? Ich koche Linsencurry."),
        ("Jonas", "Bin in zehn Minuten da."),
        ("Familie", "Oma hat Fotos vom Sonntag geschickt."),
    ]),
    "musik": ("Musik", "music", "#FF8A6B", "#E8505B", "dark", "audio", []),
    "kamera": ("Kamera", "camera", "#6B7486", "#3B4252", "grad", "image", []),
    "galerie": ("Galerie", "image", "#FFB35C", "#F0627C", "grad", "image", [
        ("Neuer Rückblick", "Herbstspaziergang · 24 Fotos"),
        ("Geteiltes Album", "Lena hat 5 Fotos zu „Urlaub“ hinzugefügt."),
    ]),
    "kalender": ("Kalender", "calendar", "#FF6F6F", "#D9404F", "light", "productivity", [
        ("Zahnarzt in 30 Minuten", "10:30 – 11:00 · Praxis am Markt"),
        ("Elternabend", "Heute 19:00 – 20:30 · Grundschule"),
        ("Geburtstag", "Morgen hat Paul Geburtstag."),
    ]),
    "wetter": ("Wetter", "cloud-sun", "#6CC8FF", "#3A8DEB", "grad", None, [
        ("Regen ab 14 Uhr", "Rheinstadt · 12° · Schirm nicht vergessen"),
        ("Morgen sonnig", "Bis zu 15° und kaum Wind."),
    ]),
    "karten": ("Karten", "map", "#5BD18B", "#22A06B", "light", "maps", []),
    "mail": ("Mail", "mail", "#5B8CFF", "#3D5BDB", "light", "productivity", [
        ("Stadtwerke", "Ihre Jahresabrechnung ist da."),
        ("Lena", "Re: Wochenende – klingt gut, ich bin dabei!"),
        ("Bücherei", "Zwei Bücher sind bald fällig."),
    ]),
    "notizen": ("Notizen", "notebook-pen", "#FFD45C", "#F2A93B", "flat", "productivity", []),
    "aufgaben": ("Aufgaben", "list-todo", "#FFA05C", "#EE7340", "grad", "productivity", [
        ("Geschenk für Oma besorgen", "Fällig heute"),
    ]),
    "fitness": ("Fitness", "heart-pulse", "#FF7EA8", "#E0457A", "light", None, []),
    "banking": ("Banking", "landmark", "#3D5A80", "#22344F", "flat", None, []),
    "zuhause": ("Zuhause", "house", "#4FD6E8", "#1C9DBB", "light", None, []),
    "podcasts": ("Podcasts", "podcast", "#B67CFF", "#8343D6", "dark", "audio", []),
    "shop": ("Shop", "shopping-bag", "#FF9F45", "#E86A1F", "flat", None, [
        ("Paket kommt heute", "Zustellung zwischen 13 und 15 Uhr."),
        ("Bestellung verschickt", "Wanderschuhe, Größe 42"),
        ("Abholung bereit", "Dein Paket liegt in der Packstation am Bahnhof."),
    ]),
    "rechner": ("Rechner", "calculator", "#8B93A7", "#5A6276", "grad", "productivity", []),
    "uhr": ("Uhr", "clock", "#40475A", "#22262F", "grad", "productivity", []),
    "dateien": ("Dateien", "folder", "#58A6FF", "#2F74D9", "grad", "productivity", []),
    "uebersetzer": ("Übersetzer", "languages", "#5FD3A8", "#239A78", "grad", "productivity", []),
}

def n(x):
    s = ('%.3f' % x).rstrip('0').rstrip('.')
    return s if s not in ('-0', '') else '0'

def to_path(el):
    tag = el.tag.split('}')[-1]; a = el.attrib; f = float
    if tag == 'path': return a['d']
    if tag == 'circle':
        cx, cy, r = f(a['cx']), f(a['cy']), f(a['r'])
        return f"M{n(cx-r)},{n(cy)}a{n(r)},{n(r)} 0 1,0 {n(2*r)},0a{n(r)},{n(r)} 0 1,0 {n(-2*r)},0"
    if tag == 'ellipse':
        cx, cy, rx, ry = f(a['cx']), f(a['cy']), f(a['rx']), f(a['ry'])
        return f"M{n(cx-rx)},{n(cy)}a{n(rx)},{n(ry)} 0 1,0 {n(2*rx)},0a{n(rx)},{n(ry)} 0 1,0 {n(-2*rx)},0"
    if tag == 'line': return f"M{a['x1']},{a['y1']}L{a['x2']},{a['y2']}"
    if tag in ('polyline', 'polygon'):
        pts = re.split(r'[\s,]+', a['points'].strip())
        return 'M' + 'L'.join(f"{pts[i]},{pts[i+1]}" for i in range(0, len(pts), 2)) + ('Z' if tag == 'polygon' else '')
    if tag == 'rect':
        x, y = f(a.get('x', 0)), f(a.get('y', 0)); w, h = f(a['width']), f(a['height'])
        rx = f(a.get('rx', a.get('ry', 0))); ry = f(a.get('ry', a.get('rx', 0)))
        if rx == 0: return f"M{n(x)},{n(y)}h{n(w)}v{n(h)}h{n(-w)}Z"
        return (f"M{n(x+rx)},{n(y)}h{n(w-2*rx)}a{n(rx)},{n(ry)} 0 0,1 {n(rx)},{n(ry)}v{n(h-2*ry)}"
                f"a{n(rx)},{n(ry)} 0 0,1 {n(-rx)},{n(ry)}h{n(-(w-2*rx))}a{n(rx)},{n(ry)} 0 0,1 {n(-rx)},{n(-ry)}"
                f"v{n(-(h-2*ry))}a{n(rx)},{n(ry)} 0 0,1 {n(rx)},{n(-ry)}Z")
    raise SystemExit('unbekannt: ' + tag)

def argb(c): return "#FF" + c[1:].upper()

def style(c1, c2, st):
    """Hintergrund (zwei Verlaufsfarben) und Glyphenfarbe – wie fill() in kit.js."""
    if st == "light": return ("#FFFFFF", "#EEF1F5", c2)
    if st == "flat": return (c2, c2, "#FFFFFF")
    if st == "dark": return ("#2A2E37", "#15181E", c1)
    return (c1, c2, "#FFFFFF")

def esc(t): return t.replace("&", "&amp;").replace("<", "&lt;").replace("'", "\\'").replace('"', '\\"')

for flavor, (name, glyph, c1, c2, st, cat, notes) in APPS.items():
    res = os.path.join(OUT, flavor, "res")
    os.makedirs(os.path.join(res, "drawable"), exist_ok=True)
    os.makedirs(os.path.join(res, "values"), exist_ok=True)
    bg1, bg2, fg = style(c1, c2, st)
    with open(os.path.join(res, "drawable", "demo_icon_background.xml"), "w") as f:
        f.write(f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Eigene Farben (Verlauf), siehe design/assets/kit.js -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108Z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:endX="81"
                android:endY="108"
                android:startX="27"
                android:startY="0"
                android:type="linear">
                <item android:color="{argb(bg1)}" android:offset="0" />
                <item android:color="{argb(bg2)}" android:offset="1" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
''')
    root = ET.parse(os.path.join(ICONS, glyph + ".svg")).getroot()
    paths = [to_path(el) for el in root.iter() if el is not root]
    scale = 1.4  # Glyphe ≈ 46 % des sichtbaren Bereichs (72 dp)
    off = 54 - 12 * scale
    body = "\n".join(f'''        <path
            android:fillColor="#00000000"
            android:pathData="{p}"
            android:strokeColor="{argb(fg)}"
            android:strokeLineCap="round"
            android:strokeLineJoin="round"
            android:strokeWidth="2" />''' for p in paths)
    with open(os.path.join(res, "drawable", "demo_icon_foreground.xml"), "w") as f:
        f.write(f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Glyphe „{glyph}“ aus Lucide (ISC), siehe THIRD_PARTY_LICENSES -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="{scale}"
        android:scaleY="{scale}"
        android:translateX="{n(off)}"
        android:translateY="{n(off)}">
{body}
    </group>
</vector>
''')
    items = "\n".join(f'        <item>{esc(t)}|{esc(x)}</item>' for t, x in notes)
    with open(os.path.join(res, "values", "demo.xml"), "w") as f:
        f.write(f'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">{esc(name)}</string>
    <bool name="demo_media">{"true" if flavor == "musik" else "false"}</bool>
    <string-array name="demo_notifications">
{items}
    </string-array>
</resources>
''')
    man = os.path.join(OUT, flavor, "AndroidManifest.xml")
    with open(man, "w") as f:
        cat_attr = f'\n        android:appCategory="{cat}"' if cat else ""
        f.write(f'''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application{cat_attr} />
</manifest>
''')
print(",".join(APPS))
