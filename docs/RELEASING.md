# Releases

Releases baut GitHub Actions (`.github/workflows/release.yml`):

1. `versionCode` und `versionName` in `app/build.gradle.kts` erhöhen.
2. Release-Notes unter `docs/release-notes/vX.Y.Z.md` anlegen.
3. Committen und pushen. Sobald die Release-Notes für die aktuelle `versionName` auf GitHub sind und es noch kein Tag `vX.Y.Z` gibt, legt der Workflow Tag **und** Release selbst an.
   Alternativ manuell taggen:
   ```
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```

Eine Version, die bereits ein Tag hat, wird nie ein zweites Mal veröffentlicht.
Der Workflow bricht ab, wenn ein gepushtes Tag nicht zur `versionName` passt, führt Tests und Lint aus, baut eine R8-optimierte, signierte APK, prüft Signatur und Paketdaten und veröffentlicht APK + SHA-256 als GitHub-Release.

## Dauerhafter Signaturschlüssel (einmalig einrichten)

Android installiert Updates nur, wenn sie mit **demselben Schlüssel** signiert sind – dann bleiben auch alle Einstellungen erhalten. Der Release-Workflow veröffentlicht deshalb **nur** mit dem dauerhaften Schlüssel aus den Secrets; fehlen sie, bricht er mit einer Fehlermeldung ab. (0.0.1 – 0.0.3 wurden noch mit Einmal-Schlüsseln signiert; der Wechsel auf 0.0.4 erfordert deshalb einmalig eine Neuinstallation.)

Schlüssel lokal erzeugen (gut aufbewahren – ein verlorener Schlüssel bedeutet: Updates nur nach Neuinstallation):

```
keytool -genkeypair -keystore layernx-release.jks -storetype PKCS12 -alias layernx -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=LayerNX Launcher, O=Moritz und Zen"
```

Dann im Repository unter *Settings → Secrets and variables → Actions* anlegen:

| Secret | Inhalt |
|---|---|
| `LAYERNX_KEYSTORE_BASE64` | Ausgabe von `base64 -w0 layernx-release.jks` |
| `LAYERNX_KEYSTORE_PASSWORD` | Keystore-Passwort |
| `LAYERNX_KEY_ALIAS` | `layernx` |
| `LAYERNX_KEY_PASSWORD` | Schlüssel-Passwort (bei PKCS12 = Keystore-Passwort) |

Lokal signierte Builds: dieselben Werte als Umgebungsvariablen `LAYERNX_KEYSTORE_FILE` (Pfad), `LAYERNX_KEYSTORE_PASSWORD`, `LAYERNX_KEY_ALIAS`, `LAYERNX_KEY_PASSWORD` setzen und `./gradlew assembleRelease` ausführen. Schlüsseldateien werden durch `.gitignore` nie eingecheckt.
