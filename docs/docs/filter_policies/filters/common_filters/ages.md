# Ages

## Filter

This filter identifies ages such as `3.5 years old` in text. Ages written as spelled-out numbers are
also identified, for example `thirty-five years old`, `thirty-five-year-old`, and `aged forty-two`.

Ages labeled with an `age` or `aged` keyword are identified whether or not a separator follows the
keyword. A colon, an equals sign, or a hyphen is accepted, with or without surrounding whitespace, so
`Age: 47`, `Age:47`, `Age = 47`, `Age - 47`, and `Age 47` are all identified, as is the spelled-out
form (`Age: thirty-five`). The whitespace around a separator may include a line break, so `Age:` on
one line with its value on the next is also identified. The keyword may also be followed by `of`, so
`at the age of 39` and `age of thirty-nine` are identified, with the span covering `age of 39`.

A run or range of ages written with digits after a keyword is identified as one span, so none of its ages is left
in the text:

* After `age` or `age of`: one age, or a range joined by a hyphen or `to`, as in `age 18-65` and `age 39 to 41`. A
  comma or `and` does not continue it, so in `age 45, 3 visits` only `age 45` is identified.
* After `aged`: ages joined by a hyphen, `to`, `and`, or `or`, as in `aged 39-41`, `aged 39 and 41`, and
  `aged 3, 5 and 7`. A comma joins ages only inside a list that a word or a hyphen closes, so in
  `aged 39, 2 children` only `aged 39` is identified.
* After the plural `ages` or `ages of`: ages joined by commas, words, or hyphens, as in `Ages: 39, 41`,
  `ages 39 to 41`, and `the ages of 39, 41, and 44`.
* After `age`, `aged`, or `ages` followed by `between` or `from`, with or without a separator: ages joined as after
  `aged`, as in `age between 18 and 65`, `Age: between 18 and 65`, `aged from 39 to 41`, and
  `Ages - from 18 to 65`.

Each age after the first in a run or range is identified only up to 125, and an age after a keyword does not end
right before a hyphen or a slash followed by a digit, so a date such as `age 25-03-2024`, `aged 12-25-2020`, or
`Age: 12/25/2020` is not identified, in whole or in part, as an age. The limit does not apply to an age that a month, week, or
day unit follows, so `aged 6 to 130 months` is identified whole.

An age written with a unit (`years`, `yrs`, `yr`, `yo`, or `y/o`) is identified only up to 125, well above
the longest recorded lifespan, so a year or the age of an object, such as `the 2019 years old` or
`the bridge is 150 years old`, is not identified as an age. An age after an `age` or `aged` keyword is not
bounded this way, since a unit after it, as in `aged 1100 months`, can make a large number a real age. A
unit that begins another word, as `yo` does in `yo-yo`, is not a unit. The identified text ends at the
unit, or at `old` after it, so punctuation that follows, such as the comma in `47 yo, female`, is not
part of it.

### Required Parameters

This filter has no required parameters.

### Optional Parameters

| Parameter             | Description                                                                                                                                                                | Default Value                                            |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `ageFilterStrategies` | A list of filter strategies.                                                                                                                                               | None                                                     |
| `enabled`             | When set to false, the filter will be disabled and not applied                                                                                                             | `true`                                                   |
| `ignored`             | A list of terms to be ignored by the filter.                                                                                                                               | None                                                     |
| `windowSize`          | Sets the size of the window (in terms) surrounding a span to look for contextual terms. If set, this value overrides the value of `span.window.size` in the configuration. | The value of `span.window.size` which is by default `5`. |
| `priority`            | The priority (integer) of this filter. Valid values are any positive integer, where a higher value indicates a higher priority. Priority is used for tie-breaking when two spans may be otherwise identical. | `0`           |

### Filter Strategies

The filter may have zero or more filter strategies. When no filter strategy is given the default strategy of `REDACT` is
used. When multiple filter strategies are given the filter strategies will be applied in order as they are listed.
See [Filter Strategies](#filter-strategies) for details.

| Strategy              | Description                                              |
|-----------------------|----------------------------------------------------------|
| `REDACT`              | Replace the sensitive text with a placeholder.           |
| `MASK`                | Replace each character of the sensitive text with a mask character (`*` by default).                                |
| `TRUNCATE`            | Replace all but a few characters of the sensitive text with a truncation character (`*` by default).                |
| `RANDOM_REPLACE`      | Replace the sensitive text with a similar, random value. |
| `STATIC_REPLACE`      | Replace the sensitive text with a given value.           |
| `CRYPTO_REPLACE`      | Replace the sensitive text with its encrypted value.     |
| `HASH_SHA256_REPLACE` | Replace the sensitive text with its SHA256 hash value.   |
| `FPE_ENCRYPT_REPLACE` | Replace the sensitive text with a value generated by [format-preserving encryption](../../filter_strategies.md#fpe) (FPE) |
| `LAST_4`              | Replace the sensitive text with just the last four characters of the text.                                          |

### Conditions

Each filter strategy may have one condition. The filter will only be applied when the condition is satisfied.
See [Conditions](#conditions) for details.

| Conditional  | Description                                                              | Operators                          |
|--------------|--------------------------------------------------------------------------|------------------------------------|
| `TOKEN`      | Compares the value of the sensitive text.                                | `==`, `!=`, `startswith`           |
| `CONTEXT`    | Compares the filtering context.                                          | `==` , `!=`                        |
| `CONFIDENCE` | Compares the confidence in the sensitive text against a threshold value. | `<` , `<=`, `>` , `>=`, `==`, `!=` |
| `TOKEN` with a number | Compares the age in years with a number, as in `token > 89`. See [Comparing the Age](#comparing-the-age). | `<` , `<=`, `>` , `>=`, `==`, `!=` |

### Comparing the Age

A condition compares the age with a number when the number is written without quotes, as in `token > 89`. Only the
age filter accepts a number for `token`; on any other filter such a condition refuses the policy when it is loaded. A
quoted value, as in `token == "47 years old"`, is still compared with the identified text.

The age compared is the age in years that the identified text gives:

* The number written with digits is used, so `47 years old`, `47-year-old`, `47yo`, `61 y/o`, `Age: 47`, `aged 47`,
  and `age of 47` each compare as 47. A decimal is kept: `3.5 years old` satisfies `token > 3` but not `token == 3`.
* A spelled-out age compares as its number, so `thirty-five years old` compares as 35 and
  `one hundred and two years old` as 102.
* An age after an `age` keyword ends at the number, so in `aged 18 months` the identified text is `aged 18`. When the
  word after it is `months`, `month`, `mos`, `mo`, `mths`, or `mth`, the number is read as months, each a twelfth of a
  year; `weeks`, `week`, `wks`, or `wk` as weeks of 7 days; and `days` or `day` as days, with a year of 365 days. So
  `aged 18 months` compares as 1.5. The word is read from the span's window, so with `span.window.size` set to `0` in
  the configuration these ages compare as years. A filter's `windowSize` of `0` means the configured size, not an empty
  window. The filter does not identify an age written with months, weeks, or days and no keyword, such as
  `18 months old`.
* A run or range of ages, such as `aged 39 and 95` or `ages 18-65`, satisfies a comparison when any of its ages does,
  and a unit after it applies to each of them. So `aged 39 and 95` satisfies `token > 89`, and the whole span is
  replaced, rather than the 95 being left in the text because the first age is 39.
* An identified age whose number cannot be read satisfies no comparison, so its strategy is skipped.

HIPAA Safe Harbor (45 CFR 164.514(b)(2)(i)(C)) requires ages over 89 to be aggregated into a single category of 90 or
older. This policy replaces those ages with `90 or older`. Because its only strategy has a condition, younger ages are
left in the text:

```
{
   "identifiers": {
      "age": {
         "ageFilterStrategies": [
            {
               "condition": "token > 89",
               "strategy": "STATIC_REPLACE",
               "staticReplacement": "90 or older"
            }
         ]
      }
   }
}
```

With this policy, `The patient is 93 years old.` becomes `The patient is 90 or older.`, and
`The patient is 72 years old.` is unchanged. The replacement takes the place of the whole identified text, including an
`age` keyword, so `Age: 93` becomes `90 or older`. To redact the younger ages instead, add a strategy with no condition
after this one.

## Example Policy

```
{
   "identifiers": {
      "age": {
         "ageFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```
