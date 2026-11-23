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
