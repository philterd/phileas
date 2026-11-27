# CSV Redaction

`CsvFilterService` redacts CSV documents with a policy, field by field. Each field is detected on its own, so a match never joins values from neighboring fields, as it can when a CSV is redacted as plain text. The output is a CSV document: fields with nothing redacted are written back unchanged, including their quoting, and the line endings and whether the document ends with a line break are kept.

Detection is the same as for text, using the policy's filters, strategies, and context. Like all detection, it is probabilistic and depends on the policy; validate the output against your own data.

`CsvFilterService` is part of `phileas-core`.

## Redacting a document

```java
final PhileasConfiguration configuration = new PhileasConfiguration(new Properties());
final CsvFilterService service = new CsvFilterService(configuration,
        new DefaultContextService(), new InMemoryVectorService(), null);

final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{}}}", Policy.class);

final byte[] input = Files.readAllBytes(Path.of("input.csv"));
final BinaryDocumentFilterResult result = service.filter(policy, "my-context", input, MimeType.TEXT_CSV);

Files.write(Path.of("redacted.csv"), result.getDocument());

for (final Span span : result.getExplanation().appliedSpans()) {
    System.out.println(span.getRow() + ":" + span.getColumn() + " " + span.getCharacterStart() + "-"
            + span.getCharacterEnd() + " " + span.getText() + " -> " + span.getReplacement());
}
```

For this `input.csv`:

```
Name,Contact
"Doe, Jane","Email jane.doe@example.com, or call"
```

`redacted.csv` is:

```
Name,Contact
"Doe, Jane","Email {{{REDACTED-email-address}}}, or call"
```

and the program prints:

```
2:2 6-26 jane.doe@example.com -> {{{REDACTED-email-address}}}
```

The output MIME type may be `null` or `MimeType.TEXT_CSV`. As with the other document services, the service can be created without context and vector services and given them on each call.

### Spans

A CSV span's `row` and `column` give its field, counting from 1, and `characterStart` and `characterEnd` are offsets into the field's value, without quoting. `row` and `column` are set only on spans from a CSV document and are left out of other spans' JSON.

`apply(policy, input, spans, outputMimeType)` applies a list of spans, for example ones a reviewer accepted or added, by their row, column, and offsets.

## Reading the document

The policy's `config.csv` sets how the document is read and written:

| Setting | Default | Description |
|---|---|---|
| `delimiter` | `,` | The field delimiter: one character, not a line break. |
| `quote` | `"` | The quote character: one character, not a line break, different from the delimiter. |
| `encoding` | `UTF-8` | The character encoding, by its IANA name, such as `ISO-8859-1`. |

```json
{
  "identifiers": { "emailAddress": {} },
  "config": { "csv": { "delimiter": ";", "encoding": "ISO-8859-1" } }
}
```

A field that starts with the quote character runs to the next quote that is not doubled. Inside it, a doubled quote is one quote, and the delimiter and line breaks are part of the value. Records end at CRLF, LF, or CR. A redacted value is quoted when its field was quoted or when it holds the delimiter, the quote, or a line break.

A document that is not valid in the policy's encoding is refused with an `IllegalArgumentException`, rather than redacted with garbled text. A byte-order mark is kept, and a `UTF-16` document is written in the byte order it was read in.

## Options

Pass `CsvRedactionOptions` to the service's constructor:

| Option | Default | Description |
|---|---|---|
| `headerRow` | `true` | The first row is a header row: it is kept by `fullyRedactedColumns` and used for header context. It is scanned like any other row. |
| `useHeaderContext` | `false` | Detect each data field with its column's header in front of it (`SSN: 123456789`), so a header can help identify a value. Only the field is redacted. |
| `fullyRedactedColumns` | none | Columns, counting from 1, whose data fields are all replaced with `{{{REDACTED}}}`, whether or not anything is detected. Header rows are kept. Overrides the policy's column rules for those columns. |

## Column rules

The policy's [column rules](word_and_excel.md#column-rules) apply to a CSV document as to one worksheet: the `tabular` sheet entry without a `worksheet` applies, and an entry that names a worksheet does not. Under column rules, the entry's `headerRows` decide which rows are header rows, and header rows are never redacted. A `header` rule is matched against the last header row's value, trimmed.

## Leftover check

The [leftover check](filter_policies/filter_policies.md#leftover-check) scans each field of the redacted document, as the redaction does. Under column rules it skips header rows and ignored columns. A finding's `row` and `column` give its field, and its offsets are into the field's value.

## Redacted text output

`CsvFilterService` can return the document's redacted plain text in place of the redacted document. Ask for it with
`FilterOptions.defaults().withOutput(FilterOptions.Output.TEXT)`; `getText()` then returns the text as labeled
segments and `getDocument()` returns `null`. A call returns either the text or the document, not both, and the output
`MimeType` is not checked. The text comes from the same walk and the same detection the document redaction uses, so
each value has the replacement the document would have, and it is not included when the result is serialized to JSON.
The segments and their statuses (`TEXT`, `NO_TEXT`, `NOT_SCANNED`) are described under
[PDF redacted text output](filter_policies/pdf.md#redacted-text-output). The policy's
[leftover check](filter_policies/filter_policies.md#leftover-check) scans each segment's text, leaving out values the
redaction inserted, and each finding's `segment` is the label of its segment, with offsets into that segment's text.

The text is one segment labeled `rows`, decoded with the policy's `config.csv` `encoding`. Each data row is a set of
`header: value` lines, one per field with text, with a blank line between rows:

```
Name: Row one
Email: {{{REDACTED-email-address}}}

Name: Row two
SSN: none
```

A column's header is its field in the last header row (the sheet entry's `headerRows` under column rules, or the first
row with `headerRow`), as the redaction leaves it: under column rules the header row is not redacted, and without them
it is redacted like any other row. A column without a header is named by its number (`column 2`). Column rules give
the same values as in the document, and a field an `ignore` rule leaves unscanned is left out, where the document keeps
it as it was. A field's value is its text without quoting, with line breaks written as spaces. `withTextMetadata` has
no effect, since a CSV document has no metadata.

## Limits

These settings in [Settings](settings.md) bound the documents redacted. A document that exceeds one is refused with `CsvLimitExceededException`.

| Setting | Default |
|---|---|
| `csv.max.rows` | `1000000` |
| `csv.max.fields.per.row` | `10000` |
| `csv.max.field.characters` | `1000000` |

## Limitations

* The delimiter, quote, and encoding are not detected from the document; they come from the policy.
* Malformed quoting is read leniently: text after a closing quote is part of the value, and a quote that is never closed runs to the end of the document.
* A replacement character the encoding cannot represent, such as a non-Latin character from `RANDOM_REPLACE` in an `ISO-8859-1` document, is written as `?`.
* Fields are not checked for spreadsheet formulas. A value that starts with `=`, `+`, `-`, or `@` is kept as it is, so a spreadsheet that opens the output may treat it as a formula. A replacement can also start a field with one of these characters: one the policy chooses (`STATIC_REPLACE`, a `redactionFormat`, or `MAP_REPLACE`), or an empty replacement, which leaves the rest of the field at its start.
