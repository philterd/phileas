# RTF Redaction

The `phileas-rtf` module redacts Rich Text Format (`.rtf`) documents with a policy. The output is RTF: redacted text is replaced in place, in the runs that hold it, and the rest of the document is written back as it was, including its formatting, tables, fields, pictures, and anything Phileas does not read.

Detection is the same as for text, using the policy's filters, strategies, and context. Like all detection, it is probabilistic and depends on the policy; validate the output against your own documents.

## Adding the module

`phileas-rtf` is not included in `phileas`. Add it alongside `phileas` or `phileas-core`, with the same version:

```xml
<dependency>
    <groupId>ai.philterd</groupId>
    <artifactId>phileas-rtf</artifactId>
    <version>4.6.0</version>
</dependency>
```

It has no dependencies beyond `phileas-core`.

## Redacting a document

```java
final PhileasConfiguration configuration = new PhileasConfiguration(new Properties());
final RtfFilterService service = new RtfFilterService(configuration,
        new DefaultContextService(), new InMemoryVectorService(), null);

final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{}}}", Policy.class);

final byte[] input = Files.readAllBytes(Path.of("input.rtf"));
final BinaryDocumentFilterResult result = service.filter(policy, "my-context", input, MimeType.APPLICATION_RTF);

Files.write(Path.of("redacted.rtf"), result.getDocument());
```

For an `input.rtf` of:

```
{\rtf1\ansi{\fonttbl{\f0 Arial;}}\f0 Email {\b jane.doe@example.com} today.\par}
```

`redacted.rtf` is:

```
{\rtf1\ansi{\fonttbl{\f0 Arial;}}\f0 Email {\b \{\{\{REDACTED-email-address\}\}\}} today.\par}
```

The output MIME type may be `null` or `MimeType.APPLICATION_RTF`. As with the other document services, the service can be created without context and vector services and given them on each call.

### What is read

Text is read from the body and tables, footnotes and endnotes, headers and footers, comments, field instructions (such as a `HYPERLINK` target), and shape text, each paragraph or table cell detected on its own. Text is decoded with the document's code page and its fonts' character sets, and a value is found when it is split across formatting runs or written with `\'hh` or `\uN` escapes.

### Spans

A span's `characterStart` and `characterEnd` are offsets within its paragraph, table cell, or other run of text, and `paragraphNumber` is that run's position in the document. `apply(policy, input, spans, outputMimeType)` applies a list of spans, for example ones a reviewer accepted or added, by their paragraph number and offsets, after the policy's Word settings, as redaction does.

## Policy settings

The policy's [`config.word`](word_and_excel.md#word) settings apply to RTF where the format has the corresponding part:

| Setting | RTF |
|---|---|
| `headersFooters` | `\header` and `\footer` groups. `keep` leaves them unscanned. |
| `comments` | `\annotation` groups, with their `\atnid`, `\atnauthor`, and range marks. `remove` deletes them; `redact` keeps them and redacts their text. |
| `trackedChanges` | `\deleted` and `\revised` text and the `\revtbl` table. `accept` removes deleted text, including a deleted paragraph mark, which joins the paragraphs, and removes the revision marks and table; `redact` keeps them and redacts their text, including deleted text. |
| `hiddenText` | Text formatted `\v`. `remove` deletes it; `redact` keeps it and redacts it like visible text. |
| `uninspectableObjects` | `\object` groups. `remove` deletes them; `keep` leaves them. |
| `removeDocumentProperties` | The title, subject, keywords, comments, and category in `\info`, and the custom properties in `\userprops`. |

`charts` and `highlight` have no RTF counterpart and are ignored. Whatever the settings, `\author`, `\operator`, `\company`, and `\manager` in `\info` are cleared, a kept comment's author becomes `Redacted` and its initials are cleared, and each author in a kept revision table becomes `Redacted`.

## Leftover check

The [leftover check](filter_policies/filter_policies.md#leftover-check) scans the redacted document's text as the redaction does, after the policy's Word settings, so headers and footers kept with `headersFooters: keep` are not checked. A finding's `paragraphNumber` and offsets are as for a [span](#spans).

## Reading text

`RtfTextExtractor.extract` returns the visible text of an RTF document's body: its paragraphs, tables, footnotes, and endnotes, without headers and footers, comments, field instructions, hidden text, tracked deletions, or pictures and objects. For RTF that encapsulates HTML, as Outlook writes, the HTML-only parts are left out. [Email redaction](email.md) uses it to read an Outlook message whose only body is RTF.

## Untrusted input

A document that is not RTF, does not close its groups, nests groups more than 1000 deep, has a control word of more than 32 letters or a parameter of more than 10 digits, has a malformed `\'hh` escape, or declares more binary data than it has, is refused with an `RtfFormatException`. Reading is linear in the size of the document. Anything after the document's closing brace is kept as it is.

## Limitations

* `hiddenText: remove` removes text formatted hidden directly. Text hidden by a character or paragraph style is kept and redacted like visible text.
* Accepting tracked changes does not remove a deleted table row; its text is removed.
* An embedded object is removed or kept whole; an embedded document inside it is not redacted.
* A replacement is written in the formatting of the first character it replaces. In a multi-byte code page, a run of `\'hh` escapes that holds a redacted value is removed whole, with any other characters in the run.
