# PDF Redaction Configuration

PDF redaction can be configured through the `config.pdf` path of a policy.

The available options are:

| Key                        | Type      | Default     | Description                                                                                                                       |
|----------------------------|-----------|-------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `redactionColor`           | `string`  | `black`     | This is the color of the redaction boxes that are drawn over the PII. Available options are `white`, `black`, `red`, and `yellow` |
| `showReplacement`          | `boolean` | `false`     | If `true` then the output of the filter's strategy will be output on the redaction box in the PDF                                 |
| `replacementFont`          | `string`  | `helvetica` | The font to use for the replacement output. Available options are `helvetica`, `times`, and `courier`                             |
| `replacementMaxFontSize`   | `float`   | `12`        | The maximum font size for the replacement text. Best efforts will be made to fit the replacement text within the redaction box    |
| `replacementFontColor`     | `string`  | `white`     | The font color for the replacement. Available options match the `redactionColor` options                                          |
| `scaling`                  | `float`   | `1`         | The scaling factor to use when generating pdf image pages                                                                         |
| `dpi`                      | `int`     | `150`       | The DPI resolution for the  output pdf image page                                                                                 |
| `compressionQuality`       | `float`   | `1`         | Sets the compression quality to a value between 0 and 1. See javax.imageio.ImageWriteParam for more details                       |
| `preserveUnredactedPages`  | `boolean` | `false`     | If `true`, will transpose original PDF page to resulting document if no redaction is required on that page                        |

### An Example PDF Configuration Policy

The following is an example policy setting the PDF redaction options.

```
{
   "name": "example-pdf-policy",
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