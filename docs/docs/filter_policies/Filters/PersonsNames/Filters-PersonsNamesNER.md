# Person's Names (NER)

## Filter

This filter identifies person's names based on natural language processing (NLP) and named-entity recognition (NER) in text.

### Required Parameters

This filter has no required parameters.

### Optional Parameters

| Parameter                   | Description                                                      | Default Value |
| --------------------------- | ---------------------------------------------------------------- | ------------- |
| `removePunctuation`         | When set to true, punctuation will be removed prior to analysis. | `false`       |
| `firstNameFilterStrategies` | A list of filter strategies.                                     | None          |
| `enabled`                   | When set to false, the filter will be disabled and not applied   | `true`        |
| `ignored`                   | A list of terms to be ignored by the filter.                     | None          |

### Filter Strategies

The filter may have zero or more filter strategies. When no filter strategy is given the default strategy of `REDACT` is used. When multiple filter strategies are given the filter strategies will be applied in as they are listed. See [Filter Strategies](#filter-strategies) for details.

| Strategy              | Description                                               |
| --------------------- | --------------------------------------------------------- |
| `REDACT`              | Replace the sensitive text with a placeholder.            |
| `RANDOM_REPLACE`      | Replace the sensitive text with a similar, random value.  |
| `STATIC_REPLACE`      | Replace the sensitive text with a given value.            |
| `CRYPTO_REPLACE`      | Replace the sensitive text with its encrypted value.      |
| `HASH_SHA256_REPLACE` | Replace the sensitive text with its SHA256 hash value.    |
| `ABBREVIATE`          | Replace the sensitive text with the initials of the text. |

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
   "name": "ner-example",
   "identifiers": {
      "ner": {
         "nerFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```
