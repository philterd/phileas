# Zip Codes

## Filter

This filter identifies zip codes in text.

Please note that the information used to map a zip code to a population is derived from publicly available USA census
data. While population mapping to zip code may be near the actual numbers it will most likely not be exact. Please use
the `POPULATION` conditional with this in mind.

### Required Parameters

This filter has no required parameters.

### Optional Parameters

| Parameter                 | Description                                                                                                                                                                                                  | Default Value |
|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| `zipCodeFilterStrategies` | A list of filter strategies.                                                                                                                                                                                 | None          |
| `enabled`                 | When set to false, the filter will be disabled and not applied                                                                                                                                               | `true`        |
| `ignored`                 | A list of terms to be ignored by the filter.                                                                                                                                                                 | None          |
| `requireDelimiter`        | When set to false, the filter will not require a dash in 9 digit zip codes, e.g. 12345-6789. Setting to false may increase the number of zip code false positives.                                           | `true`        |
| `priority`                | The priority (integer) of this filter. Valid values are any positive integer, where a higher value indicates a higher priority. Priority is used for tie-breaking when two spans may be otherwise identical. | `0`           |

### Filter Strategies

The filter may have zero or more filter strategies. When no filter strategy is given the default strategy of `REDACT` is
used. When multiple filter strategies are given the filter strategies will be applied in order as they are listed.
See [Filter Strategies](#filter-strategies) for details.

| Strategy              | Description                                                                                                                                                                            |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `REDACT`              | Replace the sensitive text with a placeholder.                                                                                                                                         |
| `RANDOM_REPLACE`      | Replace the sensitive text with a similar, random value.                                                                                                                               |
| `STATIC_REPLACE`      | Replace the sensitive text with a given value.                                                                                                                                         |
| `CRYPTO_REPLACE`      | Replace the sensitive text with its encrypted value.                                                                                                                                   |
| `HASH_SHA256_REPLACE` | Replace the sensitive text with its SHA256 hash value.                                                                                                                                 |
| `TRUNCATE`            | Replace the sensitive text by removing everything except `x` characters. (Set the number of characters to leave using the `truncateLeaveCharacters` parameter of the filter strategy.) |
| `ZERO_LEADING`        | Replace the sensitive text by zeroing the first 3 digits.                                                                                                                              |

### Conditions

Each filter strategy may have one condition. See [Conditions](#conditions) for details.

Please note that the information used to map a zip code to a population is derived from publicly available USA census
data. While population mapping to zip code may be near the actual numbers it will most likely not be exact. Please use
the `POPULATION` conditional with this in mind.

| Conditional  | Description                                                              | Operators                          |
|--------------|--------------------------------------------------------------------------|------------------------------------|
| `TOKEN`      | Compares the value of the sensitive text.                                | `==` , `!=`                        |
| `CONTEXT`    | Compares the filtering context.                                          | `==` , `!=`                        |
| `CONFIDENCE` | Compares the confidence in the sensitive text against a threshold value. | `<` , `<=`, `>` , `>=`, `==`, `!=` |
| `POPULATION` | Compares the population of the zip code against the 2010 census values.  | `<` , `<=`, `>` , `>=`, `==`, `!=` |

## Example Policy

```
{
   "name": "zip-code-example",
   "identifiers": {
      "zipCode": {
         "zipCodeFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```
