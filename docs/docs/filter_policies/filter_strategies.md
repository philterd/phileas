# Filter Strategies

A filter strategy defines how sensitive information identified by Phileas should be manipulated, whether it is redacted, replaced, encrypted, or manipulated in some other fashion.

In a policy, you list the types of sensitive information that should be filtered. How Phileas replaces each type of sensitive information is specific to each type. For instance, zip codes can be truncated based on the leading digits or zip code population while phone numbers are redacted. These replacements are performed by "filter strategies."

> Each filter can have one or more filter strategies and conditions can be used to determine when to apply each filter strategy.


A sample policy containing a filter strategy is shown below. In this example, email addresses will be redacted.

```
{
   "name": "email-address",
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

> Most of the filter strategies apply to all types of data, however, some filter strategies only apply to a few types. For example, the `ZERO_LEADING` filter strategy only applies to the zip code filter, and the `ABBREVIATE` filter strategy only applies to the person's names (NER) filter.


## Filter Strategies

The filter strategies are described below. Each filter type can specify zero or more filter strategies. When no filter strategies are given, Phileas will default to `REDACT` for that filter type. When multiple filter strategies are given for a single filter type, the filter strategies will be applied in order as they are listed in the policy, top to bottom.

* [`REDACT`](filter_strategies.md#the-redact-filter-strategy)
* [`MASK`](filter_strategies.md#mask)
* [`CRYPTO_REPLACE`](filter_strategies.md#crypto)(AES-GCM authenticated encryption)
* [`HASH_SHA256_REPLACE`](filter_strategies.md#hash)(SHA512 encryption)
* [`FPE_ENCRYPT_REPLACE`](filter_strategies.md#fpe)(Format-preserving encryption)
* [`RANDOM_REPLACE`](filter_strategies.md#random)
* [`STATIC_REPLACE`](filter_strategies.md#static)
* [`TRUNCATE`](filter_strategies.md#truncate)
* [`ZERO_LEADING`](filter_strategies.md#zero_leading)
* [`ABBREVIATE`](filter_strategies.md#abbreviate)

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
   "name": "email-address",
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
   "name":"sample-profile",
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

> A fresh random nonce is generated for every value, so encrypting the same value twice produces different output — identical values do not produce identical redactions across the corpus — and each value carries an authentication tag that detects tampering. Because the nonce is random, no initialization vector (`iv`) is required; an `iv` in an existing policy is ignored.

The encrypted replacement has the form `{{<base64>}}`, where the Base64 content is `nonce || ciphertext || tag` (a 12-byte nonce, the ciphertext, and a 16-byte authentication tag).

#### Decrypting a value

Because GCM is an authenticated mode, a value cannot be decrypted with the `openssl enc` command line, which does not support GCM. Decrypt it with a GCM-capable library instead — for example, with Python:

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
   "name": "email-address",
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
   "name": "email-address",
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

> **Keep these secrets out of the policy file.** As with the [crypto key](#crypto), prefix either value with `env:` to read it from an environment variable at runtime — for example, `"key": "env:FPE_KEY"` and `"tweak": "env:FPE_TWEAK"`. This is the recommended way to supply the `key` and `tweak`.

> FF3 can only encrypt values whose format-preservable (alphanumeric) content is between 6 and 56 characters long. When a detected value falls outside this range — for example a 5-digit ZIP code — it cannot be format-preserving encrypted. In that case Phileas falls back to redacting that value (using the `REDACT` placeholder) instead, so the value is still redacted and a single out-of-range value does not affect the rest of the document. Consider applying `FPE_ENCRYPT_REPLACE` to types whose values fall within the supported length range.

> **Which filters support FPE.** Every filter supports the `FPE_ENCRYPT_REPLACE` strategy except the **date** and **zip code** filters, which do not offer it. (A ZIP code is shorter than the 6-character minimum noted above; date values use the date-specific strategies such as `SHIFT` instead.) Each filter's own documentation page lists the strategies it supports.

An example policy using the FPE\_ENCRYPT\_REPLACE filter strategy:

```
{
   "name": "credit-cards",
   "identifiers": {
      "creditCardNumbers": {
         "creditCardNumbersFilterStrategies": [
            {
               "strategy": "FPE_ENCRYPT_REPLACE",
               "key": "...",
               "tweak": "..."
            }
         ]
      }
   }
}
```

### The `RANDOM_REPLACE` Filter Strategy {id="random"}

Replaces the identified text with one of the following:

* A fake value but of the same type. For example, an SSN will be replaced by a random text having the format `###-##-####`, such as 123-45-6789. An email address will be replaced with a randomly generated email address. Available to all filter types. Set `anonymizationMethod` to `REALISTIC`.
* A random value from a list of candidate replacement values. For example, an SSN could be replaced with a random value from a list of known fake SSNs. Available to all filter types. Set `anonymizationMethod` to `FROM_LIST`, and set `candidateReplacementValues` to a list of candidate replacement values.
* A random UUID. Set `anonymizationMethod` to `UUID`.

If `anonymizationMethod` is not specified, the default is `REALISTIC`. If an invalid value is given for `anonymizationMethod`, `UUID` will be used as the default.

An example policy using the `REALISTIC` filter strategy:

```
{
   "name": "email-address",
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
   "name": "email-address",
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "RANDOM_REPLACE",
               "anonymizationMethod": "FROM_LIST",
               "candidateReplacementValues": [
                  "123-45-6789",
                  "987-65-4321",
                  "555-55-5555"
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
   "name": "email-address",
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
   "name": "email-address",
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

### The `MASK` Filter Strategy {id="mask"}

This strategy replaces each character of the identified text with a mask character. By default the mask character
is `*` and the masked value is the same length as the original text. Set `maskCharacter` to change the character
used. Set `maskLength` to a number to force a fixed length, or leave it as the default `SAME` to preserve the
original length. Available to all filter types.

An example policy using the `MASK` filter strategy:

```
{
   "name": "credit-cards",
   "identifiers": {
      "creditCardNumbers": {
         "creditCardNumbersFilterStrategies": [
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

This strategy allows for truncating tokens to only a select number of digits. Specify `truncateLeaveCharacters`
to set the desired number of digits to leave. For example, if `truncateLeaveCharacters` is 4, the
string `4111111111111111` will be truncated to `4111************`. `truncateDirection` can be set to
`LEADING` (the default) which leaves N leading digits or `TRAILING` which leaves N trailing digits.
`truncateCharacter` can be overwritten (defaults to `*`) to change the character that is used for the
replacement.

The `TRUNCATE` filter has special behavior for the zip code filter. For zip codes the Zip will always be truncated
to 5 digits long. For example, `truncateLeaveCharacters=2` and a token of `90210-0110` will result in `90***`.

```
{
   "name": "zip-codes",
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
   "name": "zip-codes",
   "identifiers": {
      "zipCodes": {
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

Available only to the person's names (NER) filter, this strategy replaces a person's name with its initials. For example, `George Washington` will be changed to `GW`.

An example person's names filter using the `ABBREVIATE` filter strategy:

```
{
   "name": "ner-example",
   "identifiers": {
      "pheye": {
         "pheyeFilterStrategies": [
            {
               "strategy": "ABBREVIATE"
            }
         ]
      }
   }
}
```

## Filter Strategy Conditions

A replacement strategy can be applied based on the sensitive information meeting one or more conditions. For example, you can create a condition such that only dates of `11/05/2010` are replaced by using the condition `token == "11/05/2010"`. The conditions that can be applied vary based on the type of sensitive information. For instance, zip codes can have conditions based on their population. Refer to each specific [filter type](filters.md) for the conditions available.

The following is an example policy for credit cards that contains a condition to only redact credit card numbers that start with the digits `3000`:

```
{
  "name": "default",
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
