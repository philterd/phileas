# Canadian Social Insurance Numbers (SIN)

## Filter

This filter identifies Canadian Social Insurance Numbers (SIN): nine digits, unformatted (`046454286`) or in three groups of three separated by a hyphen or a single space (`046-454-286`, `046 454 286`). Detected values are reported with the span type `canada-sin`.

The filter accepts the same separators as the [SSN filter](ssns-and-tins.md), including the Unicode hyphen substitutes, and the same line-wrap handling, so `046-454-` at the end of one line followed by `286` on the next is detected as one SIN. A match may not begin or end partway through a longer run of digits.

Every match must pass the Luhn (mod 10) check. There is no option to turn the check off. SINs beginning with 9, which are issued to temporary residents, are detected like any other.

CRA business numbers pass the Luhn check on their nine-digit root, so in the default mode they are detected as SINs. Set `onlyValidPrefixes` to `true` to drop values beginning with 0 or 8.

If both the `canadaSin` and `ssn` filters are enabled, an unformatted nine-digit value can match both. The existing span disambiguation decides which type it is reported as; there is no SIN-specific rule. Hyphenated forms never collide (`NNN-NNN-NNN` and `NNN-NN-NNNN`).

### Required Parameters

This filter has no required parameters.

### Optional Parameters

| Parameter                   | Description                                                                                                                                                                                                                                                          | Default Value                                            |
|-----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `canadaSinFilterStrategies` | A list of filter strategies.                                                                                                                                                                                                                                         | None                                                     |
| `onlyValidPrefixes`         | When `true`, do not detect values beginning with 0 or 8. Neither is issued as a personal SIN, and 8 is used for CRA business numbers. This rests on secondary sources rather than an official Government of Canada statement, so it is off by default.              | `false`                                                  |
| `enabled`                   | When set to false, the filter will be disabled and not applied                                                                                                                                                                                                       | `true`                                                   |
| `ignored`                   | A list of terms to be ignored by the filter.                                                                                                                                                                                                                         | None                                                     |
| `windowSize`                | Sets the size of the window (in terms) surrounding a span to look for contextual terms. If set, this value overrides the value of `span.window.size` in the configuration.                                                                                           | The value of `span.window.size` which is by default `5`. |
| `priority`                  | The priority (integer) of this filter. Valid values are any positive integer, where a higher value indicates a higher priority. Priority is used for tie-breaking when two spans may be otherwise identical.                                                         | `0`                                                      |

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
| `TOKEN`      | Compares the value of the sensitive text.                                | `==` , `!=`                        |
| `CONTEXT`    | Compares the filtering context.                                          | `==` , `!=`                        |
| `CONFIDENCE` | Compares the confidence in the sensitive text against a threshold value. | `<` , `<=`, `>` , `>=`, `==`, `!=` |

## Example Policy

```
{
   "identifiers": {
      "canadaSin": {
         "onlyValidPrefixes": true,
         "canadaSinFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```

The `canadaSin` filter requires redaction policy schema 1.3.0 as bundled with PhiSQL 1.5.0 or later.
