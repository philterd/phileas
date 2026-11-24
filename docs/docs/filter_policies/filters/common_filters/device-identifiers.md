# Device Identifiers

## Filter

This filter identifies medical device identifiers and serial numbers, one of the identifiers listed by HIPAA Safe Harbor (45 CFR 164.514(b)(2)(i)(M)). Detected values are reported with the span type `device-identifier`. It detects three forms:

* **GS1 UDIs** in their human-readable form, with no cue word: `(01)` and a 14-digit GTIN whose GS1 check digit is valid, with any `(11)` or `(17)` date, `(10)` lot, and `(21)` serial that follow it, such as `(01)00812345000016(17)270128(21)SN0042`. The whole UDI is one value. A UDI whose GTIN fails the check digit is not detected.
* **HIBCC UDIs**, with no cue word: `+`, a four-character labeler code starting with a letter, the product number, a one-digit unit of measure, and a mod-43 check character, such as `+H123ABC01C`. A UDI whose check character is wrong is not detected.
* **Serial numbers** that follow a cue word. By default a serial number is 4 to 20 letters, digits, and hyphens, with at least one digit. The default cue words are `serial number`, `serial no.`, `serial #`, `S/N`, `SN`, and `device ID`.

ISBT 128 UDIs are not detected.

### Cue Words

A serial number is detected only when a cue word appears within `windowSize` tokens before it. A cue word after the value does not count. Cue words match case-insensitively as whole words, and a cue word of several words matches as a phrase. A cue word may be followed by punctuation, such as `Member ID:` or `Member #`, and punctuation at the end of a cue word is optional in the text, so the cue word `serial no.` also matches `Serial No:`. The cue word itself is not part of the detected value. UDIs do not need a cue word and do not give way to other filters.

### Overlaps

A serial number detected by a cue word gives way to an overlapping value detected by the [MBI](mbi.md), [SSN](ssns-and-tins.md), [NPI](npi.md), [phone number](phone-numbers.md), or [date](dates.md) filter, whatever the filters' `priority`. A value of one of those filters that is left unchanged, for example because no strategy's condition is satisfied, does not take priority.

Where a value found by this filter's cue word overlaps a value found by another cue word filter, such as the [health plan member ID](health-plan-member-ids.md) filter, the value takes the type of the filter whose cue word is nearest before it, whatever the filters' `priority`. So in `Member ID ABC12345 Serial No: SN-998877`, `ABC12345` is a member ID and `SN-998877` is a device identifier. A cue word otherwise reaches past other values within `windowSize` tokens, so in `Member ID: 778899 and 112233` both values are member IDs. Only the cue words of filters in the policy count: without the health plan member ID filter, `ABC12345` in `Serial No: SN-998877 Member ID ABC12345` is a device identifier. As above, a value that is left unchanged does not take priority.

### Required Parameters

This filter has no required parameters.

### Optional Parameters

| Parameter                          | Description                                                                                                                                                                 | Default Value                                            |
|------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `deviceIdentifierFilterStrategies` | A list of filter strategies.                                                                                                                                                | None                                                     |
| `patterns`             | A list of Java regular expressions for the value. When set, they replace the default serial number pattern (4 to 20 letters, digits, and hyphens, with at least one digit). Each pattern is checked when the policy is loaded and has the same match timeout as a [custom identifier's](../custom_filters/identifier.md) pattern. The UDI checks always apply. | The default pattern |
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

```
{
   "identifiers": {
      "deviceIdentifier": {
         "cueWords": [
            "implant ID"
         ],
         "deviceIdentifierFilterStrategies": [
            {
               "strategy": "REDACT"
            }
         ]
      }
   }
}
```

The `deviceIdentifier` filter requires redaction policy schema 1.3.0 as bundled with PhiSQL 1.7.0 or later.
