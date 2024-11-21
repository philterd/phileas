# Credit Cards

## Filter

This filter identifies credit cards such as `378282246310005` in text.

### Required Parameters

This filter has no required parameters.

### Optional Parameters

| Parameter                    | Description                                                                                                                                                                                                                                                                                                                                                                                                 | Default Value |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| `creditCardFilterStrategies` | A list of filter strategies.                                                                                                                                                                                                                                                                                                                                                                                | None          |
| `enabled`                    | When set to false, the filter will be disabled and not applied                                                                                                                                                                                                                                                                                                                                              | `true`        |
| `ignored`                    | A list of terms to be ignored by the filter.                                                                                                                                                                                                                                                                                                                                                                | None          |
| `onlyValidCreditCardNumbers` | When set to true, only credit card numbers satisfying a [Luhn](https://en.wikipedia.org/wiki/Luhn_algorithm) check will be filtered.                                                                                                                                                                                                                                                                        | `true`        |
| `ignoreWhenInUnixTimestamp`  | When set to true, only credit card numbers that do not match the pattern for a Unix timestamp will be filtered.                                                                                                                                                                                                                                                                                             | `false`       |
| `onlyWordBoundaries`          | When set to true, only credit card numbers at [regex word boundaries](https://www.regular-expressions.info/wordboundaries.html) are considered. If `false` then preceding and succeeding characters will be ignored. This can have performance impacts on large documents with many large numbers in them. The `onlyValidCreditCardNumbers` option must be set to `true` for this parameter to have effect. | `true`        |

### Filter Strategies

The filter may have zero or more filter strategies. When no filter strategy is given the default strategy of `REDACT` is
used. When multiple filter strategies are given the filter strategies will be applied in order as they are listed.
See [Filter Strategies](#filter-strategies) for details.

| Strategy              | Description                                                                                                         |
|-----------------------|---------------------------------------------------------------------------------------------------------------------|
| `REDACT`              | Replace the sensitive text with a placeholder.                                                                      |
| `RANDOM_REPLACE`      | Replace the sensitive text with a similar, random value.                                                            |
| `STATIC_REPLACE`      | Replace the sensitive text with a given value.                                                                      |
| `CRYPTO_REPLACE`      | Replace the sensitive text with its encrypted value.                                                                |
| `HASH_SHA256_REPLACE` | Replace the sensitive text with its SHA256 hash value.                                                              |
| `FPE_ENCRYPT_REPLACE` | Replace the sensitive text with a value generated by [format-preserving encryption](filter-strategies.md#fpe) (FPE) |
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
   "name": "credit-cards-example",
   "identifiers": {
      "creditcard": {
         "onlyValidCreditCardNumbers": false,
         "onlyWordBoundaries": false,
         "creditCardFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```
