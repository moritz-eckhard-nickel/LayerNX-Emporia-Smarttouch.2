#!/usr/bin/env bash
# Prints the GitHub release body for a LayerNX release.
# Usage: release-notes.sh VERSION APK_FILE APKSIGNER_OUTPUT SIGNING_MODE(permanent|one-off)
set -euo pipefail

version="$1"
apk="$2"
signature="$3"
mode="$4"

notes="docs/release-notes/v$version.md"
[ -f "$notes" ] || { echo "::error::$notes is missing" >&2; exit 1; }

apk_sha=$(sha256sum "$apk" | cut -d' ' -f1)
# apksigner prints e.g. "Signer #1 certificate SHA-256 digest: <hex>" or
# "V3.0 Signer: certificate SHA-256 digest: <hex>" depending on its version.
cert_sha=$(grep -m1 "certificate SHA-256 digest" "$signature" | grep -oE '[0-9a-f]{64}' || true)
[ ${#cert_sha} -eq 64 ] || { echo "::error::Certificate digest not found in apksigner output" >&2; exit 1; }

cat "$notes"
echo
echo "---"
echo
echo "**Datei:** \`LayerNX-v$version.apk\` · SHA-256: \`$apk_sha\`"
echo
echo "**Signaturzertifikat (SHA-256):** \`$cert_sha\`"
if [ "$mode" = "one-off" ]; then
  echo
  echo "> Diese Version wurde mit einem einmaligen Schlüssel signiert. Spätere Versionen mit dem dauerhaften Release-Schlüssel erfordern einmalig eine Neuinstallation (Einstellungen von LayerNX gehen dabei verloren)."
fi
