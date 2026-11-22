# Word and Excel Redaction

The `phileas-ooxml` module redacts Microsoft Word (`.docx`) and Excel (`.xlsx`) documents with a policy. The output is a document of the same format, still editable: redacted text is replaced in the document's XML rather than drawn over. Parts of the document that redaction does not change are written back unchanged.

Detection is the same as for text, using the policy's filters, strategies, and context. Like all detection, it is probabilistic and depends on the policy; validate the output against your own documents.

## Adding the module

`phileas-ooxml` is not included in `phileas`. Add it alongside `phileas` or `phileas-core`, with the same version:

```xml
<dependency>
    <groupId>ai.philterd</groupId>
    <artifactId>phileas-ooxml</artifactId>
    <version>4.6.0</version>
</dependency>
```

It uses the package layer of Apache POI.

## Redacting a document

`OoxmlFilterService` takes the document's bytes and returns the redacted document and the spans redacted:

```java
final PhileasConfiguration configuration = new PhileasConfiguration(new Properties());
final OoxmlFilterService service = new OoxmlFilterService(configuration,
        new DefaultContextService(), new InMemoryVectorService(), null);

final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{}}}", Policy.class);

final byte[] input = Files.readAllBytes(Path.of("input.docx"));
final BinaryDocumentFilterResult result = service.filter(policy, "my-context", input, MimeType.APPLICATION_DOCX);

Files.write(Path.of("redacted.docx"), result.getDocument());

for (final Span span : result.getExplanation().appliedSpans()) {
    System.out.println(span.getParagraphNumber() + " " + span.getCharacterStart() + "-" + span.getCharacterEnd()
            + " " + span.getText() + " -> " + span.getReplacement());
}
```

For a document whose second paragraph is `Email jane.doe@example.com or call (555) 555-0142.`, this prints:

```
1 6-26 jane.doe@example.com -> {{{REDACTED-email-address}}}
```

The format is read from the document itself. The output MIME type may be `null`, or must match the input: `MimeType.APPLICATION_DOCX` or `MimeType.APPLICATION_XLSX`. A document that is not a Word document or Excel workbook, including templates, macro-enabled files, and the legacy `.doc` and `.xls` formats, is rejected with an `IllegalArgumentException`.

As with `PdfFilterService`, the service can be created without context and vector services and given them on each call, with `filter(policy, contextService, vectorService, context, input, outputMimeType)`.

### Spans

A span's `characterStart` and `characterEnd` are offsets within its paragraph (Word) or cell (Excel), and `paragraphNumber` is that paragraph's or cell's position in the document:

* **Word:** body paragraphs (including tables), then headers and footers, footnotes, endnotes, and comments.
* **Excel:** cells, sheet by sheet in workbook order, then row by row.

A span with `paragraphNumber` -1 was found in content that is not a body paragraph or cell, such as a chart, a shape, a hyperlink target, or an embedded object.

### Applying spans

`apply(policy, input, spans, outputMimeType)` applies a list of spans, for example ones a reviewer accepted or added, by their paragraph number and offsets. Content that spans cannot be placed in (paragraph number -1) is redacted again with the policy.

## Options

Pass `OoxmlRedactionOptions` to the service's constructor to change what is redacted:

```java
final OoxmlRedactionOptions options = OoxmlRedactionOptions.defaults()
        .setRedactCharts(false)
        .setFullyRedactedColumns(Set.of(2));

final OoxmlFilterService service = new OoxmlFilterService(configuration,
        new DefaultContextService(), new InMemoryVectorService(), new SecureRandom(), null, options);
```

| Option | Default | Description |
|---|---|---|
| `highlight` | `false` | Word: each replacement is highlighted in yellow. |
| `redactHeadersFooters` | `true` | Redact Word headers and footers and Excel print headers and footers. |
| `redactCharts` | `true` | Redact chart titles and labels, cached chart values, and charts' embedded workbooks. |
| `removeUninspectableEmbeddedObjects` | `true` | Remove embedded objects that cannot be inspected (anything but an embedded Word or Excel document). When `false` they are kept, unredacted. |
| `redactFormulaValues` | `true` | Excel: redact formula cells' cached results, and when any cell is redacted, clear the cached results and set the workbook to recalculate when opened. |
| `redactPivotCaches` | `true` | Excel: redact pivot caches and set a redacted cache to refresh when opened. |
| `useHeaderContext` | `false` | Excel: detect each data cell with its column's header in front of it (`SSN: 123456789`), so a header can help identify a bare value. Only the cell is redacted. |
| `fullyRedactedColumns` | none | Excel: 1-based columns whose data cells are all replaced with `{{{REDACTED}}}`, whether or not anything is detected. The header row is kept. |
| `worksheet` | all | Excel: redact only the worksheet with this name. |

## What is redacted

Office documents keep copies of text in places other than the visible body.

**Word:**

* Body paragraphs, including tables
* Headers and footers
* Footnotes, endnotes, and comments. A threaded comment's copy of the comment text (`threadedComments.xml`) is removed, with its companion parts, after the comment itself is redacted.
* Text in shapes, text boxes, and SmartArt
* Chart titles and labels, cached chart values, and each chart's embedded workbook
* Hyperlink targets. A target the policy flags is replaced with `https://redacted.invalid/`.
* Field instructions, such as `HYPERLINK` and `INCLUDETEXT`
* Tracked deletions, whose text could otherwise be restored with Reject Changes
* Embedded Word and Excel documents, redacted in place

**Excel:**

* Text, number, and date cells. A bare number can be an SSN or an account number.
* Shared strings left behind by redacted cells, which are blanked
* Print headers and footers
* Legacy and threaded comments, and threaded comment authors
* Text in shapes and text boxes
* Chart titles and labels, cached chart values, and each chart's embedded workbook
* Cached formula results
* Pivot caches
* Embedded Word and Excel documents, redacted in place

## Limitations

* A Word paragraph that is redacted loses its inline formatting, and hyperlinks and fields in it become plain text. Line breaks and tabs are kept. Paragraphs with nothing redacted are unchanged.
* A redacted Excel cell is written as text, and a redacted formula cell loses its formula.
* Embedded objects other than Word and Excel documents, such as legacy OLE objects, cannot be inspected.
* Document properties, such as the author in `docProps/core.xml`, are not redacted.
* Only Word documents and Excel workbooks in the Transitional Office Open XML format, which Word and Excel save by default, are supported. Documents saved as Strict Open XML, and `.doc`, `.xls`, and OpenDocument files, are rejected.
