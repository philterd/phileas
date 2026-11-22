# Checking output in Microsoft Word and Excel

CI validates redacted output with the Open XML SDK (`src/test/validator`). Word and Excel can be
stricter, so before a release that changes how documents are written, open output in Office on Windows.

1. Run the module's tests (`mvn test -pl phileas-ooxml`). `target/ooxml-validation/` then holds each
   corpus document in `input/` and its redaction in `output/`. Add the redactions of any Word- or
   Excel-authored documents you want to check to `output/`, with the originals in `input/`.
2. Copy that folder to a Windows machine with Office. Start Word and Excel once by hand to dismiss any
   first-run dialogs, then close them and run:

   ```powershell
   Get-ChildItem -Recurse .\ooxml-validation | Unblock-File
   powershell -ExecutionPolicy Bypass -File open-check.ps1 -InputDir .\ooxml-validation -OutDir .\office-results
   ```

3. In `office-results\results.csv`, every file in `output` should open as its counterpart in `input`
   does. Some generated inputs are minimal and fail in Office themselves. Word reports no repair
   through COM, so also open a few outputs by hand and look for a repair prompt.
