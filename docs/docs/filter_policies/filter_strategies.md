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

> Most of the filter strategies apply to all types of data, however, some filter strategies only apply to a few types. For example, the `TRUNCATE` filter strategy only applies to a zip code filter.


## Filter Strategies

The filter strategies are described below. Each filter type can specify zero or more filter strategies. When no filter strategies are given, Phileas will default to `REDACT` for that filter type. When multiple filter strategies are given for a single filter type, the filter strategies will be applied in order as they are listed in the policy, top to bottom.

* [`REDACT`](filter-strategies.md#the-redact-filter-strategy)
* [`CRYPTO_REPLACE`](filter-strategies.md#crypto)(AES encryption)
* [`HASH_SHA256_REPLACE`](filter-strategies.md#hash)(SHA512 encryption)
* [`FPE_ENCRYPT_REPLACE`](filter-strategies.md#fpe)(Format-preserving encryption)
* [`RANDOM_REPLACE`](filter-strategies.md#random)
* [`STATIC_REPLACE`](filter-strategies.md#static)
* [`TRUNCATE`](filter-strategies.md#truncate)
* [`ZERO_LEADING`](filter-strategies.md#zero_leading)

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

The `CRYPTO_REPLACE` filter strategy replaces each identified piece of sensitive information by encrypting it using the AES encryption algorithm. To use this filter strategy, the policy must include the details of the encryption key as shown below:

```
{
   "name":"sample-profile",
   "crypto": {
     "key": "....",
     "iv": "...."
   },
   ...
```

In the snippet of a policy shown above, a crypto element is is defined with a `key` and an initialization vector (`iv`). These two items are required to encrypt the sensitive information. To generate a key, run the following command:

```
openssl enc -e -aes-256-cbc -a -salt -P
```

You will be prompted to enter an encryption password. Once entered, the values of the `key` and `iv` will be shown. Copy and paste those values into the policy.

An example policy using the `CRYPTO_REPLACE` filter strategy:

```
{
   "name": "email-address",
   "crypto": {
     "key": "....",
     "iv": "...."
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

The `HASH_SHA256_REPLACE` filter strategy replaces sensitive information with the SHA256 hash value of the sensitive information. To append a random salt value to each value prior to hashing, set the `salt` property to `true`. The salt value used will be returned in the `explain` response from Phileas' API.

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

Replaces the identified text with a fake value but of the same type. For example, an SSN will be replaced by a random text having the format `###-##-####`, such as 123-45-6789. An email address will be replaced with a randomly generated email address. Available to all filter types.

An example policy using the `RANDOM_REPLACE` filter strategy:

```
{
   "name": "email-address",
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "RANDOM_REPLACE"
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

### The `TRUNCATE` Filter Strategy {id="truncate"}

Available only to zip codes, this strategy allows for truncating zip codes to only a select number of digits. Specify `truncateDigits` to set the desired number of leading digits to leave. For example, if `truncateDigits` is 2, the zip code 90210 will be truncated to `90***`.&#x20;

The TRUNCATE filter strategy is available only to the zip code filter. An example policy using the `TRUNCATE` filter strategy:

```
{
   "name": "zip-codes",
   "identifiers": {
      "zipCode": {
         "zipCodeFilterStrategies": [
            {
               "strategy": "TRUNCATE",
               "truncateDigits": 3
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

## Filter Strategy Conditions

A replacement strategy can be applied based on the sensitive information meeting one or more conditions. For example, you can create a condition such that only dates of `11/05/2010` are replaced by using the condition `token == "11/05/2010"`. The conditions that can be applied vary based on the type of sensitive information. For instance, zip codes can have conditions based on their population. Refer to each specific [filter type](filters_README.md) for the conditions available.

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
