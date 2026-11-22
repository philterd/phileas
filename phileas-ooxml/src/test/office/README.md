# Checking output in Microsoft Word and Excel

CI validates redacted output with the Open XML SDK (`src/test/validator`). Word and Excel can be
stricter, so before a release that changes how documents are written, open output in Office on Windows.

On a Windows machine with Office and JDK 17 or later:

1. Start Word and Excel once by hand to dismiss any first-run dialogs, then close them.
2. From the repository root, in PowerShell, write the redacted corpus and open it in Office:

   ```powershell
   .\mvnw.cmd -pl phileas-ooxml -am test "-Dtest=OoxmlValidationCorpusTest" "-Dsurefire.failIfNoSpecifiedTests=false"
   powershell -ExecutionPolicy Bypass -File phileas-ooxml\src\test\office\open-check.ps1 -InputDir phileas-ooxml\target\ooxml-validation -OutDir office-results
   ```

   The test writes each corpus document to `phileas-ooxml\target\ooxml-validation\input\` and its
   redaction to `output\`. Add the redactions of any other Word- or Excel-authored documents you want to
   check to `output\`, with the originals in `input\`.
3. In `office-results\results.csv`, every file in `output` should open as its counterpart in `input`
   does. Some generated inputs are minimal and fail in Office themselves. Word reports no repair
   through COM, so also open a few outputs by hand and look for a repair prompt.

To check output built elsewhere, copy `target/ooxml-validation/` to the Windows machine, run
`Get-ChildItem -Recurse .\ooxml-validation | Unblock-File` so Office does not open the files in
Protected View, and run `open-check.ps1` on that folder.
