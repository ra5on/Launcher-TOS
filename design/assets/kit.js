// Gemeinsame Helfer für alle Vorlagen: Symbole einbetten, App-Icons mit
// Squircle-Maske, Statusleiste. Läuft über einen lokalen HTTP-Server.

const KIT = (() => {
  const base = new URL(".", document.currentScript.src).href;

  // Fiktive Apps: Name, Lucide-Glyph, Verlauf (eigene Farben)
  const APPS = {
    telefon:   ["Telefon", "phone", "#3BA7F5", "#1D6FD8", "flat"],
    browser:   ["Browser", "globe", "#46D0C2", "#1A8F9E", "light"],
    chat:      ["Chat", "message-circle", "#9C7CFF", "#6A44E0", "flat"],
    musik:     ["Musik", "music", "#FF8A6B", "#E8505B", "dark"],
    kamera:    ["Kamera", "camera", "#6B7486", "#3B4252"],
    galerie:   ["Galerie", "image", "#FFB35C", "#F0627C"],
    kalender:  ["Kalender", "calendar", "#FF6F6F", "#D9404F", "light"],
    wetter:    ["Wetter", "cloud-sun", "#6CC8FF", "#3A8DEB"],
    karten:    ["Karten", "map", "#5BD18B", "#22A06B", "light"],
    mail:      ["Mail", "mail", "#5B8CFF", "#3D5BDB", "light"],
    notizen:   ["Notizen", "notebook-pen", "#FFD45C", "#F2A93B", "flat"],
    aufgaben:  ["Aufgaben", "list-todo", "#FFA05C", "#EE7340"],
    uhr:       ["Uhr", "clock", "#40475A", "#22262F"],
    rechner:   ["Rechner", "calculator", "#8B93A7", "#5A6276"],
    einstell:  ["Einstellungen", "settings", "#7C8799", "#4E586B"],
    fitness:   ["Fitness", "heart-pulse", "#FF7EA8", "#E0457A", "light"],
    bank:      ["Banking", "landmark", "#3D5A80", "#22344F", "flat"],
    shop:      ["Shop", "shopping-bag", "#FF9F45", "#E86A1F", "flat"],
    bahn:      ["Bahn", "train-front", "#E9575F", "#B92F3D", "flat"],
    podcast:   ["Podcasts", "podcast", "#B67CFF", "#8343D6", "dark"],
    video:     ["Video", "clapperboard", "#F25F5C", "#C6303E", "flat"],
    spiele:    ["Spiele", "gamepad-2", "#8FE05B", "#3EAF4A", "dark"],
    lesen:     ["Lesen", "book-open", "#C98F5E", "#8C5A34", "flat"],
    zeitung:   ["Zeitung", "newspaper", "#A0A8B8", "#646C7E", "light"],
    home:      ["Zuhause", "house", "#4FD6E8", "#1C9DBB", "light"],
    auto:      ["Auto", "car", "#7FA2C8", "#4A6C94"],
    dateien:   ["Dateien", "folder", "#58A6FF", "#2F74D9"],
    kontakte:  ["Kontakte", "contact", "#9AA5B5", "#667185", "light"],
    sprache:   ["Übersetzer", "languages", "#5FD3A8", "#239A78"],
    rezepte:   ["Rezepte", "chef-hat", "#FFB86B", "#E58A3A"],
    fahrrad:   ["Radtouren", "bike", "#7EE0A0", "#2DAA63"],
    tv:        ["TV", "tv", "#5C6CFF", "#3A3FC9", "dark"],
    malen:     ["Skizzen", "brush", "#FF7AC8", "#C94AD8"],
    geld:      ["Haushalt", "wallet", "#58C9A0", "#2B8E70", "light"],
    training:  ["Training", "dumbbell", "#FF8A5C", "#D2533A"],
    flug:      ["Reisen", "plane", "#62B6FF", "#2F7FE0"],
  };

  // Superellipse |x|^n + |y|^n = 1 als Polygon. rounding 0..100 %:
  // 0 % = fast eckig (n=12), 100 % = Kreis (n=2).
  function squirclePath(rounding = 62, steps = 96) {
    const n = 12 - (rounding / 100) * 10;
    const pts = [];
    for (let k = 0; k < steps; k++) {
      const t = (k / steps) * Math.PI * 2;
      const c = Math.cos(t), s = Math.sin(t);
      const x = Math.sign(c) * Math.pow(Math.abs(c), 2 / n);
      const y = Math.sign(s) * Math.pow(Math.abs(s), 2 / n);
      pts.push(`${(50 + x * 50).toFixed(2)}% ${(50 + y * 50).toFixed(2)}%`);
    }
    return `polygon(${pts.join(",")})`;
  }

  // Unterschiedliche Icon-Stile, damit die Platzhalter wie echte,
  // gemischte App-Icons wirken (im Launcher kommen die Original-Icons).
  function fill(key) {
    const [, , c1, c2, st = "grad"] = APPS[key];
    if (st === "light") return { bg: "linear-gradient(180deg,#FFFFFF,#EEF1F5)", fg: c2 };
    if (st === "flat")  return { bg: c2, fg: "#fff" };
    if (st === "dark")  return { bg: "linear-gradient(180deg,#2A2E37,#15181E)", fg: c1 };
    return { bg: `linear-gradient(150deg,${c1},${c2})`, fg: "#fff" };
  }

  async function loadIcons(root = document) {
    const els = [...root.querySelectorAll("i[data-i]:not([data-done])")];
    const cache = {};
    await Promise.all(els.map(async el => {
      const name = el.dataset.i;
      if (!cache[name]) cache[name] = fetch(`${base}icons/${name}.svg`).then(r => r.text());
      el.innerHTML = (await cache[name]).replace(/<!--.*?-->/s, "");
      el.dataset.done = "1";
    }));
  }

  function appHTML(key, { label = true, badge = 0, size = 58, rounding, minus = false, cls = "" } = {}) {
    const [name, glyph] = APPS[key]; const f = fill(key);
    const r = rounding ?? window.ROUNDING ?? 62;
    return `<div class="app ${cls}" style="--icon:${size}px">
      ${minus ? `<div class="minus"><i data-i="minus"></i></div>` : ""}
      <div class="ico" style="color:${f.fg}">
        <div class="shape" style="clip-path:${squirclePath(r)};background:${f.bg}"></div>
        <i data-i="${glyph}"></i>
        ${badge ? `<div class="badge">${badge}</div>` : ""}
      </div>
      ${label ? `<div class="lbl">${name}</div>` : ""}
    </div>`;
  }

  function iconTile(key, size, rounding) {
    const [, glyph] = APPS[key]; const f = fill(key);
    const r = rounding ?? window.ROUNDING ?? 62;
    return `<div class="ico" style="width:${size}px;height:${size}px;position:relative;display:grid;place-items:center;color:${f.fg};flex:none">
      <div class="shape" style="position:absolute;inset:0;clip-path:${squirclePath(r)};background:${f.bg}"></div>
      <i data-i="${glyph}" style="position:relative;width:46%;height:46%"></i></div>`;
  }

  function statusbar(time = "08:15") {
    return `<div class="statusbar"><span>${time}</span>
      <div class="right"><i data-i="wifi"></i><i data-i="signal"></i><i data-i="battery-medium"></i><span style="font-size:12px">72 %</span></div></div>
      <div class="navbar"></div>`;
  }

  async function boot() {
    document.querySelectorAll("[data-app]").forEach(el => {
      const o = { ...el.dataset };
      el.outerHTML = appHTML(o.app, {
        label: o.label !== "0", badge: +o.badge || 0, size: +o.size || 58,
        minus: o.minus === "1", cls: o.cls || "",
        rounding: o.rounding ? +o.rounding : undefined,
      });
    });
    document.querySelectorAll("[data-tile]").forEach(el => {
      el.outerHTML = iconTile(el.dataset.tile, +el.dataset.size || 40, el.dataset.rounding ? +el.dataset.rounding : undefined);
    });
    if (!document.body.dataset.nostatus) document.body.insertAdjacentHTML("beforeend", statusbar(document.body.dataset.time));
    await loadIcons();
    await document.fonts.ready;
    document.body.dataset.ready = "1";
  }

  return { APPS, squirclePath, appHTML, iconTile, loadIcons, boot };
})();

document.addEventListener("DOMContentLoaded", () => KIT.boot());
