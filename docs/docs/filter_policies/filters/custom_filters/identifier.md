# Identifier

## Filter

This filter identifies custom text based on a given regular expression.&#x20;

The Identifier filter accepts a list of regular expression-based identifiers. See the policy at the bottom of this page
for an example.&#x20;

_Note that backslashes in the regular expression will need to be escaped for the policy to be valid JSON._

> Because the `pattern` is a user-supplied regular expression, each match attempt is time-bounded to guard against
> catastrophic backtracking (ReDoS). If a pattern exceeds the budget on a given input, matching is aborted and that
> input yields no matches for the identifier. The budget is controlled by the `regex.timeout.ms`
> [setting](../../../settings.md#advanced-settings) (default `1000` ms).

### Required Parameters

This filter has no required parameters.

### Optional Parameters

| Parameter        | Description                                                                                                                                                                                                  | Default Value                                            |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `enabled`        | When set to false, the filter will be disabled and not applied                                                                                                                                               | `true`                                                   |
| `ignored`        | A list of terms to be ignored by the filter.                                                                                                                                                                 | None                                                     |
| `caseSensitive`  | When set to true, the regular expression will be case sensitive.                                                                                                                                             | `true`                                                   |
| `classification` | Used to apply an arbitrary label to the identifier, such as "patient-id", or "account-number."                                                                                                               | `"custom-identifier"`                                    |
| `pattern`        | A regular expression for the identifier. _Note that backslashes will need to be escaped._                                                                                                                    | `\b[A-Z0-9_-]{4,}\b`                                     |
| `groupNumber`    | The regular expression capture group to extract as the identifier (`0` is the entire match). Use a capture group when you want to match on surrounding context but redact only part of the match.             | `0`                                                      |
| `validator`      | An optional named, post-match validator. A match is kept only if the validator passes, so a generic identifier can reject format-valid but invalid values (for example a checksum-invalid number). See [Validators](#validators) below. | None                                                     |
| `windowSize`     | Sets the size of the window (in terms) surrounding a span to look for contextual terms. If set, this value overrides the value of `span.window.size` in the configuration.                                   | The value of `span.window.size` which is by default `5`. |
| `priority`       | The priority (integer) of this filter. Valid values are any positive integer, where a higher value indicates a higher priority. Priority is used for tie-breaking when two spans may be otherwise identical. | `0`                                                      |

### Filter Strategies

The filter may have zero or more filter strategies. When no filter strategy is given the default strategy of `REDACT` is
used. When multiple filter strategies are given the filter strategies will be applied in as they are listed.
See [Filter Strategies ](../../filter_strategies.md)for details.

| Strategy              | Description                                                                |
|-----------------------|----------------------------------------------------------------------------|
| `REDACT`              | Replace the sensitive text with a placeholder.                             |
| `MASK`                | Replace each character of the sensitive text with a mask character (`*` by default).                                |
| `TRUNCATE`            | Replace all but a few characters of the sensitive text with a truncation character (`*` by default).                |
| `RANDOM_REPLACE`      | Replace the sensitive text with a similar, random value.                   |
| `STATIC_REPLACE`      | Replace the sensitive text with a given value.                             |
| `CRYPTO_REPLACE`      | Replace the sensitive text with its encrypted value.                       |
| `HASH_SHA256_REPLACE` | Replace the sensitive text with its SHA256 hash value.                     |
| `FPE_ENCRYPT_REPLACE` | Replace the sensitive text with a value generated by [format-preserving encryption](../../filter_strategies.md#fpe) (FPE) |
| `LAST_4`              | Replace the sensitive text with just the last four characters of the text. |

### Validators

A regular expression matches a _format_, but it cannot tell a valid identifier from a value that
merely has the same shape. The optional `validator` runs a named, built-in check on each match and
keeps the match only if the check passes. This lets a generic identifier reject format-valid but
invalid values without a dedicated filter and without embedding any executable code in the policy.

The validator may be written in either of two forms:

```
"validator": "luhn"
```

```
"validator": { "name": "luhn", "params": { } }
```

The object form exists for validators that take parameters; `luhn` takes none, so the two forms are
equivalent for it. The validator name must be one defined by the
[redaction policy schema](https://philterd.ai/schemas/redaction-policy/1.1.0/schema.json). An unknown
name, or a name the current build does not implement, is a policy error and the policy will fail to
load. A validator is never silently skipped.

| Validator | Description                                                                                       |
|-----------|---------------------------------------------------------------------------------------------------|
| `luhn`    | Standard mod-10 Luhn checksum over the digits of the match. Separators (spaces, hyphens) are ignored, so a value may be formatted or unformatted. Used by identifiers such as the Canadian SIN, French SIREN, and SIRET. |

> The `luhn` validator implements the standard Luhn algorithm only. La Poste SIRETs are a known
> exception (they are validated by a digit-sum mod 5 rather than Luhn) and will not pass this check.

For example, with `"validator": "luhn"` a nine-digit pattern keeps `046 454 286` (Luhn-valid) but
drops `123 456 789` (same shape, fails the checksum). Without the validator, both would be redacted.

### Conditions

Each filter strategy may have one condition. See [Conditions](#conditions) for details.

| Conditional      | Description                                                              | Operators                          |
|------------------|--------------------------------------------------------------------------|------------------------------------|
| `TOKEN`          | Compares the value of the sensitive text.                                | `==` , `!=`                        |
| `CONTEXT`        | Compares the filtering context.                                          | `==` , `!=`                        |
| `CONFIDENCE`     | Compares the confidence in the sensitive text against a threshold value. | `<` , `<=`, `>` , `>=`, `==`, `!=` |

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

## Example Policy with a Validator

This identifier matches a nine-digit Canadian SIN, formatted or unformatted, and keeps only the
matches that pass the Luhn checksum.

```
{
  "name": "default",
  "identifiers": {
    "identifiers": [
      {
        "pattern": "\\b\\d{3}[ -]?\\d{3}[ -]?\\d{3}\\b",
        "caseSensitive": false,
        "classification": "canada-sin",
        "validator": "luhn",
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
