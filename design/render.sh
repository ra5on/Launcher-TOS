#!/usr/bin/env bash
# Rendert alle Vorlagen: screens/*.html → export/screens/*.png (1080×2400)
# und store/*.html → export/store/*.png (1080×1920). Benötigt den lokalen Server:
#   python3 -m http.server 8765 --bind 127.0.0.1   (im Ordner store-screenshots)
set -euo pipefail
cd "$(dirname "$0")"
CHROME=$(ls -d ~/.cache/ms-playwright/chromium_headless_shell-*/chrome-linux/headless_shell | tail -1)
mkdir -p export/screens export/store
render() { # $1=url $2=out $3=w $4=h $5=scale
  "$CHROME" --headless --no-sandbox --disable-gpu-sandbox --hide-scrollbars \
    --force-device-scale-factor="$5" --window-size="$3,$4" --virtual-time-budget=4000 \
    --screenshot="$2" "$1" >/dev/null 2>&1
}
pat="${1:-}"
for f in screens/*.html; do
  n=$(basename "$f" .html); [[ -n "$pat" && "$n" != *$pat* ]] && continue
  render "http://127.0.0.1:8765/$f" "export/screens/$n.png" 360 800 3 && echo "ok $n"
done
# Store-Bilder aus store/list.tsv: name<TAB>screen<TAB>Überschrift<TAB>Unterzeile<TAB>Hintergrund
if [[ -z "$pat" || "$pat" == store ]]; then
  enc() { python3 -c 'import sys,urllib.parse;print(urllib.parse.quote(sys.argv[1]))' "$1"; }
  while IFS=$'\t' read -r name scr h sub bg; do
    [[ -z "$name" || ! -f "export/screens/$scr.png" ]] && { echo "fehlt: $scr"; continue; }
    render "http://127.0.0.1:8765/store/frame.html?s=$scr&h=$(enc "$h")&sub=$(enc "$sub")&bg=$bg" "export/store/$name.png" 360 640 3 && echo "ok store/$name"
  done < store/list.tsv
  render "http://127.0.0.1:8765/store/feature-graphic.html" "export/store/feature-graphic.png" 1024 500 1 && echo "ok feature-graphic"
fi
