# PDF Redaction Configuration

PDF redaction can be configured through the `config.pdf` path of a policy.

The available options are:

| Key                        | Type      | Default     | Description                                                                                                                       |
|----------------------------|-----------|-------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `redactionColor`           | `string`  | `black`     | The color of the redaction boxes drawn over the PII. A named color (`black`, `white`, `red`, `orange`, `yellow`, `green`, `blue`, `gray`) or a 6-digit hex value such as `#ff8800`. A [filter strategy](#per-strategy-redaction-color) can override it |
| `showReplacement`          | `boolean` | `false`     | If `true` then the output of the filter's strategy will be output on the redaction box in the PDF                                 |
| `replacementFont`          | `string`  | `helvetica` | The font to use for the replacement output. Available options are `helvetica`, `times`, and `courier`                             |
| `replacementMaxFontSize`   | `float`   | `12`        | The maximum font size for the replacement text. Best efforts will be made to fit the replacement text within the redaction box    |
| `replacementFontColor`     | `string`  | `white`     | The font color for the replacement. Takes the same values as `redactionColor`                                                     |
| `dpi`                      | `int`     | `150`       | The resolution, in dots per inch, of the image a redacted page is rendered to. It sets the image's detail and the output's file size, not the page size: a redacted page has the same size as the input page (its CropBox, turned by any page rotation). Must be greater than zero. A page that would render to more pixels than the `pdf.max.pixels.per.page` [setting](../settings.md#advanced-settings) allows is refused |
| `compressionQuality`       | `float`   | `1`         | Sets the compression quality to a value between 0 and 1. See javax.imageio.ImageWriteParam for more details                       |
| `preserveUnredactedPages`  | `boolean` | `false`     | If `true`, a page with nothing redacted on it (no redacted value and no bounding box) is copied to the output unchanged, with its text. Every other page is rasterized. |
| `scale`                    | `float`   | `0.25`      | Deprecated and ignored. A policy that sets it still loads. Before version 4.5.0 it multiplied a redacted page's size, so the default of `0.25` produced pages a quarter of the input's width and height |

### An Example PDF Configuration Policy

The following is an example policy setting the PDF redaction options.

```
{
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   },
   "config": {
     "pdf": {
        "redactionColor": "red",
        "showReplacement": true,
        "replacementFontColor": "yellow"
     }
   }
}
```

### Per-Strategy Redaction Color

A filter strategy can set its own `color`, which applies to the boxes drawn over the spans that strategy redacts and
overrides `config.pdf.redactionColor` for them. It takes the same values as `redactionColor`, and has no effect on text
redaction.

```
{
   "identifiers": {
      "ssn": {
         "ssnFilterStrategies": [
            {
               "strategy": "REDACT",
               "color": "red"
            }
         ]
      },
      "date": {
         "dateFilterStrategies": [
            {
               "strategy": "REDACT",
               "color": "#ff8800"
            }
         ]
      }
   },
   "config": {
      "pdf": {
         "redactionColor": "gray"
      }
   }
}
```

Here SSN boxes are red, date boxes are orange, and every other filter uses the policy-wide gray. The color for a box is
the strategy's `color` when it sets one, then `config.pdf.redactionColor`, then black. A value that is neither a known
name nor a 6-digit hex string renders black.

## Different Policies for Different Pages

A PDF that bundles different documents, such as a cover letter followed by medical records, can be redacted with a
policy per page set in one call. Pass a `PdfPagePolicies` to `PdfFilterService.filter`. The policy passed to `filter` is
the default: it applies to every page not assigned a policy, so every page is redacted under some policy.

```java
final Policy defaultPolicy = new Gson().fromJson(defaultPolicyJson, Policy.class);
final Policy recordsPolicy = new Gson().fromJson(recordsPolicyJson, Policy.class);
final Policy exhibitsPolicy = new Gson().fromJson(exhibitsPolicyJson, Policy.class);

final PdfPagePolicies pagePolicies = new PdfPagePolicies()
        .assign("2-5,8", recordsPolicy, "records")
        .assign("12-", exhibitsPolicy, "exhibits");

final BinaryDocumentFilterResult result = pdfFilterService.filter(defaultPolicy, context, body,
        MimeType.APPLICATION_PDF, pagePolicies);
```

Page sets number pages from 1 and accept single pages (`3`), ranges (`2-5`), lists (`1,2,5`), mixes (`2-5,8`), and
open-ended ranges (`12-`, page 12 to the end). `assign(pages, policy)` without a label uses the page set as the label.

* **Filters.** Each page's text is filtered with the filters, strategies, and post-filters of its policy.
* **Bounding boxes.** A policy's `graphical.boundingBoxes` are drawn only on pages assigned to that policy. The
  default policy's boxes are drawn only on unassigned pages.
* **Context.** One context and one context service are used for the whole document. Where two policies both use a
  `CONTEXT` replacement scope, a value gets the same replacement on pages under either policy.
* **Rendering.** `config.pdf` settings (`dpi`, `compressionQuality`, `preserveUnredactedPages`, `redactionColor`, and
  the replacement text settings) come from the default policy, so the output is rendered one way. A strategy's own
  `color` still applies.
* **Spans.** Each span's `pageAssignment` is the label of the assignment whose policy found it, or `default` for an
  unassigned page. Spans' page numbers refer to the original document. Without `PdfPagePolicies`, `pageAssignment` is
  `null`.
* **Validation.** A malformed page set, a page set that overlaps one already assigned, and a label that is blank,
  already used, or `default` are rejected by `assign` with an `IllegalArgumentException`. An assigned page beyond the
  document's last page is rejected by `filter`, before anything is redacted, with an `IllegalArgumentException`
  naming the pages.

`PdfFilterService.apply(policy, input, spans, outputMimeType, pagePolicies)` applies reviewed spans and draws each
policy's bounding boxes on its pages in the same way.

## Redacted Text Output

`PdfFilterService` can return a PDF's redacted plain text in place of the redacted PDF, for pipelines that need text,
such as preparing documents for AI training or a search index. Ask for it with `FilterOptions`:

```java
final FilterOptions options = FilterOptions.defaults().withOutput(FilterOptions.Output.TEXT);

final BinaryDocumentFilterResult result = pdfFilterService.filter(policy, contextService, vectorService, context,
        body, MimeType.APPLICATION_PDF, options);

for (final TextSegment segment : result.getText().getSegments()) {
    System.out.println(segment.getLabel() + ": " + segment.getText());
}
```

A call returns either the text or the document, not both. With text output, `getDocument()` returns `null`, the
output `MimeType` is ignored, and no page is rendered, so the call skips the cost of rendering pages to images. The
redacted document stays the default. Like the document, the text is not included when the result is serialized to
JSON.

The text comes from the same lines and the same detection the document redaction uses, so it is the text Phileas
checked. A redacted page's text cannot be read back from a redacted PDF, since the page is rendered to an image.

`getText()` returns a `RedactedText` holding one `TextSegment` per page, in page order, labeled `page 1`, `page 2`, and
so on. A page's lines are joined with a line break (`\n`); words a PDF places separately on one line are joined with a
space. Each detected value is replaced with its strategy's replacement, as in text redaction (`{{{REDACTED-ssn}}}`
with the default `REDACT` strategy). `config.pdf` settings such as `showReplacement` apply only to the document.
`RedactedText.join(separator)` joins the segments that have text.

Each segment has a status (`getStatus()`):

* `TEXT`: the page's redacted text.
* `NO_TEXT`: the page has no extractable text, such as a scanned page. Its text is empty, and its label is listed by
  `RedactedText.getSegmentsWithoutText()`, so it can be treated as unprocessed. OCR is not performed.
* `NOT_SCANNED`: the page's policy has no filters, such as a [page policy](#different-policies-for-different-pages)
  with only bounding boxes. Its text is left out rather than returned unredacted.

Text under a policy's `graphical.boundingBoxes` is left out of its page's text. A character counts as covered when its
glyph touches a box, and when a line's characters cannot be matched to its glyphs one for one, the whole line is left
out if any glyph is covered. A replacement that overlaps a box is kept, since it holds nothing from the document. A
page whose text is all under boxes has status `TEXT` with empty text, since it had text to read.

### Metadata

The document information dictionary's text entries (such as `Author`, `Title`, `Subject`, `Keywords`, `Creator`,
`Producer`, `CreationDate`, `ModDate`, and custom entries) are left out of the text by default. `withTextMetadata(true)` adds them as a last segment labeled `metadata`,
one `key: value` line per entry, redacted with the policy passed to `filter`. Its status is `NO_TEXT` when the
document has none, and `NOT_SCANNED` when that policy has no filters. XMP metadata is not included. The metadata's
spans are not in the result's explanation, since they are not on a page.

```java
final FilterOptions options = FilterOptions.defaults()
        .withOutput(FilterOptions.Output.TEXT)
        .withTextMetadata(true);
```

### Leftover Check

With text output, the policy's [leftover check](filter_policies.md#leftover-check) scans the returned text rather than
a redacted document: each page's text whole, with the check built from that page's policy, and the metadata segment
with the policy passed to `filter`. Values the redaction inserted are not reported. Each finding's `segment` is the
label of the segment it is in, and its offsets are into that segment's text; `pageNumber` (from 0) and `lineNumber`
are set for a page. Text output with no text in any segment is reported as not performed (`unreadableOutput`).

Text output is also supported for [Word and Excel](../word_and_excel.md#redacted-text-output),
[RTF](../rtf.md#redacted-text-output), [email](../email.md#redacted-text-output), and
[CSV](../csv.md#redacted-text-output) documents.
