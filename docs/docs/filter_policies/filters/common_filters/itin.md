# Individual Taxpayer Identification Numbers (ITIN)

## Filter

This filter identifies US Individual Taxpayer Identification Numbers (ITIN). An ITIN has the same shape as an SSN but always begins with 9, such as `912-70-1234`. Detected values are reported with the span type `itin`.

The filter accepts the same forms as the [SSN filter](ssns-and-tins.md): hyphenated (`912-70-1234`), separated by a single space (`912 70 1234`), or unformatted (`912701234`). The same hyphen substitutes and line-wrap handling apply, so `912-` at the end of one line followed by `70-1234` on the next is detected as one ITIN.

The SSN filter never reports a value beginning with 9, so when both the `itin` and `ssn` filters are enabled an ITIN is reported only as `itin` and an SSN only as `ssn`.

Adoption Taxpayer Identification Numbers (ATIN), which have the form `9XX-93-XXXX`, are reported as `itin` unless `onlyValidRanges` is `true`.

### Required Parameters

This filter has no required parameters.

### Optional Parameters

| Parameter              | Description                                                                                                                                                                                                                                                                                                | Default Value                                            |
|------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `itinFilterStrategies` | A list of filter strategies.                                                                                                                                                                                                                                                                               | None                                                     |
| `onlyValidRanges`      | When `true`, only detect ITINs whose fourth and fifth digits fall in the ranges the IRS issues: 50 to 65, 70 to 88, 90 to 92, and 94 to 99 (IRS Publication 4757). This reduces false positives but drops ATINs (93) and any range the IRS adds after this release until the engine's range list is updated. | `false`                                                  |
| `enabled`              | When set to false, the filter will be disabled and not applied                                                                                                                                                                                                                                             | `true`                                                   |
| `ignored`              | A list of terms to be ignored by the filter.                                                                                                                                                                                                                                                               | None                                                     |
| `windowSize`           | Sets the size of the window (in terms) surrounding a span to look for contextual terms. If set, this value overrides the value of `span.window.size` in the configuration.                                                                                                                                 | The value of `span.window.size` which is by default `5`. |
| `priority`             | The priority (integer) of this filter. Valid values are any positive integer, where a higher value indicates a higher priority. Priority is used for tie-breaking when two spans may be otherwise identical.                                                                                               | `0`                                                      |

### Filter Strategies

The filter may have zero or more filter strategies. When no filter strategy is given the default strategy of `REDACT` is
used. When multiple filter strategies are given the filter strategies will be applied in order as they are listed.
See [Filter Strategies](#filter-strategies) for details.

| Strategy              | Description                                                                                                         |
|-----------------------|---------------------------------------------------------------------------------------------------------------------|
| `REDACT`              | Replace the sensitive text with a placeholder.                                                                      |
| `MASK`                | Replace each character of the sensitive text with a mask character (`*` by default).                                |
| `TRUNCATE`            | Replace all but a few characters of the sensitive text with a truncation character (`*` by default).                |
| `RANDOM_REPLACE`      | Replace the sensitive text with a similar, random value.                                                            |
| `STATIC_REPLACE`      | Replace the sensitive text with a given value.                                                                      |
| `CRYPTO_REPLACE`      | Replace the sensitive text with its encrypted value.                                                                |
| `HASH_SHA256_REPLACE` | Replace the sensitive text with its SHA256 hash value.                                                              |
| `FPE_ENCRYPT_REPLACE` | Replace the sensitive text with a value generated by [format-preserving encryption](../../filter_strategies.md#fpe) (FPE) |
| `LAST_4`              | Replace the sensitive text with just the last four characters of the text.                                          |

### Conditions

Each filter strategy may have one condition. See [Conditions](#conditions) for details.

| Conditional  | Description                                                              | Operators                          |
|--------------|--------------------------------------------------------------------------|------------------------------------|
| `TOKEN`      | Compares the value of the sensitive text.                                | `==`, `!=`, `startswith`           |
| `CONTEXT`    | Compares the filtering context.                                          | `==` , `!=`                        |
| `CONFIDENCE` | Compares the confidence in the sensitive text against a threshold value. | `<` , `<=`, `>` , `>=`, `==`, `!=` |

## Example Policy

```
{
   "identifiers": {
      "itin": {
         "onlyValidRanges": true,
         "itinFilterStrategies": [
            {
               "strategy": "LAST_4"
            }
         ]
      }
   }
}
```

The `itin` filter requires redaction policy schema 1.3.0 as bundled with PhiSQL 1.5.0 or later.
