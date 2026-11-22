#!/usr/bin/env bash
# Builds the fixture corpus in fixtures/: phileas-dotnet's test-helper fixtures, the PhilterDesktop
# test documents and the philter-demos example document (copied from sibling clones, not committed),
# and a .docx and .xlsx written by LibreOffice.
set -eu
S="$(cd "$(dirname "$0")" && pwd)"
CODE="$(cd "$S/../../.." && pwd)"
rm -rf "$S/fixtures" && mkdir -p "$S/fixtures" "$S/out/lo-src"
dotnet "$S/dotnet-harness/bin/out/Harness.dll" fixtures "$S/fixtures"
for f in "$CODE"/PhilterDesktop/test-documents/*.docx "$CODE"/PhilterDesktop/test-documents/*.xlsx; do
  [ -f "$f" ] && cp "$f" "$S/fixtures/pd-$(basename "$f")"
done
[ -f "$CODE/philter-demos/example-document.docx" ] && cp "$CODE/philter-demos/example-document.docx" "$S/fixtures/demo-example-document.docx"
cd "$S/out/lo-src"
cat > report.html <<'HTML'
<html><head><title>Intake report</title></head><body>
<h1>Intake report</h1>
<p>Patient <b>Jane Doe</b>, SSN <b>123-45</b>-<i>6789</i>, can be reached at <a href="mailto:jane.doe@example.com">jane.doe@example.com</a> or (555) 555-0142.</p>
<ul><li>Card on file: 4111 1111 1111 1111</li><li>Backup contact: john@example.org</li></ul>
<table border="1"><tr><th>Field</th><th>Value</th></tr><tr><td>SSN</td><td>987-65-4321</td></tr><tr><td>Phone</td><td>555-555-0199</td></tr></table>
<p>Visit <a href="https://example.com/profile?email=jane.doe@example.com">the profile</a>.</p>
</body></html>
HTML
printf 'Name,SSN,Email,Phone\nJane,123-45-6789,jane.doe@example.com,(555) 555-0142\nJohn,987-65-4321,john@example.org,555-555-0199\n' > contacts.csv
soffice --headless --convert-to 'docx:MS Word 2007 XML' report.html >/dev/null 2>&1
soffice --headless --convert-to xlsx contacts.csv >/dev/null 2>&1
cp report.docx "$S/fixtures/lo-report.docx"
cp contacts.xlsx "$S/fixtures/lo-contacts.xlsx"
ls "$S/fixtures" | wc -l
