# State Abbreviations

## Filter

This filter identifies US state abbreviations in text.

### Required Parameters

This filter has no required parameters.

### Optional Parameters

| Parameter                            | Description                                                                                                                                                                                                  | Default Value                                            |
|--------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `stateAbbreviationsFilterStrategies` | A list of filter strategies.                                                                                                                                                                                 | None                                                     |
| `enabled`                            | When set to false, the filter will be disabled and not applied                                                                                                                                               | `true`                                                   |
| `ignored`                            | A list of terms to be ignored by the filter.                                                                                                                                                                 | None                                                     |
| `capitalized`                        | Whether or not the first letter of the term must be capitalized to be redacted.                                                                                                                              | `false`                                                  |
| `windowSize`                         | Sets the size of the window (in terms) surrounding a span to look for contextual terms. If set, this value overrides the value of `span.window.size` in the configuration.                                   | The value of `span.window.size` which is by default `5`. |
| `priority`                           | The priority (integer) of this filter. Valid values are any positive integer, where a higher value indicates a higher priority. Priority is used for tie-breaking when two spans may be otherwise identical. | `0`                                                      |

### Filter Strategies

The filter may have zero or more filter strategies. When no filter strategy is given the default strategy of `REDACT` is
used. When multiple filter strategies are given the filter strategies will be applied in as they are listed.
See [Filter Strategies](#filter-strategies) for details.

| Strategy              | Description                                              |
|-----------------------|----------------------------------------------------------|
| `REDACT`              | Replace the sensitive text with a placeholder.           |
| `RANDOM_REPLACE`      | Replace the sensitive text with a similar, random value. |
| `STATIC_REPLACE`      | Replace the sensitive text with a given value.           |
| `CRYPTO_REPLACE`      | Replace the sensitive text with its encrypted value.     |
| `HASH_SHA256_REPLACE` | Replace the sensitive text with its SHA256 hash value.   |

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
   "name": "states-abbreviations-example",
   "identifiers": {
      "stateAbbreviation": {
         "stateAbbreviationFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```
