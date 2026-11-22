#!/usr/bin/env bash
# Collects the inputs and both ports' outputs from the last compare.sh run into office-bundle.zip,
# with open-check.ps1, for opening in Word and Excel.
#   input/  the fixtures      net/  phileas-dotnet's output      java/  the POI port's output
set -eu
S="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${FIXTURES:-$S/fixtures}"
B="$S/out/office-bundle"
rm -rf "$B" "$S/out/office-bundle.zip" && mkdir -p "$B/input" "$B/net" "$B/java"
for f in "$SRC"/*.docx "$SRC"/*.xlsx; do
  [ -f "$f" ] || continue
  name=$(basename "$f")
  cp "$f" "$B/input/"
  [ -f "$S/out/net/$name" ] && cp "$S/out/net/$name" "$B/net/"
  [ -f "$S/out/java/$name" ] && cp "$S/out/java/$name" "$B/java/"
done
cp "$S/office/open-check.ps1" "$B/"
(cd "$S/out" && zip -qr office-bundle.zip office-bundle)
echo "$S/out/office-bundle.zip: $(ls "$B/input" | wc -l) inputs, $(ls "$B/net" | wc -l) .NET outputs, $(ls "$B/java" | wc -l) Java outputs"
