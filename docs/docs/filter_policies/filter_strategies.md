# Filter Strategies

A filter strategy defines how sensitive information identified by Phileas should be manipulated, whether it is redacted, replaced, encrypted, or manipulated in some other fashion.

In a policy, you list the types of sensitive information that should be filtered. How Phileas replaces each type of sensitive information is specific to each type. For instance, zip codes can be truncated based on the leading digits or zip code population while phone numbers are redacted. These replacements are performed by "filter strategies."

> Each filter can have one or more filter strategies and conditions can be used to determine when to apply each filter strategy.


A sample policy containing a filter strategy is shown below. In this example, email addresses will be redacted.

```
{
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```

> Most of the filter strategies apply to all types of data, however, some filter strategies only apply to a few types. For example, the `ZERO_LEADING` filter strategy only applies to the zip code filter.


## Filter Strategies

The filter strategies are described below. Each filter type can specify zero or more filter strategies. When no filter strategies are given, Phileas will default to `REDACT` for that filter type. When multiple filter strategies are given for a single filter type, the filter strategies will be applied in order as they are listed in the policy, top to bottom.

* [`REDACT`](filter_strategies.md#the-redact-filter-strategy)
* [`MASK`](filter_strategies.md#mask)
* [`CRYPTO_REPLACE`](filter_strategies.md#crypto)(AES-GCM authenticated encryption)
* [`HASH_SHA256_REPLACE`](filter_strategies.md#hash)(SHA512 encryption)
* [`FPE_ENCRYPT_REPLACE`](filter_strategies.md#fpe)(Format-preserving encryption)
* [`RANDOM_REPLACE`](filter_strategies.md#random)
* [`STATIC_REPLACE`](filter_strategies.md#static)
* [`MAP_REPLACE`](filter_strategies.md#map)
* [`TRUNCATE`](filter_strategies.md#truncate)
* [`ZERO_LEADING`](filter_strategies.md#zero_leading)
* [`ABBREVIATE`](filter_strategies.md#abbreviate)

### Which Filters Accept Which Strategies

Most filters accept the same set of strategies. The date, ZIP code, and Ph-Eye filters differ: each
adds strategies of its own and leaves out some of the common ones.

| Filter | Accepted strategies |
| ------ | ------------------- |
| Every filter except those below | `REDACT`, `RANDOM_REPLACE`, `STATIC_REPLACE`, `MAP_REPLACE`, `CRYPTO_REPLACE`, `FPE_ENCRYPT_REPLACE`, `HASH_SHA256_REPLACE`, `LAST_4`, `MASK`, `TRUNCATE`, `ABBREVIATE` |
| `date` | `REDACT`, `RANDOM_REPLACE`, `STATIC_REPLACE`, `CRYPTO_REPLACE`, `HASH_SHA256_REPLACE`, `MASK`, `TRUNCATE`, `TRUNCATE_TO_YEAR`, `SHIFT`, `RELATIVE` |
| `zipCode` | `REDACT`, `RANDOM_REPLACE`, `STATIC_REPLACE`, `CRYPTO_REPLACE`, `HASH_SHA256_REPLACE`, `MASK`, `TRUNCATE`, `ZERO_LEADING` |
| `pheyes` | `REDACT`, `RANDOM_REPLACE`, `STATIC_REPLACE`, `CRYPTO_REPLACE`, `FPE_ENCRYPT_REPLACE`, `HASH_SHA256_REPLACE`, `MASK`, `TRUNCATE`, `ABBREVIATE` |

Strategy names are not case-sensitive.

A strategy a filter does not accept is not an error: the filter falls back to `REDACT`, so a policy
whose strategy name is misspelled still removes the sensitive value rather than leaving it in the
clear. Phileas logs a warning at `WARN` when it loads such a policy, naming the strategy and the
filter, because the fallback is otherwise invisible — a misspelled `CRYPTO_REPLACE` or
`FPE_ENCRYPT_REPLACE` produces irreversible redaction, and re-identification then fails against data
that can no longer be recovered.

### The `REDACT` Filter Strategy

The REDACT filter strategy replaces sensitive information with a given redaction format. You can put variables in the redaction format that Phileas will replace when performing the redaction.

The available redaction variables are:

| Redaction Variable | Description                                                                                                                                               |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `%t`               | Will be replaced with the type of sensitive information. This is to allow you to know the type of sensitive information that was identified and redacted. |
| `%l`               | Will be replaced by the given classification for the type of sensitive information.                                                                       |
| `%v`               | Will be replaced by the original value of the sensitive text. With `%v` you can annotate sensitive information instead of masking or removing it.         |

To redact sensitive information by replacing it with the type of sensitive information, the redaction format would be `REDACTED-%t`.

An example filter using the `REDACT` filter strategy:

```
{
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```

### The `CRYPTO_REPLACE` Filter Strategy {id="crypto"}

The `CRYPTO_REPLACE` filter strategy replaces each identified piece of sensitive information with its encrypted value, so an authorized party holding the key can recover the original later. Phileas uses **AES in GCM mode** (authenticated encryption). To use this filter strategy, the policy must include the encryption `key`:

```
{
   "crypto": {
     "key": "...."
   },
   ...
```

The `key` is a hex-encoded AES key (for example, 64 hex characters for a 256-bit key). You can generate one with:

```
openssl rand -hex 32
```

> **Keep the key out of the policy file.** Because policies are configuration files that are often kept in version control, prefix the value with `env:` to read the key from an environment variable at runtime instead of storing it inline. For example, `"key": "env:CRYPTO_KEY"` reads the key from the `CRYPTO_KEY` environment variable. This is the recommended way to supply the key.

> A fresh random nonce is generated for every value, so encrypting the same value twice produces different output (identical values do not produce identical redactions across the corpus), and each value carries an authentication tag that detects tampering. Because the nonce is random, no initialization vector (`iv`) is required; an `iv` in an existing policy is ignored.

The encrypted replacement has the form `{{<base64>}}`, where the Base64 content is `nonce || ciphertext || tag` (a 12-byte nonce, the ciphertext, and a 16-byte authentication tag).

#### Decrypting a value

Because GCM is an authenticated mode, a value cannot be decrypted with the `openssl enc` command line, which does not support GCM. Decrypt it with a GCM-capable library instead, for example with Python:

```
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
import base64

key  = bytes.fromhex("<the hex key from the policy>")
blob = base64.b64decode("<the base64 inside the {{ }}>")
nonce, ciphertext_and_tag = blob[:12], blob[12:]
plaintext = AESGCM(key).decrypt(nonce, ciphertext_and_tag, None)
print(plaintext.decode())
```

An example policy using the `CRYPTO_REPLACE` filter strategy:

```
{
   "crypto": {
     "key": "...."
   },
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "CRYPTO_REPLACE"
            }
         ]
      }
   }
}
```

### The `HASH_SHA256_REPLACE` Filter Strategy {id="hash"}

The `HASH_SHA256_REPLACE` filter strategy replaces sensitive information with the SHA256 hash value of the sensitive information. To append a random salt value to each value prior to hashing, set the `salt` property to `true`. The salt value used is returned in the redaction explanation that Phileas produces.

An example policy using the `HASH_SHA256_REPLACE` filter strategy:

```
{
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "HASH_SHA256_REPLACE"
            }
         ]
      }
   }
}
```

### The FPE\_ENCRYPT\_REPLACE Filter Strategy {id="fpe"}

The `FPE_ENCRYPT_REPLACE` filter strategy uses format-preserving encryption (FPE) to encrypt the sensitive information. Phileas uses the FF3-1 algorithm for format-preserving encryption. The FPE\_ENCRYPT\_REPLACE filter strategy requires a `key` and a `tweak` value. These values control the format-preserving encryption. For more information on these values and format-preserving encryption, refer to the resources below:

* [https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-38Gr1-draft.pdf](https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-38Gr1-draft.pdf)
* [https://nvlpubs.nist.gov/nistpubs/specialpublications/nist.sp.800-38g.pdf](https://nvlpubs.nist.gov/nistpubs/specialpublications/nist.sp.800-38g.pdf)

> **Keep these secrets out of the policy file.** As with the [crypto key](#crypto), prefix either value with `env:` to read it from an environment variable at runtime, for example `"key": "env:FPE_KEY"` and `"tweak": "env:FPE_TWEAK"`. This is the recommended way to supply the `key` and `tweak`.

> FF3 can only encrypt values whose format-preservable (alphanumeric) content is between 6 and 56 characters long. When a detected value falls outside this range (for example a 5-digit ZIP code) it cannot be format-preserving encrypted. In that case Phileas falls back to redacting that value (using the `REDACT` placeholder) instead, so the value is still redacted and a single out-of-range value does not affect the rest of the document. Consider applying `FPE_ENCRYPT_REPLACE` to types whose values fall within the supported length range.

> **Which filters support FPE.** Every filter supports the `FPE_ENCRYPT_REPLACE` strategy except the **date** and **zip code** filters, which do not offer it. (A ZIP code is shorter than the 6-character minimum noted above; date values use the date-specific strategies such as `SHIFT` instead.) Each filter's own documentation page lists the strategies it supports.

An example policy using the FPE\_ENCRYPT\_REPLACE filter strategy:

```
{
   "fpe": {
      "key": "...",
      "tweak": "..."
   },
   "identifiers": {
      "creditCard": {
         "creditCardFilterStrategies": [
            {
               "strategy": "FPE_ENCRYPT_REPLACE"
            }
         ]
      }
   }
}
```

### The `RANDOM_REPLACE` Filter Strategy {id="random"}

Replaces the identified text with one of the following:

* A fake value but of the same type. For example, an SSN will be replaced by a random text having the format `###-##-####`, such as 123-45-6789. An email address will be replaced with a randomly generated email address. Available to all filter types. Set `anonymizationMethod` to `REALISTIC`.
* A random value from a list of candidate replacement values. For example, an SSN could be replaced with a random value from a list of known fake SSNs. Available to all filter types. Set `anonymizationMethod` to `FROM_LIST`, and set `anonymizationCandidates` to a list of candidate replacement values.
* A random UUID. Set `anonymizationMethod` to `UUID`.

If `anonymizationMethod` is not specified, the default is `REALISTIC`. If an invalid value is given for `anonymizationMethod`, `UUID` will be used as the default.

With `REALISTIC`, the replacement depends on the filter type:

| Filter types | Replacement |
|---|---|
| `firstName`, `surname`, `city`, `county`, `state`, `hospital`, `stateAbbreviation` | A value from a list of names or places. First names and surnames follow the case of the identified text. |
| `creditCard` | A card number that passes the Luhn check, of the same card brand, length, and layout (spaces or hyphens) as the identified number. The leading digits that identify the brand (one to four, or just the first digit for a number of no known brand) are kept and the rest are random. |
| `bitcoinAddress` | An address of the same form as the identified address, with a valid checksum: a legacy address beginning with the same `1` or `3`, or a segregated witness address beginning with the same `bc1q` or `bc1p` and of the same length. Its hash or witness program is random. |
| `vin` | 17 characters from the VIN character set with a valid check digit in position 9. |
| `emailAddress`, `url`, `date`, `ipAddress`, `macAddress`, `streetAddress`, `zipCode`, `age`, `currency` | A generated value of that type. A date keeps the format of the identified date when it is one of the common formats the date filter detects, and is otherwise written as `yyyy-MM-dd`. An age or currency amount keeps its format with its digits replaced. |
| `ibanCode` | An IBAN with the same country code and spacing, the account number replaced character for character, and valid check digits. |
| `passportNumber` | A value with the same shape, with a US passport prefix where the identified number has one, so the passport filter detects it. |
| `phoneNumber` | The same format with each digit replaced, keeping parentheses, spaces, hyphens, dots, and a leading plus sign. |
| `itin`, `canadaSin` | A value of that type in the same format. An ITIN begins with 9 and has fourth and fifth digits in an IRS-issued range, and a SIN passes the Luhn check. |
| All other types, including `ssn`, `bankRoutingNumber`, `driversLicense`, `ein`, `phoneNumberExtension`, `trackingNumber`, `pheyes`, custom `identifiers`, custom `dictionaries`, and `sections` | A random value with the same shape: each digit is replaced with a digit and each letter with a letter of the same case, and other characters are kept. The value is not checked against the type's rules, so for example an SSN may begin with 9, and a bank routing number's checksum or a tracking number's carrier prefix may not be valid. |

If a value that differs from the identified text cannot be generated, a random UUID is used instead.

When `anonymizationCandidates` is a non-empty list, its values are used unless `anonymizationMethod` is `UUID`, so a policy that sets `anonymizationCandidates` without an `anonymizationMethod` draws from the list. A candidate equal to the identified text, ignoring case, is never chosen; each other entry in the list is equally likely, so a candidate listed twice is chosen twice as often. When `anonymizationMethod` is `FROM_LIST` and `anonymizationCandidates` is empty or not set, or when every candidate equals the identified text, a random UUID is used instead.

An example policy using the `REALISTIC` filter strategy:

```
{
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "RANDOM_REPLACE",
               "anonymizationMethod": "REALISTIC"
            }
         ]
      }
   }
}
```

An example policy using the `RANDOM_REPLACE` filter strategy with a list of candidate replacement values:

```
{
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "RANDOM_REPLACE",
               "anonymizationMethod": "FROM_LIST",
               "anonymizationCandidates": [
                  "jane.doe@example.com",
                  "john.roe@example.com",
                  "alex.poe@example.com"
               ]
            }
         ]
      }
   }
}
```

An example policy using the `RANDOM_REPLACE` filter strategy with a UUID:

```
{
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "RANDOM_REPLACE",
               "anonymizationMethod": "UUID"
            }
         ]
      }
   }
}
```

### The `STATIC_REPLACE` Filter Strategy {id="static"}

Replaces the identified text with a given static value. Available to all filter types.

An example policy using the `STATIC_REPLACE` filter strategy:

```
{
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "STATIC_REPLACE",
               "staticReplacement": "some new value"
            }
         ]
      }
   }
}
```

### The `MAP_REPLACE` Filter Strategy {id="map"}

Replaces a detected value with a replacement from a lookup table you provide. When the value is absent from the table, an optional generator produces a replacement, and a fallback strategy is applied when the table and generator do not yield one. A detected value is never left in the clear. Available to all filter types. See [Mapped Replacement](../deidentification/mapped-replacement.md) for full details, including generators.

An example policy using the `MAP_REPLACE` filter strategy:

```
{
   "identifiers": {
      "identifiers": [
         {
            "pattern": "Acme Corp|Globex",
            "identifierFilterStrategies": [
               {
                  "strategy": "MAP_REPLACE",
                  "mappings": { "Acme Corp": "Widget Co" },
                  "mappingFiles": [ "/etc/philter/vendors.tsv" ],
                  "caseSensitive": false,
                  "fallbackStrategy": "REDACT"
               }
            ]
         }
      ]
   }
}
```

### The `MASK` Filter Strategy {id="mask"}

This strategy replaces each character of the identified text with a mask character. By default the mask character
is `*` and the masked value is the same length as the original text. Set `maskCharacter` to change the character
used. Set `maskLength` to a number to force a fixed length, or leave it as the default `SAME` to preserve the
original length. Available to all filter types.

An example policy using the `MASK` filter strategy:

```
{
   "identifiers": {
      "creditCard": {
         "creditCardFilterStrategies": [
            {
               "strategy": "MASK",
               "maskCharacter": "#",
               "maskLength": "SAME"
            }
         ]
      }
   }
}
```

### The `TRUNCATE` Filter Strategy {id="truncate"}

This strategy keeps a set number of characters of the value and replaces each of the others with
`truncateCharacter`.

| Setting | Default | Description |
| ------- | ------- | ----------- |
| `truncateLeaveCharacters` | `4` | The number of characters to keep. A value below 1 is treated as 1. |
| `truncateDirection` | `LEADING` | `LEADING` keeps the first characters; `TRAILING` keeps the last ones. |
| `truncateCharacter` | `*` | The character put in place of each removed character. |

For example, with the defaults `4111111111111111` becomes `4111************`, and with `TRAILING` it
becomes `************1111`.

Except for zip codes (below), at least one character is always replaced, so a value is never returned
unchanged: a value no longer than `truncateLeaveCharacters` keeps all but one of its characters. With the defaults, `ABCD` becomes
`ABC*` and `AB` becomes `A*` (`*BCD` and `*B` with `TRAILING`), and a single character becomes `*`.

The `TRUNCATE` filter has special behavior for the zip code filter. For zip codes the Zip will always be truncated
to 5 digits long. For example, `truncateLeaveCharacters=2` and a token of `90210-0110` will result in `90***`.
For zip codes, `truncateLeaveCharacters` is kept between 1 and 4: a value below 1 is treated as 1 and a value
above 4 is treated as 4, so at least one digit is always replaced. For example, `truncateLeaveCharacters=5` and a
token of `90210` will result in `9021*` (`*0210` with `TRAILING`).

```
{
   "identifiers": {
      "zipCode": {
         "zipCodeFilterStrategies": [
            {
               "strategy": "TRUNCATE",
               "truncateLeaveCharacters": 3
            }
         ]
      }
   }
}
```

### The `ZERO_LEADING` Filter Strategy {id="zero_leading"}

Available only to zip codes, this strategy changes the first 3 digits of a zip code to be 0. For example, the zip code 90210 will be changed to 00010.

The `ZERO_LEADING` filter strategy is only available to zip code filters. An example zip code filter using the `ZERO_LEADING` filter strategy:

```
{
   "identifiers": {
      "zipCode": {
         "zipCodeFilterStrategies": [
            {
               "strategy": "ZERO_LEADING"
            }
         ]
      }
   }
}
```

### The `ABBREVIATE` Filter Strategy {id="abbreviate"}

This strategy replaces a detected value with the uppercase initials of its whitespace-separated words. For example, `George Washington` becomes `GW`, and `john smith` becomes `JS`. It is most useful for names (for example reducing a person's name to initials) and is available to any filter type.

An example person's names filter using the `ABBREVIATE` filter strategy:

```
{
   "identifiers": {
      "pheyes": [
         {
            "phEyeFilterStrategies": [
               {
                  "strategy": "ABBREVIATE"
               }
            ]
         }
      ]
   }
}
```

## Filter Strategy Conditions

A replacement strategy can be applied based on the sensitive information meeting one or more conditions. For example, you can create a condition such that only dates of `11/05/2010` are replaced by using the condition `token == "11/05/2010"`. The conditions that can be applied vary based on the type of sensitive information. For instance, zip codes can have conditions based on their population. Refer to each specific [filter type](filters.md) for the conditions available.

A condition compares a field to a value. The values compared with the identified text (`token`) and the context (`context`) must be in double quotes. The `token` condition supports these operators on every filter type:

| Operator | Satisfied when | Example |
|---|---|---|
| `==` | The identified text equals the value, ignoring case. | `token == "123-45-6789"` |
| `!=` | The identified text does not equal the value, ignoring case. | `token != "127.0.0.1"` |
| `startswith` | The identified text begins with the value. This comparison is case-sensitive. | `token startswith "3000"` |

The date filter also supports the `is` and `is not` operators for `token`, to tell birth and death dates apart, as in `token is "birthdate"`. See [Dates](filters/common_filters/dates.md).

The conditions of every enabled filter are checked when the policy is loaded. (A filter with `enabled` set to `false` is not loaded, so its conditions are neither checked nor evaluated.) A condition that cannot be parsed, such as one with a value that is not in double quotes (123 where `"123"` belongs), a misspelled field, or a word where a number belongs, refuses the policy with an `InvalidPolicyConditionException` before any document is filtered with it. The message names the filter, the position of the strategy in its list, and the character where the condition stops making sense. It does not repeat the condition, since a condition can contain a sensitive value.

A condition is also refused when no strategy evaluates it, even though it parses. The grammar accepts any field with any operator, but only these are evaluated:

| Field | Operators |
|---|---|
| `token` | `==`, `!=`, and `startswith`; `is` and `is not` with `"birthdate"`, `"deathdate"`, or `"birthdate or deathdate"` |
| `context` | `==` and `!=` |
| `confidence` | `<`, `<=`, `>`, `>=`, `==`, and `!=` |
| `population` | `<`, `<=`, `>`, `>=`, `==`, and `!=` with a whole number, on the zip code filter only |

Any other condition, such as `context startswith "a"`, `token is "x"`, `population > 4500` on a filter other than zip codes, or any condition on `type`, refuses the policy when it is loaded, with a message naming the field and operator. Previously such a condition was never satisfied on its own, so its strategy was skipped, and after a satisfied clause joined with `and` it was ignored, so its strategy applied more widely than written.

The following is an example policy for credit cards that contains a condition to only redact credit card numbers that start with the digits `3000`:

```
{
  "identifiers": {
    "creditCard": {
      "creditCardFilterStrategies": [
        {
          "condition": "token startswith \"3000\"",
          "strategy": "REDACT",
          "redactionFormat": "{{{REDACTED-%t}}}"
        }
      ]
    }
  }
}
```

#### Evaluation Order

A filter's strategies are evaluated in the order they are listed. The first strategy that has no condition, or whose condition is satisfied, is applied, and the remaining strategies are not evaluated.

If every strategy has a condition and none is satisfied, the identified text is left unchanged. It is reported in the explanation's identified spans but not in its applied spans (`getExplanation().appliedSpans()`). In the example above, a credit card number that does not start with `3000` is left in the text as is. To transform every value a filter identifies, end the list with a strategy that has no condition:

```
"creditCardFilterStrategies": [
  {
    "condition": "token startswith \"3000\"",
    "strategy": "REDACT",
    "redactionFormat": "{{{REDACTED-%t}}}"
  },
  {
    "strategy": "MASK"
  }
]
```

A filter with no strategies defaults to `REDACT` with the redaction format `{{{REDACTED-%t}}}`.

#### Combining Conditions

Conditions can be joined through the use of the `and` keyword. When conditions are joined, each condition must be satisfied for the identified text to be filtered. If any of the conditions are not satisfied the identified text will not be filtered. Below is an example joined condition:

```
token != "123-45-6789" and context == "my-context"
```

This condition requires that the identified text (the token) not be equal to `123-45-6789` and the context be equal to `my-context`. Both of these conditions must be satisfied for the identified text to be filtered.

Conversely, conditions can be `OR`'d through the use of multiple filter strategies. For example, if we want to `OR` a condition on the token and a condition on the context, we would use two filter strategies:

```
"ssnFilterStrategies": [
  {
    "condition": "token != \"123-45-6789\"",
    "strategy": "REDACT",
    "redactionFormat": "{{{REDACTED-%t}}}"
  },
  {
    "condition": "context == \"my-context\"",
    "strategy": "REDACT",
    "redactionFormat": "{{{REDACTED-%t}}}"
  }        
]
```
