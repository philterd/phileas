# Sections

## Filter

This filter identifies sections in text between a given start regular expression pattern and a given end regular expression pattern.

### Required Parameters

| Parameter      | Description                                             | Default Value |
| -------------- | ------------------------------------------------------- | ------------- |
| `startPattern` | A regular expression denoting the start of the section. | None          |
| `endPattern`   | A regular expression denoting the end of the section.   | None          |

### Optional Parameters

| Parameter                 | Description                                                    | Default Value |
| ------------------------- | -------------------------------------------------------------- | ------------- |
| `sectionFilterStrategies` | A list of filter strategies.                                   | None          |
| `enabled`                 | When set to false, the filter will be disabled and not applied | `true`        |
| `ignored`                 | A list of terms to be ignored by the filter.                   | None          |

### Filter Strategies

The filter may have zero or more filter strategies. When no filter strategy is given the default strategy of `REDACT` is used. When multiple filter strategies are given the filter strategies will be applied in order as they are listed. See [Filter Strategies](#filter-strategies) for details.

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
   "name": "sections-example",
   "identifiers": {
      "section": {
         "startPattern": "START",
         "endPattern": "END",
         "sectionFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
}
```
