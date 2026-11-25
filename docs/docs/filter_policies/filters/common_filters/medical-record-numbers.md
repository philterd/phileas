# Medical Record Numbers

## Filter

This filter identifies medical record numbers, one of the identifiers listed by HIPAA Safe Harbor (45 CFR 164.514(b)(2)(i)(H)). Medical record number formats differ by hospital, so a value is detected only when it follows a cue word such as `MRN`. Detected values are reported with the span type `medical-record-number`.

By default a value is 6 to 12 letters, digits, and hyphens, with at least one digit. Use `patterns` to match your systems' formats instead.

The default cue words are `MRN`, `MR #`, `med rec #`, `medical record #`, `medical record no.`, `medical record number`, `chart #`, and `chart number`. `patient ID` is not a default cue word, because some systems use it for other identifiers; add it with `cueWords` if your records use it for medical record numbers.

### Cue Words

A value is detected only when a cue word appears within `windowSize` tokens before it. A cue word after the value does not count. Cue words match case-insensitively as whole words, and a cue word of several words matches as a phrase. A `#` may follow a word with no space, so the cue word `MR #` also matches `MR#`. A cue word may be followed by punctuation, such as `MRN:`, and punctuation at the end of a cue word is optional in the text, so the cue word `medical record no.` also matches `Medical Record No:`. The cue word itself is not part of the detected value.

### Overlaps

A value detected by a cue word gives way to an overlapping value detected by the [MBI](mbi.md), [SSN](ssns-and-tins.md), [NPI](npi.md), [phone number](phone-numbers.md), or [date](dates.md) filter, whatever the filters' `priority`. A value of one of those filters that is left unchanged, for example because no strategy's condition is satisfied, does not take priority.

Where a value found by this filter's cue word overlaps a value found by another cue word filter, such as the [health plan member ID](health-plan-member-ids.md) filter, the value takes the type of the filter whose cue word is nearest before it, whatever the filters' `priority`. So in `Member ID ABC12345 MRN: 00482913`, `ABC12345` is a member ID and `00482913` is a medical record number. Only the cue words of filters in the policy count. As above, a value that is left unchanged does not take priority.

### Required Parameters

This filter has no required parameters.

### Optional Parameters

| Parameter                            | Description                                                                                                                                                                 | Default Value                                            |
|--------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `medicalRecordNumberFilterStrategies` | A list of filter strategies.                                                                                                                                                | None                                                     |
| `patterns`             | A list of Java regular expressions for the value. When set, they replace the default pattern (6 to 12 letters, digits, and hyphens, with at least one digit). Each pattern is checked when the policy is loaded and has the same match timeout as a [custom identifier's](../custom_filters/identifier.md) pattern. | The default pattern |
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

This policy detects medical record numbers in a hospital's two formats after the default cue words or `patient ID`:

```
{
   "identifiers": {
      "medicalRecordNumber": {
         "patterns": [
            "\\b[0-9]{8}\\b",
            "\\bMR-[0-9]{6}\\b"
         ],
         "cueWords": [
            "patient ID"
         ],
         "medicalRecordNumberFilterStrategies": [
            {
               "strategy": "REDACT"
            }
         ]
      }
   }
}
```

The `medicalRecordNumber` filter requires redaction policy schema 1.3.0 as bundled with PhiSQL 1.7.0 or later.
