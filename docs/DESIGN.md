# LayerNX Design-Standards

Quelle: [`launcher-mockup.html`](../launcher-mockup.html) (Layout „Zen“). Umsetzung: `app/src/main/java/com/layernx/launcher/ui/Ui.kt`.

## Farben

| Token | Wert | Verwendung |
|---|---|---|
| `bg` | `#0C0D10` | Hintergrund aller Screens |
| `panel` | `#1A1C22` | Kacheln, Listenzeilen, Zahnrad |
| `text` | `#F8F8FC` | Primärtext, Icons |
| `muted` | `#A0A3B1` | Sekundärtext, Marke |
| `border` | `#32323F` / `#292B33` | „Alle Apps“, Einstellungszeilen |
| `input` | `#242630`, Rand `#414451` | Eingabefelder |
| `accent` | `#552FFB` (Standard) | Primär-Kachel, Akzentlinie, Buttons, Glow |

Akzent-Presets: Violett `#552FFB`, Blau `#1F5FE0`, Grün `#0B7A5C`, Rot `#C9352A`, Pink `#B4256F`, Petrol `#0B6F86` – alle ≥ 4,5 : 1 Kontrast zu weißem Text (per Unit-Test geprüft). Icons auf dunklen Flächen nutzen den Akzent zu 60 % aufgehellt.

Hintergrund-Glow: radialer Verlauf Akzent @ 15 % Deckkraft bei 90 % / 35 %, ausblendend bis 65 %.

## Formen & Maße (dp)

- Seitenrand 16 · Startleisten-Kacheln Radius 22, Abstand 10 · „Alle Apps“ Radius 18, Höhe 52
- Listenzeilen Radius 14 (Apps/Auswahl) bzw. 12 mit 1-dp-Rand (Einstellungen), Höhe ≥ 56
- Buttons Radius 13, Höhe ≥ 48 · Eingabefelder Radius 10, Höhe ≥ 44 · Touch-Ziele ≥ 44
- Akzentlinie 28 × 3, Radius 2

## Typografie

Uhr 80 dp Light (fest in dp, damit große Systemschrift das Layout nicht sprengt) · Überschrift 22 sp Medium · Begrüßung 18 sp Medium · Fließtext 15 sp · Sekundär 12–13 sp · Marke/Eyebrow 11 sp, Großbuchstaben, Laufweite 0,14–0,18. Dock-Beschriftungen verkleinern sich bei Platzmangel bis 9 sp statt abgeschnitten zu werden.

## Interaktion

- **Fokus** (Tastatur): 2,5 dp weißer Rahmen, 3 dp nach innen versetzt – nur bei Tastenbedienung sichtbar (entspricht `:focus-visible`).
- **Touch:** weißer Ripple (20 %).
- Jede Aktion ist per Tastatur erreichbar; Auswahl-Dialoge sind Vollbild-„Shades“ mit Liste und Zurück-Button.
