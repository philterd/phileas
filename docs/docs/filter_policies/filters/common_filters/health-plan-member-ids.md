# Health Plan Member IDs

## Filter

This filter identifies health plan beneficiary and member IDs, one of the identifiers listed by HIPAA Safe Harbor (45 CFR 164.514(b)(2)(i)(I)). Member IDs have no common format, so a value is detected only when it follows a cue word such as `Member ID`. Detected values are reported with the span type `health-plan-member-id`. Medicare Beneficiary Identifiers have their own [MBI filter](mbi.md).

By default a value is 6 to 20 letters, digits, and hyphens, with at least one digit. Use `patterns` to match your plans' formats instead.

The default cue words are `member ID`, `member #`, `member number`, `subscriber ID`, `subscriber #`, `subscriber number`, `beneficiary ID`, `Medicaid ID`, and `insurance ID`.

### Cue Words

A value is detected only when a cue word appears within `windowSize` tokens before it. A cue word after the value does not count. Cue words match case-insensitively as whole words, and a cue word of several words matches as a phrase. A cue word may be followed by punctuation, such as `Member ID:` or `Member #`, and punctuation at the end of a cue word is optional in the text, so the cue word `serial no.` also matches `Serial No:`. The cue word itself is not part of the detected value.

### Overlaps

A value detected by a cue word gives way to an overlapping value detected by the [MBI](mbi.md), [SSN](ssns-and-tins.md), [NPI](npi.md), [phone number](phone-numbers.md), or [date](dates.md) filter, whatever the filters' `priority`. A value of one of those filters that is left unchanged, for example because no strategy's condition is satisfied, does not take priority.

### Required Parameters

This filter has no required parameters.

### Optional Parameters

| Parameter                            | Description                                                                                                                                                                 | Default Value                                            |
|--------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `healthPlanMemberIdFilterStrategies` | A list of filter strategies.                                                                                                                                                | None                                                     |
| `patterns`             | A list of Java regular expressions for the value. When set, they replace the default pattern (6 to 20 letters, digits, and hyphens, with at least one digit). Each pattern is checked when the policy is loaded and has the same match timeout as a [custom identifier's](../custom_filters/identifier.md) pattern. | The default pattern |
| `cueWords`             | A list of cue words. They are added to the default cue words unless `defaultCueWords` is `false`. | None |
| `defaultCueWords`      | Whether the default cue words are used along with `cueWords`. `false` requires at least one entry in `cueWords`; a policy that sets it to `false` with no cue words is refused when it is loaded. | `true` |
| `enabled`              | When set to false, the filter will be disabled and not applied                                                                                                              | `true`                                                   |
| `ignored`              | A list of terms to be ignored by the filter.                                                                                                                                | None                                                     |
| `windowSize`           | The number of tokens before a value in which a cue word must appear. If set, this value overrides the value of `span.window.size` in the configuration. | The value of `span.window.size` which is by default `5`. |
| `priority`             | The priority (integer) of this filter. Valid values are any positive integer, where a higher value indicates a higher priority. Priority is used for tie-breaking when two spans may be otherwise identical. It does not override the precedence of the filters listed under [Overlaps](#overlaps). | `0`                                                      |

### Filter Strategies

The filter may have zero or more filter strategies. When no filter strategy is given the default strategy of `REDACT` is
used. When multiple filter strategies are given the filter strategies will be applied in order as they are listed.
See [Filter Strategies](#filter-strategies) for details.

| Strategy              | Description                                                                                                         |
|-----------------------|---------------------------------------------------------------------------------------------------------------------|
| `REDACT`              | Replace the sensitive text with a placeholder.                                                                      |
| `MASK`                | Replace each character of the sensitive text with a mask character (`*` by default).                                |
| `TRUNCATE`            | Replace all but a few characters of the sensitive text with a truncation character (`*` by default).                |
| `RANDOM_REPLACE`      | Replace the sensitive text with a similar, random value.                                                          |
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

This policy detects member IDs in two plan formats after the default cue words or `Plan ID`:

```
{
   "identifiers": {
      "healthPlanMemberId": {
         "patterns": [
            "\\b[A-Z]{3}[0-9]{9}\\b",
            "\\b[A-Z][0-9]{8}-[0-9]{2}\\b"
         ],
         "cueWords": [
            "Plan ID"
         ],
         "healthPlanMemberIdFilterStrategies": [
            {
               "strategy": "REDACT"
            }
         ]
      }
   }
}
```

The `healthPlanMemberId` filter requires redaction policy schema 1.3.0 as bundled with PhiSQL 1.7.0 or later.
