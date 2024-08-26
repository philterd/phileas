# Dictionary

## Filter

This filter identifies custom text based on a given dictionary.

### Required Parameters

At least one of `terms` or `files` must be provided.

| Parameter | Description                                    | Default Value |
| --------- | ---------------------------------------------- | ------------- |
| `terms`   | A list of terms in the dictionary.             | None          |
| `files`   | A list of files containing terms one per line. | None          |

### Optional Parameters

| Parameter        | Description                                                                                                                                                                                                                      | Default Value         |
| ---------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------- |
| `enabled`        | When set to false, the filter will be disabled and not applied                                                                                                                                                                   | `true`                |
| `ignored`        | A list of terms to be ignored by the filter.                                                                                                                                                                                     | None                  |
| `fuzzy`          | When set to true, the dictionary will employ fuzzy comparisons. Use the `sensitivity` parameter to control the level of fuzziness. Setting this value to false will disable fuzziness and provide a higher level of performance. | `false`               |
| `classification` | Used to apply an arbitrary label to the identifier, such as "patient-id", or "account-number."                                                                                                                                   | `"custom-identifier"` |
| `sensitivity`    | Controls the "fuzziness" of allowed values to account for misspellings and derivations. Valid values are `low`, `medium`, and `high`. Only applies when `fuzzy` is set to `true`.                                                | `medium`              |

### Filter Strategies

The filter may have zero or more filter strategies. When no filter strategy is given the default strategy of `REDACT` is used. When multiple filter strategies are given the filter strategies will be applied in as they are listed. See [Filter Strategies ](filter-strategies.md)for details.

| Strategy              | Description                                              |
| --------------------- | -------------------------------------------------------- |
| `REDACT`              | Replace the sensitive text with a placeholder.           |
| `RANDOM_REPLACE`      | Replace the sensitive text with a similar, random value. |
| `STATIC_REPLACE`      | Replace the sensitive text with a given value.           |
| `CRYPTO_REPLACE`      | Replace the sensitive text with its encrypted value.     |
| `HASH_SHA256_REPLACE` | Replace the sensitive text with its SHA256 hash value.   |

### Conditions

Each filter strategy may have one condition. See [Conditions](#conditions) for details.

| Conditional  | Description                                                              | Operators                          |
| ------------ | ------------------------------------------------------------------------ | ---------------------------------- |
| `TOKEN`      | Compares the value of the sensitive text.                                | `==` , `!=`                        |
| `CONTEXT`    | Compares the filtering context.                                          | `==` , `!=`                        |
| `CONFIDENCE` | Compares the confidence in the sensitive text against a threshold value. | `<` , `<=`, `>` , `>=`, `==`, `!=` |

## Example Policy

```
{
   "name": "dictionary-example",
   "identifiers": {
      "dictionaries": [
         "customDictionary": {
            "terms": ["john", "jane", "doe"],
            "files": "c:\temp\dictionary.txt",
            "fuzzy": true,
            "sensitivity": "medium",
            "sectionFilterStrategies": [
               {
                  "strategy": "REDACT",
                  "redactionFormat": "{{{REDACTED-%t}}}"
               }
            ]
         }
      ]
   }   
}
```
