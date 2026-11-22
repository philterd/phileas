# Checking the spike's output in Microsoft Office

Two things the Linux run cannot do: open the outputs in Word and Excel, and test documents that Word
and Excel wrote themselves. This covers both. Any Windows or Mac machine with desktop Office works for
authoring and hand checks; `open-check.ps1` needs Windows.

## 1. Build the bundle (Linux)

From `spikes/office-poi`, after the steps in the README:

```sh
./compare.sh
office/bundle.sh          # writes out/office-bundle.zip
```

The bundle holds `input/`, `net/` (phileas-dotnet output), and `java/` (POI port output) for every
fixture, plus `open-check.ps1`.

## 2. Open everything in Office (Windows)

1. Start Word and Excel once by hand and dismiss any sign-in, activation, or first-run dialogs, then
   close both. The script cannot answer dialogs; it records them as timeouts.
2. Unzip the bundle and unblock the files, so Protected View does not intercept them:

   ```powershell
   Get-ChildItem -Recurse .\office-bundle | Unblock-File
   ```

3. Run the check (Windows PowerShell 5.1 or later):

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\office-bundle\open-check.ps1 -InputDir .\office-bundle -OutDir .\office-results
   ```

   It writes `office-results\results.csv` and the text Office read from each file under
   `office-results\text\`. Expect a few seconds per file.

4. Copy `office-results` back and compare:

   ```sh
   python3 -I office/office-compare.py path/to/office-results
   ```

   A file is flagged when its Java output opened differently from the .NET output, when Office read
   different text from the two, or when Office found a PII value in the Java output that it did not
   find in the .NET output.

Known baseline: the `w-threaded-comments.docx` input's `commentsExtended` part uses the wrong
namespace (a fixture quirk; the Open XML SDK validator rejects it). Both ports remove that part.

## 3. Check by hand

The script cannot see a Word repair, so open these **Java** outputs by hand, compare with the .NET
output where something looks off, and note any "unreadable content" or repair prompt:

| File | Why | Also check |
| --- | --- | --- |
| `demo-example-document.docx` | Word-authored | Formatting looks the same as the .NET output |
| `lo-report.docx`, `lo-contacts.xlsx` | LibreOffice-authored | Hyperlinks point at `https://redacted.invalid/` |
| `w-chart.docx`, `x-pivot.xlsx`, `pd-header-footer.xlsx` | Carry POI's `/.xml` content-type override | Opens without a prompt |
| `w-chart-embedded-workbook.docx`, `pd-docx-chart-embedded-workbook.docx` | Chart with an embedded workbook | Right-click the chart, Edit Data: the workbook opens and is redacted |
| `pd-docx-embedded-workbook.docx` | Embedded Excel object | Double-click the object: it opens and is redacted |
| `w-threaded-comments.docx`, `x-comments.xlsx` | Threaded comments | Comments pane shows redacted text |
| `x-pivot.xlsx`, `pd-xlsx-with-pivot-cache.xlsx` | Pivot cache set to refresh on open | Opens and refreshes without an error, or fails the same way as the input (the generated pivot fixtures are minimal) |
| `w-tracked-deletion.docx` | Tracked deletion | Review, Reject All: the restored text is redacted |
| `w-runs.docx` | Breaks, tabs, hyphens, split runs | Line break and tab are still in place |

## 4. Author real documents (Windows or Mac)

Before writing anything, set the Office user name to an invented one (File, Options, General: user
name `Test Author`, initials `TA`). Word and Excel store it as the document's creator and last editor,
and on every comment and tracked change, and neither port removes document metadata.

Use only these synthetic values, which `leaks.py` searches for: SSN `123-45-6789`, email
`jane.doe@example.com`, phone `(555) 555-0142`, card `4111 1111 1111 1111`. (`987-65-4321` is in the
list but neither port's SSN filter detects it.) Save as `.docx`/`.xlsx`, not in compatibility mode,
and name the files `r-<feature>.docx` / `.xlsx`.

Word, one document per line or a few combined:

- [ ] Plain paragraphs with each value, some values bold or split by formatting mid-value
- [ ] A table with values in cells
- [ ] Header, footer, footnote, and endnote, each with a value
- [ ] Track Changes on: insert a value, delete a value, move a paragraph containing a value
- [ ] Modern comments: a comment and a reply, each with a value
- [ ] A hyperlink with a `mailto:` address, and a URL with an email in its query string
- [ ] A field: Insert, Quick Parts, Field, `HYPERLINK` to a `mailto:` address
- [ ] A text box and a shape with text containing a value
- [ ] SmartArt with a value in a node
- [ ] A chart (Insert, Chart) whose data sheet has a value in a series name and an unplotted column
- [ ] An embedded Excel worksheet (Insert, Object) with values
- [ ] Content controls (Developer tab): plain text and rich text controls holding values
- [ ] A document saved from Word's built-in résumé or letter template, with values filled in

Excel:

- [ ] Cells with each value as text, and an SSN stored as the number `123456789`
- [ ] A formula that copies a value cell (`=B2`)
- [ ] Legacy note and modern threaded comment, each with a value
- [ ] Page header and footer with a value
- [ ] A text box with a value
- [ ] A chart titled with a value
- [ ] A pivot table over a range containing values
- [ ] A second sheet, and a defined name

## 5. Run the authored documents

Copy them into a folder on the Linux machine, then:

```sh
FIXTURES=/path/to/authored ./compare.sh
FIXTURES=/path/to/authored office/bundle.sh
```

Read `compare.sh`'s table as before, then repeat steps 2 to 4 with the new bundle.

## What settles it

POI is fine if every Java output opens in Word and Excel the same way the .NET output does, Office
reads the same text from both, and the authored documents match on `compare.sh`'s checks as the
fixtures did. A Java-only repair prompt points at POI's package layer (rewritten relationships, the
`/.xml` override, or `core.xml`), which can be corrected before saving and rechecked.
