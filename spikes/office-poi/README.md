# Office redaction spike: Apache POI vs phileas-dotnet

A spike, not a module: it is not part of the Maven build and is not published. It answers whether
phileas-dotnet's Word (`.docx`) and Excel (`.xlsx`) redaction can be reproduced in Java with Apache POI.

`java/` is a port of phileas-dotnet's `WordDocumentRedactor`, `XlsxRedactor`, `ChartRedactor`, and
`EmbeddedObjectRedactor` (`Redact`, `Detect`, and `ReadParagraphs` with default options; no
`ApplySpans`, highlighting, header context, whole-column redaction, or worksheet selection). It uses
POI's OPC layer for parts and relationships and the JDK DOM for each part's XML, and writes back only
the parts it changed. POI's `XWPFDocument`/`XSSFWorkbook` are used only by the `roundtrip` command.

`dotnet-harness/` is the reference side. It generates fixtures with phileas-dotnet's test helpers
(`WordDocs.cs` and `SpreadsheetTestHelper.cs`, copied from `phileas-dotnet/tests/Phileas.Tests`) and
redacts, detects, and validates (Open XML SDK, Office 2019) with phileas-dotnet.

## Running

Requires JDK 21+, Maven, the .NET 10 SDK, LibreOffice (`soffice`), and Python 3, with `phileas-dotnet`
cloned next to this repository. `PhilterDesktop` and `philter-demos` clones are used for extra fixtures
when present.

```sh
mvn -q install -DskipTests -pl phileas-core -am          # from the repository root
cd spikes/office-poi
(cd java && mvn -q compile dependency:build-classpath -Dmdep.outputFile=cp.txt)
(cd dotnet-harness && dotnet build -c Release -o bin/out)
./prepare-fixtures.sh
./compare.sh
```

`compare.sh` redacts every fixture with both ports and reports, per file: spans redacted, known PII
values left in any part (`leaks.py`, recursing into embedded packages), what phileas-dotnet's `Detect`
still finds in each output, validation errors, whether LibreOffice renders the two outputs to identical
text, and whether the two ports read identical paragraph text. `semdiff.py a b` compares two packages
part by part with namespaces resolved.

`office/` checks the outputs in Microsoft Word and Excel and covers authoring real test documents; see
[office/OFFICE-TESTING.md](office/OFFICE-TESTING.md).

## Results (2026-11-22, 43 fixtures)

- Same spans redacted, and no residual PII from either port beyond one value both SSN filters reject.
- Content parts (document, sheets, charts, comments, pivot caches, embedded packages) semantically
  identical between ports; identical paragraph text on all 26 `.docx` files; no new validation errors;
  LibreOffice renders both outputs to identical text.
- Differences are in POI's package layer: it rewrites `.rels` parts with relative targets, always
  writes `docProps/core.xml`, and adds a spurious `/.xml` content-type override when the input's
  default `.xml` content type is the main part's.
- Saving through POI's `XSSFWorkbook` without edits rewrites workbook, sheet, and shared-string parts of
  minimal workbooks, so a redactor should edit parts directly as this spike does.
- Not yet checked in Microsoft Word or Excel, or against a corpus of real-world documents.
