# LayerNX Launcher

Ein schlanker, tastenfreundlicher Startbildschirm für das **Emporia TOUCHsmart.2 (V188.2, Android 11)** – im LayerNX-Design aus [`launcher-mockup.html`](launcher-mockup.html).

LayerNX ersetzt nur die Oberfläche. Firmware, Telefonie, Notruf, VoLTE und alle System-Apps bleiben unangetastet: kein Root, keine Änderungen am System. Benötigt werden nur zwei unkritische Berechtigungen (Hintergrund-Apps beenden, „Bitte nicht stören“ für den Stumm-Modus) und – nur für die Benachrichtigungsliste – der Benachrichtigungszugriff, den du selbst erlaubst.

## Funktionen

- **Zen-Startbildschirm** wie im Mockup: große Uhr, Datum, Begrüßung mit deinem Namen, Startleiste mit drei frei belegbaren Plätzen, „Alle Apps“.
- **Buttons oben rechts:** ⓘ Benachrichtigungen (mit Zähler, öffnen und löschen – das Emporia hat sonst keine Benachrichtigungsleiste) · **!** Hintergrund-Apps schließen und RAM freigeben · 🔔 Klingelton Ton → Vibration → Stumm · ⚙ Anpassen.
- **Standard-Apps statt Emporia-Apps:** Für Telefon, Nachrichten, Kamera, Kontakte, Browser, Galerie, Musik, Wecker, Kalender, Rechner und Einstellungen wählt LayerNX automatisch die beste App – zuerst deine systemweite Standard-App, dann von dir installierte Apps (z. B. Firefox, Open Camera, Simple Gallery), dann Android-Standard-Apps (AOSP-Telefon, MediaTek-Kamera). Emporia-Apps werden nur genutzt, wenn es sonst keine gibt. **„Nachrichten“ öffnet WhatsApp**, sobald es installiert ist.
- **Standard-Apps einstellbar:** Unter *Anpassen → Standard-Apps* lässt sich für jede Aufgabe eine beliebige App festlegen – oder lang auf eine App in „Alle Apps“ drücken → „Als Standard-App festlegen …“.
- **Tastatur zuerst:** Alles ist ohne Touchscreen bedienbar, mit gut sichtbarem Fokusrahmen.
  - Ziffern, `*`, `#` auf dem Startbildschirm → Wähltastatur mit der Ziffer (es wird nie automatisch angerufen)
  - Anruftaste → Telefon-App · Kamerataste → deine Kamera-App · Brieftaste → Nachrichten (WhatsApp) · Menütaste → Anpassen
  - In „Alle Apps“: Ziffern suchen per **T9** (`347` → Firefox), Löschtaste entfernt die letzte Ziffer, Zurück leert die Suche, Menütaste öffnet App-Aktionen
- **Emporia-Apps ausblenden** (optional, nur Anzeige) und einzelne Apps ausblenden.
- **Akzentfarben** (6 Presets, alle mit WCAG-AA-Kontrast). Oberfläche und Datum sind immer auf Deutsch, unabhängig von der Systemsprache.
- **Sparsam:** ~110 KB APK, keine Bibliotheken außer Kotlin, kein Polling, Icons werden im Hintergrund geladen und klein zwischengespeichert.
- **Updates behalten deine Einstellungen:** Ab 0.0.4 einfach die neue APK drüber installieren.

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
