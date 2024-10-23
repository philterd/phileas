# Identifier

## Filter

This filter identifies custom text based on a given regular expression.&#x20;

The Identifier filter accepts a list of regular expression-based identifiers. See the policy at the bottom of this page for an example.&#x20;

_Note that backslashes in the regular expression will need to be escaped for the policy to be valid JSON._

### Required Parameters

This filter has no required parameters.

### Optional Parameters

| Parameter        | Description                                                                                    | Default Value         |
| ---------------- | ---------------------------------------------------------------------------------------------- | --------------------- |
| `enabled`        | When set to false, the filter will be disabled and not applied                                 | `true`                |
| `ignored`        | A list of terms to be ignored by the filter.                                                   | None                  |
| `caseSensitive`  | When set to true, the regular expression will be case sensitive.                               | `true`                |
| `classification` | Used to apply an arbitrary label to the identifier, such as "patient-id", or "account-number." | `"custom-identifier"` |
| `pattern`        | A regular expression for the identifier. _Note that backslashes will need to be escaped._      | `\b[A-Z0-9_-]{4,}\b`  |

### Filter Strategies

The filter may have zero or more filter strategies. When no filter strategy is given the default strategy of `REDACT` is used. When multiple filter strategies are given the filter strategies will be applied in as they are listed. See [Filter Strategies ](filter-strategies.md)for details.

| Strategy              | Description                                                                |
| --------------------- | -------------------------------------------------------------------------- |
| `REDACT`              | Replace the sensitive text with a placeholder.                             |
| `RANDOM_REPLACE`      | Replace the sensitive text with a similar, random value.                   |
| `STATIC_REPLACE`      | Replace the sensitive text with a given value.                             |
| `CRYPTO_REPLACE`      | Replace the sensitive text with its encrypted value.                       |
| `HASH_SHA256_REPLACE` | Replace the sensitive text with its SHA256 hash value.                     |
| `LAST_4`              | Replace the sensitive text with just the last four characters of the text. |

### Conditions

Each filter strategy may have one condition. See [Conditions](#conditions) for details.

| Conditional      | Description                                                              | Operators                          |
| ---------------- | ------------------------------------------------------------------------ | ---------------------------------- |
| `TOKEN`          | Compares the value of the sensitive text.                                | `==` , `!=`                        |
| `CONTEXT`        | Compares the filtering context.                                          | `==` , `!=`                        |
| `CONFIDENCE`     | Compares the confidence in the sensitive text against a threshold value. | `<` , `<=`, `>` , `>=`, `==`, `!=` |
| `CLASSIFICATION` | Compares the classification of the sensitive text.                       | `==` , `!=`                        |

## Example Policy

```
{
  "name": "default",
  "identifiers": {
    "identifiers": [
      {
        "pattern": "[A-Z]{9}",
        "caseSensitive": false,
        "classification": "custom-identifier",
        "enabled": true,
        "identifierFilterStrategies": [
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
