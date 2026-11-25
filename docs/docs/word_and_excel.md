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
| `fullyRedactedColumns` | none | Excel: 1-based columns whose data cells are all replaced with `{{{REDACTED}}}`, whether or not anything is detected. The header row is kept, or the `headerRows` of the sheet entry that applies. Overrides the policy's [column rules](#column-rules) for those columns. |
| `worksheet` | all | Excel: redact only the worksheet with this name. Applies with the policy's column rules: the other worksheets are left unchanged. |

## Column rules

A policy's top-level `tabular` section sets rules per column. A rule can replace every value in a column, leave a column unchanged, or scan a column with only some of the policy's filters. A rule on a known column, such as `SSN`, replaces malformed and partial values that detection could miss, and its cells are not run through detection.

```json
{
  "identifiers": {
    "emailAddress": {},
    "ssn": {}
  },
  "tabular": {
    "sheets": [
      {
        "worksheet": "Patients",
        "headerRows": 1,
        "unlistedColumns": "identifiers",
        "columns": [
          { "header": "SSN", "action": "redact", "strategies": [ { "strategy": "MASK" } ] },
          { "header": "Account ID", "action": "ignore" },
          { "header": "Notes", "action": "filter", "filters": [ "emailAddress" ] },
          { "index": 7, "action": "redact", "strategies": [ { "strategy": "REDACT" } ] }
        ]
      }
    ]
  }
}
```

With this policy, a `Patients` worksheet like this:

| Name | SSN | Account ID | Notes |
|---|---|---|---|
| Jane | 123-45-6789 | 4417 | Email jane.doe@example.com, SSN 123-45-6789 |
| John | 123-45 | 4418 | none |

is redacted to:

| Name | SSN | Account ID | Notes |
|---|---|---|---|
| Jane | `***********` | 4417 | Email `{{{REDACTED-email-address}}}`, SSN 123-45-6789 |
| John | `******` | 4418 | none |

The partial value `123-45` is replaced because the whole column is. The SSN in `Notes` is kept because that column is scanned only with `emailAddress`. `Name` has no rule, so it is scanned with every filter in `identifiers`.

### Sheet entries

Each entry in `sheets` applies to the worksheet named by `worksheet`, compared case-insensitively. An entry without `worksheet` applies to every worksheet that has no entry of its own. A worksheet with no entry that applies is redacted as it is without column rules. There can be one entry per worksheet name and one entry without a name.

| Field | Default | Description |
|---|---|---|
| `worksheet` | none | The worksheet the entry applies to. |
| `headerRows` | `1` | The number of leading rows that are headers. Header rows are never redacted. `0` means the sheet has no header, and columns can only be picked by `index`. |
| `unlistedColumns` | `identifiers` | What happens to columns without a rule: `identifiers` scans them with every filter in `identifiers`, `ignore` leaves them unchanged, and `redact` replaces every value with `unlistedStrategies`. |
| `unlistedStrategies` | one `REDACT` | The strategies that replace unlisted columns. Allowed only when `unlistedColumns` is `redact`. |
| `columns` | none | The column rules. |

`unlistedColumns: redact` makes an allow-list: only the columns with a rule, such as `ignore`, are kept as they are.

### Column rule

A rule picks its column by exactly one of:

* `header`: the displayed text of the column's cell in the last header row, trimmed. A formula is matched by its result, rich text as plain text, a boolean as `TRUE` or `FALSE`, and a merged cell by the value of its top-left cell. A number or date is matched by its stored value, as described under [Limitations](#limitations), so a date header shown as `1/1/2025` is matched as `45658`. When several columns have the header, the rule applies to all of them. A header that matches no column is logged as a warning, so one policy can cover workbooks whose columns differ.
* `index`: the column's position, counting from 1 (column A is 1).

When a header rule and an index rule pick the same column, the more protective one applies: `redact`, then `filter`, then `ignore`. Two `filter` rules scan the column with the filters of both, and of two `redact` rules the one listed first applies.

| `action` | Description |
|---|---|
| `redact` | Every data cell is replaced with `strategies`, with no detection. Every value is replaced, whatever it holds. |
| `ignore` | The data cells are left unchanged. |
| `filter` | Each data cell is scanned with only the filters in `filters`, named by their key in `identifiers` (for example `emailAddress`, `pheyes`). A key that holds several filters, such as `pheyes` or `dictionaries`, uses all of them. Each key must be in `identifiers`, or the policy is refused when it is used. |

Detection in `filter` and `identifiers` columns is probabilistic, as it is elsewhere. A `redact` column does not depend on detection, but whether a column holds what its header says is up to the workbook. Validate the output against your own data.

`strategies` and `unlistedStrategies` take the [filter strategies](filter_policies/filter_strategies.md) every filter supports (`REDACT`, `MASK`, `RANDOM_REPLACE`, `STATIC_REPLACE`, `CRYPTO_REPLACE`, `FPE_ENCRYPT_REPLACE`, `HASH_SHA256_REPLACE`, `LAST_4`, `TRUNCATE`, `ABBREVIATE`, and `MAP_REPLACE`) with their options, including `replacementScope`. They cannot have a `condition`, since a rule replaces every cell. `CRYPTO_REPLACE` and `FPE_ENCRYPT_REPLACE` use the policy's `crypto` and `fpe` keys. `%t` in `redactionFormat` is `full-column`, and each replaced cell is reported as a span with the classification `full-column`.

Column rules apply to cells: those of the workbook, and those of every workbook embedded in it or in a Word document, including the source workbook a chart keeps. An embedded workbook's worksheets are matched to sheet entries by their own names. The `fullyRedactedColumns` and `worksheet` options do not apply to embedded workbooks, and a header that matches no column in one is not reported.

Other content, such as comments, print headers and footers, shapes, chart labels and cached chart values, and pivot caches, is scanned with every filter in `identifiers`. Charts and pivot caches keep copies of the values in the columns they are built from, and those copies are scanned, not replaced by column. A value in a `redact` column that no filter detects, such as a partial SSN, is replaced in its cell but can remain in a chart label or a pivot cache built from that column. A value in an `ignore` column or a header row is scanned there and can be redacted.

The `fullyRedactedColumns` and `worksheet` options override the rules for a call, so an application can let a user pick columns while the policy holds the rules that stay the same from one workbook to the next.

The [leftover check](filter_policies/filter_policies.md#leftover-check) does not scan header rows or ignored columns, since the redaction leaves them as they are.

A policy that is not what the schema allows, such as an unknown key or action, a `redact` rule without `strategies`, or two rules for the same header, is refused when it loads.

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

* Text and number cells, each detected on its own. A bare number can be an SSN or an account number.
* Shared strings left behind by redacted cells, which are blanked
* Print headers and footers
* Legacy and threaded comments
* Text in shapes and text boxes
* Chart titles and labels, cached chart values, and each chart's embedded workbook
* Cached formula results
* Pivot caches
* Embedded Word and Excel documents, redacted in place

### Authors and document properties

Who wrote and edited a document is cleared whatever the policy, including a policy with no filters, and in embedded
Word and Excel documents. A chart's embedded workbook is cleared only when `redactCharts` is on; otherwise it is not
opened. These fields are not run through detection.

| Location | Cleared |
|---|---|
| Word comments and tracked changes | Each `w:author` is replaced with `Redacted`, and comment initials are removed. This includes insertions, deletions, moves, and formatting changes. |
| Word `word/people.xml` | Removed, with the authors' presence identifiers, which are often email addresses or directory IDs. |
| Word attached template | The template relationship and `w:attachedTemplate` in `word/settings.xml` are removed, because the template's path is often in the user's profile. Word attaches the Normal template on open. |
| Excel legacy comments (notes) | Each author is replaced with `Redacted`. When a note's text starts with its author's name and a colon, as Excel writes it, the name is replaced with `Redacted`. |
| Excel threaded-comment persons | Each person's `displayName` is replaced with `Redacted`, and its `userId` and `providerId` are removed. |
| Excel write-reservation user | `userName` on `fileSharing` in `xl/workbook.xml` is replaced with `Redacted`. |
| Excel shared-workbook revisions | User names on revision headers, in the user list, and on comment revisions are replaced with `Redacted`. |
| `docProps/core.xml` | `dc:creator` and `cp:lastModifiedBy` are emptied. |
| `docProps/app.xml` | `Company` and `Manager` are emptied. |

In Excel, authors are cleared on every worksheet, including when `worksheet` selects one. The text of comments is
redacted with the policy like any other text.

## Limitations

* A Word paragraph that is redacted loses its inline formatting, and hyperlinks and fields in it become plain text. Line breaks and tabs are kept. Paragraphs with nothing redacted are unchanged.
* A redacted Excel cell is written as text, and a redacted formula cell loses its formula.
* Column rules replace values in cells, not in the copies that charts and pivot caches keep. See [Column rules](#column-rules).
* Excel cells are detected by their stored value, not as Excel displays them. Excel stores most dates as serial numbers, so a date displayed as `3/14/1985` is scanned as `31120` and is not found by the date filter. A number shown through a display format, such as `123456789` shown as `123-45-6789`, is scanned as `123456789`.
* Embedded objects other than Word and Excel documents, such as legacy OLE objects, cannot be inspected.
* Document properties other than those listed under [Authors and document properties](#authors-and-document-properties), such as the title, subject, keywords, and custom properties, are not redacted.
* Only Word documents and Excel workbooks in the Transitional Office Open XML format, which Word and Excel save by default, are supported. Documents saved as Strict Open XML, and `.doc`, `.xls`, and OpenDocument files, are rejected.
