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

## Policy settings

The policy's `config.word` and `config.excel` set how a document is redacted beyond its text. They are read from each call's policy, so one service can redact with policies that differ. Without them, the defaults below apply.

```json
{
  "identifiers": { "emailAddress": {} },
  "config": {
    "word": { "comments": "redact", "highlight": true },
    "excel": { "pivotCaches": "keep" }
  }
}
```

### Word

| Setting | Default | Description |
|---|---|---|
| `headersFooters` | `redact` | `redact` redacts the text of headers and footers. `keep` leaves them unscanned. |
| `comments` | `remove` | `remove` deletes every comment with its author, including the threaded-comment copy and the comment ranges and references in the text. `redact` keeps comments and redacts their text. |
| `trackedChanges` | `accept` | `accept` accepts every insertion and deletion and removes the revision history, so deleted text is gone and inserted text is redacted as body text. A deleted table row is removed. `redact` keeps the revisions and redacts their text, including deleted text. |
| `hiddenText` | `remove` | `remove` deletes runs marked hidden. `redact` keeps them and redacts them like visible text. |
| `charts` | `redact` | `redact` redacts chart titles and labels, cached chart values, and each chart's embedded workbook. `keep` leaves charts as they are. |
| `uninspectableObjects` | `remove` | `remove` deletes embedded objects that cannot be inspected (anything but an embedded Word or Excel document). `keep` leaves them, unredacted, and reports them. |
| `removeDocumentProperties` | `true` | Clears the title, subject, keywords, categories, the comments field, and custom properties. `false` keeps them. Authorship is cleared either way. |
| `highlight` | `false` | Highlights each replacement in yellow. |

### Excel

| Setting | Default | Description |
|---|---|---|
| `printHeadersFooters` | `redact` | `redact` redacts print headers and footers. `keep` leaves them unscanned. |
| `charts` | `redact` | As for Word. |
| `formulaCaches` | `redact` | `redact` scans formula cells' cached results, and when any cell is redacted, clears the cached results and sets the workbook to recalculate when opened. `keep` leaves formula cells as they are, so a cached copy of a redacted value can remain. |
| `pivotCaches` | `redact` | `redact` redacts pivot caches and sets a redacted cache to refresh when opened. `keep` leaves them as they are. |
| `uninspectableObjects` | `remove` | As for Word. |
| `removeDocumentProperties` | `true` | As for Word. |

Cell text and comments are always redacted. A Word or Excel document embedded in another follows the same settings. The Word settings also apply to [RTF documents](rtf.md#policy-settings), where RTF has the corresponding part. A setting with an unknown key or value is refused when the policy loads.

## Per-call options

Pass `OoxmlRedactionOptions` to the service's constructor to pick worksheets and columns for its calls:

```java
final OoxmlRedactionOptions options = OoxmlRedactionOptions.defaults()
        .setFullyRedactedColumns(Set.of(2));

final OoxmlFilterService service = new OoxmlFilterService(configuration,
        new DefaultContextService(), new InMemoryVectorService(), new SecureRandom(), null, options);
```

| Option | Default | Description |
|---|---|---|
| `useHeaderContext` | `false` | Excel: detect each data cell with its column's header in front of it (`SSN: 123456789`), so a header can help identify a bare value. Only the cell is redacted. |
| `fullyRedactedColumns` | none | Excel: 1-based columns whose data cells are all replaced with `{{{REDACTED}}}`, whether or not anything is detected. The header row is kept, or the `headerRows` of the sheet entry that applies. Overrides the policy's [column rules](#column-rules) for those columns. |
| `worksheet` | all | Excel: redact only the worksheet with this name. Applies with the policy's column rules: the other worksheets are left unchanged. |

When calling `WordDocumentRedactor` or `XlsxRedactor` directly, set the Word and Excel settings on the options with `setWord` and `setExcel`.

## Column rules

A policy's top-level `tabular` section sets rules per column, for Excel workbooks and for [CSV documents](csv.md#column-rules). A rule can replace every value in a column, leave a column unchanged, or scan a column with only some of the policy's filters. A rule on a known column, such as `SSN`, replaces malformed and partial values that detection could miss, and its cells are not run through detection.

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

* `header`: the displayed text of the column's cell in the last header row, trimmed. A formula is matched by its result, rich text as plain text, a boolean as `TRUE` or `FALSE`, and a merged cell by the value of its top-left cell. A number or date is matched by its stored value, as described under [Limitations](#limitations), so a date header shown as `1/1/2025` is matched as `45658`. When several columns have the header, the rule applies to all of them. A header that matches no column is not an error, so one policy can cover workbooks whose columns differ; it is reported as a [warning](#unmatched-column-rules).
* `index`: the column's position, counting from 1 (column A is 1).

When a header rule and an index rule pick the same column, the more protective one applies: `redact`, then `filter`, then `ignore`. Two `filter` rules scan the column with the filters of both, and of two `redact` rules the one listed first applies.

| `action` | Description |
|---|---|
| `redact` | Every data cell is replaced with `strategies`, with no detection. Every value is replaced, whatever it holds. |
| `ignore` | The data cells are left unchanged. |
| `filter` | Each data cell is scanned with only the filters in `filters`, named by their key in `identifiers` (for example `emailAddress`, `pheyes`). A key that holds several filters, such as `pheyes` or `dictionaries`, uses all of them. Each key must be in `identifiers`, or the policy is refused when it is used. |

Detection in `filter` and `identifiers` columns is probabilistic, as it is elsewhere. A `redact` column does not depend on detection, but whether a column holds what its header says is up to the workbook. Validate the output against your own data.

`strategies` and `unlistedStrategies` take the [filter strategies](filter_policies/filter_strategies.md) every filter supports (`REDACT`, `MASK`, `RANDOM_REPLACE`, `STATIC_REPLACE`, `CRYPTO_REPLACE`, `FPE_ENCRYPT_REPLACE`, `HASH_SHA256_REPLACE`, `LAST_4`, `TRUNCATE`, `ABBREVIATE`, and `MAP_REPLACE`) with their options, including `replacementScope`. They cannot have a `condition`, since a rule replaces every cell. `CRYPTO_REPLACE` and `FPE_ENCRYPT_REPLACE` use the policy's `crypto` and `fpe` keys. `%t` in `redactionFormat` is `full-column`, and each replaced cell is reported as a span with the classification `full-column`.

Column rules apply to cells: those of the workbook, and those of every workbook embedded in it or in a Word document, including the source workbook a chart keeps. An embedded workbook's worksheets are matched to sheet entries by their own names. The `fullyRedactedColumns` and `worksheet` options do not apply to embedded workbooks, and a header that matches no column in one is reported as a warning marked as embedded.

Other content, such as comments, print headers and footers, shapes, chart labels and cached chart values, and pivot caches, is scanned with every filter in `identifiers`. Charts and pivot caches keep copies of the values in the columns they are built from, and those copies are scanned, not replaced by column. A value in a `redact` column that no filter detects, such as a partial SSN, is replaced in its cell but can remain in a chart label or a pivot cache built from that column. A value in an `ignore` column or a header row is scanned there and can be redacted.

The `fullyRedactedColumns` and `worksheet` options override the rules for a call, so an application can let a user pick columns while the policy holds the rules that stay the same from one workbook to the next.

The [leftover check](filter_policies/filter_policies.md#leftover-check) does not scan header rows or ignored columns, since the redaction leaves them as they are.

A policy that is not what the schema allows, such as an unknown key or action, a `redact` rule without `strategies`, or two rules for the same header, is refused when it loads.

### Unmatched column rules

A `header` rule that matches no column does nothing, and the column follows its sheet entry's `unlistedColumns`, which can leave sensitive values in the output:

* With `unlistedColumns: ignore`, the column is left unchanged, and the leftover check, which skips ignored columns, does not report it. A `redact` rule for `SSN` on a workbook whose header reads `Social Security` leaves that column as it was.
* With the default `unlistedColumns: identifiers`, the column is scanned with every filter, so values only the rule would replace, such as a partial SSN, can remain.

Each such rule is returned as a warning by `BinaryDocumentFilterResult.getWarnings()`, and logged. A warning (`RedactionWarning`) has the code `unmatched-header` (`getCode()`), the rule's `header`, the sheet entry's `worksheet` when the entry names one, and whether it is about an embedded workbook (`isEmbedded()`). It holds only values from the policy, never the document's sheet names or cell text. A rule that matches no column on several worksheets is reported once, and an `index` rule is never reported. A serialized result has `warnings` only when there are any. [Text output](#redacted-text-output) has the warnings of the worksheets it includes; it does not include embedded workbooks, so it has none of theirs. Review the output of a document with warnings, or fix the policy.

## What is redacted

Office documents keep copies of text in places other than the visible body.

**Word:**

* Body paragraphs, including tables
* Headers and footers
* Footnotes and endnotes
* Comments, which are removed by default. With `comments: redact`, a threaded comment's copy of the comment text (`threadedComments.xml`) is removed, with its companion parts, after the comment itself is redacted.
* Text in shapes, text boxes, and SmartArt
* Chart titles and labels, cached chart values, and each chart's embedded workbook
* Hyperlink targets. A target the policy flags is replaced with `https://redacted.invalid/`.
* Field instructions, such as `HYPERLINK` and `INCLUDETEXT`
* Tracked changes, which are accepted by default. With `trackedChanges: redact`, the text of tracked deletions, which could otherwise be restored with Reject Changes, is redacted.
* Hidden text, which is removed by default
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
Word and Excel documents, whatever `removeDocumentProperties` is set to. A chart's embedded workbook is cleared only when
`charts` is `redact`; otherwise it is not opened. These fields are not run through detection.

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

In Excel, authors are cleared on every worksheet, including when `worksheet` selects one. The text of comments that
are kept is redacted with the policy like any other text. `removeDocumentProperties` clears the other properties in
`docProps/core.xml` (`dc:title`, `dc:subject`, `cp:keywords`, `cp:category`, and `dc:description`) and every property in
`docProps/custom.xml`.

## Redacted text output

`OoxmlFilterService` can return a Word or Excel document's redacted plain text in place of the redacted document, for
pipelines that need text, such as preparing documents for AI training or a search index:

```java
final FilterOptions options = FilterOptions.defaults().withOutput(FilterOptions.Output.TEXT);

final BinaryDocumentFilterResult result = ooxmlFilterService.filter(policy, contextService, vectorService, context,
        document, MimeType.APPLICATION_DOCX, options);

for (final TextSegment segment : result.getText().getSegments()) {
    System.out.println(segment.getLabel() + ": " + segment.getText());
}
```

A call returns either the text or the document, not both. With text output, `getDocument()` returns `null`, no
document is written, and the output `MimeType` is not checked; the document's type is read from its content. The text comes from the same walk and the same detection the document redaction uses, after the
policy's settings are applied, so it holds what the redacted document would hold, and each value has the replacement
the document would have. Like the document, the text is not included when the result is serialized to JSON. The
segments and their statuses (`TEXT`, `NO_TEXT`, `NOT_SCANNED`) are described under
[PDF redacted text output](filter_policies/pdf.md#redacted-text-output).

### Word segments

One paragraph per line, in these segments, each present when the document has that part. Segments are in the order
their part first appears, with the metadata last:

| Label | Content |
|---|---|
| `body` | The body, including tables and text boxes. Always present. |
| `headers` | Header paragraphs. `NOT_SCANNED`, with no text, when `headersFooters` is `keep`. |
| `footers` | Footer paragraphs. `NOT_SCANNED`, with no text, when `headersFooters` is `keep`. |
| `footnotes` | Footnote paragraphs. |
| `endnotes` | Endnote paragraphs. |
| `comments` | Comment paragraphs, when `comments` is `redact`. With the default `remove`, comments are removed, so there is no segment. |
| `tracked changes` | The deleted text of tracked changes, one line per paragraph with a deletion, when `trackedChanges` is `redact`. With the default `accept`, deletions are removed, so there is no segment; inserted text is part of the paragraphs it is in. |

Runs `hiddenText` removes are not included. A text box's fallback copy for older versions of Word is not repeated,
and the separator notes Word keeps above footnotes and endnotes are left out.
Drawing text outside text boxes, hyperlink targets, field instructions, charts, and embedded objects are not included
in the text.

### Excel segments

One segment per worksheet, in workbook order, labeled `sheet: ` and the worksheet's name (`sheet: People`). Each data
row is a set of `header: value` lines, one per cell with text, with a blank line between rows:

```
Name: Row one
Email: {{{REDACTED-email-address}}}
Phone: {{{REDACTED-phone-number}}}

Name: Row two
Phone: none
```

A column's header is the text of the last header row: the sheet entry's `headerRows` under
[column rules](#column-rules), or the first row otherwise. Under column rules the header row is not redacted, as in the
document; without them the first row is redacted like any other. A column whose header cell is empty, or is a cell redaction does not
read, is named by its letter (`B: free text`). Column rules give the same values as in the document: a `redact` column holds the rule's
replacement, and a `filter` column is scanned with the rule's filters. Cells redaction does not scan are left out: an
`ignore` column, boolean and error cells, and formula cells unless `formulaCaches` is `redact`. A value is the cell's
stored value, as detection reads it (see [Limitations](#limitations)), so a date is its serial number. A line break in a
cell is written as a space. The `worksheet` per-call option limits the text to that worksheet. Comments, shapes, charts,
print headers and footers, pivot caches, and embedded objects are not included.

### Metadata

`withTextMetadata(true)` adds a last segment labeled `metadata`, one `key: value` line per property, redacted with the
policy: `title`, `subject`, `keywords`, `category`, and `description` from `docProps/core.xml`, then each custom
property by name. It follows `removeDocumentProperties`: with the default `true`, the document drops these properties,
so the segment is `NOT_SCANNED` with no text. Authorship (`dc:creator`, `cp:lastModifiedBy`, `Company`, and `Manager`)
is always cleared from the document and is never included. The segment is left out unless asked for.

### Leftover check

With text output, the policy's [leftover check](filter_policies/filter_policies.md#leftover-check) scans each segment's
text whole, rather than the redacted document, leaving out values the redaction inserted. Each finding's `segment` is
the label of its segment, with offsets into that segment's text. Text output with no text in any segment is reported as
not performed (`unreadableOutput`).

## Limitations

* In Word, a replacement is written in place of the redacted text, in the run that held its first character, and takes that run's formatting. The rest of the paragraph, including its formatting, hyperlinks, fields, tracked changes, and comment ranges, is unchanged. A value that spans runs is removed from the runs after the first.
* `hiddenText: remove` removes runs marked hidden in their own properties. A hidden run that holds a field marker keeps the marker and loses its text, so the field stays well formed. Text hidden by a character or paragraph style is kept and redacted like visible text.
* `trackedChanges: accept` removes a deleted paragraph mark without joining the paragraph to the next one.
* A redacted Excel cell is written as text, and a redacted formula cell loses its formula.
* Column rules replace values in cells, not in the copies that charts and pivot caches keep. See [Column rules](#column-rules).
* Excel cells are detected by their stored value, not as Excel displays them. Excel stores most dates as serial numbers, so a date displayed as `3/14/1985` is scanned as `31120` and is not found by the date filter. A number shown through a display format, such as `123456789` shown as `123-45-6789`, is scanned as `123456789`.
* Embedded objects other than Word and Excel documents, such as legacy OLE objects, cannot be inspected.
* Document properties other than those listed under [Authors and document properties](#authors-and-document-properties) are not redacted.
* Only Word documents and Excel workbooks in the Transitional Office Open XML format, which Word and Excel save by default, are supported. Documents saved as Strict Open XML, and `.doc`, `.xls`, and OpenDocument files, are rejected.
