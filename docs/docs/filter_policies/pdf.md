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
| `scaling`                  | `float`   | `1`         | The scaling factor to use when generating pdf image pages                                                                         |
| `dpi`                      | `int`     | `150`       | The DPI resolution for the  output pdf image page                                                                                 |
| `compressionQuality`       | `float`   | `1`         | Sets the compression quality to a value between 0 and 1. See javax.imageio.ImageWriteParam for more details                       |
| `preserveUnredactedPages`  | `boolean` | `false`     | If `true`, will transpose original PDF page to resulting document if no redaction is required on that page                        |

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
