# PDF Redaction Configuration

PDF redaction can be configured through the `config.pdf` path of a policy.

The available options are:

| Key                      | Type      | Default     | Description                                                                                                                       |
|--------------------------|-----------|-------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `redactionColor`         | `string`  | `black`     | This is the color of the redaction boxes that are drawn over the PII. Available options are `white`, `black`, `red`, and `yellow` |
| `showReplacement`        | `boolean` | `false`     | If `true` then the output of the filter's strategy will be output on the redaction box in the PDF                                 |
| `replacementFont`        | `string`  | `helvetica` | The font to use for the replacement output. Available options are `helvetica`, `times`, and `courier`                             |
| `replacementMaxFontSize` | `float`   | `12`        | The maximum font size for the replacement text. Best efforts will be made to fit the replacement text within the redaction box    |
| `replacementFontColor`   | `string`  | `white`     | The font color for the replacement. Available options match the `redactionColor` options                                          |

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