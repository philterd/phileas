# Dictionary

## Filter

This filter identifies custom text based on a given dictionary.

### Required Parameters

At least one of `terms` or `files` must be provided.

| Parameter | Description                                    | Default Value |
|-----------|------------------------------------------------|---------------|
| `terms`   | A list of terms in the dictionary. Terms are plain text, not regular expressions, and the words of a multi-word term may be separated in the text by any whitespace, such as a line break or tab. | None          |
| `files`   | A list of files containing terms one per line. Subject to the `policy.files` [settings](../../../settings.md). | None          |

### Optional Parameters

| Parameter        | Description                                                                                                                                                                                                                      | Default Value                                            |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `enabled`        | When set to false, the filter will be disabled and not applied                                                                                                                                                                   | `true`                                                   |
| `ignored`        | A list of terms to be ignored by the filter.                                                                                                                                                                                     | None                                                     |
| `fuzzy` | When `true`, a word that is close to a term, such as a misspelling, is also reported, as set by `sensitivity` and `capitalized`. When `false`, only exact matches are reported, ignoring case, and `sensitivity` and `capitalized` have no effect. | `false` |
| `capitalized` | When `true`, a match is reported only when it begins with a capital letter. Only applies when `fuzzy` is `true`. | `false` |
| `sensitivity` | How close a word must be to a term to be reported when `fuzzy` is `true`, measured in edits (a character inserted, removed, or changed). `high` reports only exact matches, `medium` allows one edit, and `low` allows up to two. `off` reports only exact matches. `auto` allows no edits for a word under five characters, one edit for a word under nine, and two for a longer word, counts two swapped neighboring letters as one edit (so `Smiht` is one edit from `Smith`), and reports a fuzzy match only when it begins with a capital letter, which helps keep common short words such as `and` and `the` from matching names and places. Words shorter than three characters, not counting punctuation around them, are not compared. Any other value refuses the policy when it is loaded, even when `fuzzy` is `false`. | `off` |
| `classification` | Used to apply an arbitrary label to the identifier, such as "patient-id", or "account-number."                                                                                                                                   | `"custom-identifier"`                                    |
| `windowSize`     | Sets the size of the window (in terms) surrounding a span to look for contextual terms. If set, this value overrides the value of `span.window.size` in the configuration.                                                       | The value of `span.window.size` which is by default `5`. |
| `priority`       | The priority (integer) of this filter. Valid values are any positive integer, where a higher value indicates a higher priority. Priority is used for tie-breaking when two spans may be otherwise identical.                     | `0`                                                      |

### Filter Strategies

The filter may have zero or more filter strategies. When no filter strategy is given the default strategy of `REDACT` is
used. When multiple filter strategies are given the filter strategies will be applied in as they are listed.
See [Filter Strategies ](../../filter_strategies.md)for details.

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
      "dictionaries": [
         {
            "terms": ["john", "jane", "doe"],
            "files": ["/tmp/dictionary.txt"],
            "fuzzy": true,
            "sensitivity": "medium",
            "customFilterStrategies": [
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
