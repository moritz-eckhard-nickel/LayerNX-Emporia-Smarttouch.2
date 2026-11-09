# LayerNX Launcher

Ein schlanker, tastenfreundlicher Startbildschirm für das **Emporia TOUCHsmart.2 (V188.2, Android 11)** – im LayerNX-Design aus [`launcher-mockup.html`](launcher-mockup.html).

LayerNX ersetzt nur die Oberfläche. Firmware, Telefonie, Notruf, VoLTE und alle System-Apps bleiben unangetastet: kein Root, keine Berechtigungen, keine Änderungen am System.

## Funktionen

- **Zen-Startbildschirm** wie im Mockup: große Uhr, Datum, Begrüßung mit deinem Namen, Startleiste mit drei frei belegbaren Plätzen, „Alle Apps“.
- **Standard-Apps statt Emporia-Apps:** Für Telefon, Nachrichten, Kamera, Kontakte, Browser, Galerie, Musik, Wecker, Kalender, Rechner und Einstellungen wählt LayerNX automatisch die beste App – zuerst deine systemweite Standard-App, dann von dir installierte Apps (z. B. Firefox, Open Camera, Simple Gallery), dann Android-Standard-Apps (AOSP-Telefon, MediaTek-Kamera). Emporia-Apps werden nur genutzt, wenn es sonst keine gibt.
- **Standard-Apps einstellbar:** Unter *Anpassen → Standard-Apps* lässt sich für jede Aufgabe eine beliebige App festlegen – oder lang auf eine App in „Alle Apps“ drücken → „Als Standard-App festlegen …“.
- **Tastatur zuerst:** Alles ist ohne Touchscreen bedienbar, mit gut sichtbarem Fokusrahmen.
  - Ziffern, `*`, `#` auf dem Startbildschirm → Wähltastatur mit der Ziffer (es wird nie automatisch angerufen)
  - Anruftaste → Telefon-App · Kamerataste → deine Kamera-App · Brieftaste → Nachrichten · Menütaste → Anpassen
  - In „Alle Apps“: Ziffern suchen per **T9** (`347` → Firefox), Löschtaste entfernt die letzte Ziffer, Zurück leert die Suche, Menütaste öffnet App-Aktionen
- **Emporia-Apps ausblenden** (optional, nur Anzeige) und einzelne Apps ausblenden.
- **Akzentfarben** (6 Presets, alle mit WCAG-AA-Kontrast), Deutsch und Englisch.
- **Sparsam:** ~110 KB APK, keine Bibliotheken außer Kotlin, kein Polling, Icons werden im Hintergrund geladen und klein zwischengespeichert.

## Installation

1. Die APK aus den [Releases](../../releases) herunterladen (z. B. per Telegram auf das Gerät schicken, siehe [`redditposts/1.md`](redditposts/1.md)) und installieren.
2. LayerNX öffnen und auf **„Als Standard-Launcher festlegen“** tippen (oder *Anpassen → System*).
3. Optional unter *Anpassen → Standard-Apps* die Apps für jede Aufgabe wählen.

Prüfsumme: Zu jeder APK gibt es eine `.sha256`-Datei; das Signaturzertifikat steht in den Release-Notes.

## Zurück zum Emporia-Launcher

*Anpassen → System → Zurück zum Emporia-Launcher* öffnet die Auswahl der Startbildschirm-App; dort den Emporia-Launcher („Simple Launcher“) wählen. Falls die Einstellungen nicht erreichbar sind, per ADB:

```
adb shell cmd package set-home-activity com.emporia.launcher/.activity.MainActivity
```

Oder LayerNX einfach deinstallieren – Android fällt dann automatisch auf den Emporia-Launcher zurück.

## Entwicklung

Voraussetzungen: JDK 17+, Android SDK (Platform 35).

```
./gradlew testDebugUnitTest lintDebug assembleDebug
```

- Unit-Tests für T9-Suche, Standard-App-Auswahl und Farbkontraste.
- Robolectric-UI-Tests mit echter Grafik auf der Displayfläche des Geräts (320 × 497 dp) – Screenshots landen in `app/build/screenshots/` und als CI-Artefakt.
- Aufbau: `core/` (reine Logik), `data/` (Apps, Einstellungen, Standard-App-Auflösung), `system/` (Starten von Apps & System-Einstellungen), `ui/` (LayerNX-Bausteine).

Design-Regeln: [`docs/DESIGN.md`](docs/DESIGN.md) · Release-Prozess: [`docs/RELEASING.md`](docs/RELEASING.md) · Geräteinfos: [`system-info.md`](system-info.md)

---

Moritz und Zen · LayerNX
