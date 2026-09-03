# Sample Policies

This page lists some sample policies. You can use these policies either as-is or as starting points for customizing them to meet your specific de-identification needs.

> These policies are examples and not an exhaustive list of all the sensitive information Phileas can identify. Items from each of these policies can be combined to make policies to meet your use-cases.

### Email Addresses and Phone Numbers

This policy finds email addresses and phone numbers and redacts them with `{{{REDACTED-email-address}}}` and `{{{REDACTED-phone-number}}}`, respectively.

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
    },
    "phoneNumber": {
      "phoneNumberFilterStrategies": [
        {
          "strategy": "REDACT",
          "redactionFormat": "{{{REDACTED-%t}}}"
        }
      ]
    }
  }
}
```

### Persons Names and SSNs

This policy finds persons names with [PhEye](filters/persons_names/ph-eye.md) and SSNs with the SSN filter, and
redacts each with the type of the filter that found it. It needs a running PhEye service at the `endpoint` shown.

```
{
  "identifiers": {
    "pheyes": [
      {
        "phEyeConfiguration": {
          "endpoint": "http://localhost:18080"
        },
        "phEyeFilterStrategies": [
          {
            "strategy": "REDACT",
            "redactionFormat": "{{{REDACTED-%t}}}"
          }
        ]
      }
    ],
    "ssn": {
      "ssnFilterStrategies": [
        {
          "strategy": "REDACT",
          "redactionFormat": "{{{REDACTED-%t}}}"
        }
      ]
    }
  }
}
```

### Dates, URLs, and VINs

This policy finds dates, URLs, and VINs. Dates and URLs are redacted with `{{{REDACTED-date}}}` and `{{{REDACTED-url}}}`, respectively. Each VIN number are replaced by a randomly generated VIN number.

```
{
  "identifiers": {
    "date": {
      "dateFilterStrategies": [
        {
          "strategy": "REDACT",
          "redactionFormat": "{{{REDACTED-%t}}}"
        }
      ]
    },
    "url": {
      "urlFilterStrategies": [
        {
          "strategy": "REDACT",
          "redactionFormat": "{{{REDACTED-%t}}}"
        }
      ]
    },
    "vin": {
      "vinFilterStrategies": [
        {
          "strategy": "RANDOM_REPLACE"
        }
      ]
    }
  }
}
```

### IP Addresses

This policy finds IP addresses and replaces each identified IP address with the static text `IP_ADDRESS` as long as the IP address is not `127.0.0.1`. (A condition on the filter strategy sets the IP address requirement.)

```
{
  "identifiers": {
    "ipAddress": {
      "ipAddressFilterStrategies": [
        {
          "strategy": "STATIC_REPLACE",
          "redactionFormat": "IP_ADDRESS",
          "condition": "token != \"127.0.0.1\""
        }
      ]
    }
  }
}
```

### Zip Codes

This policy finds ZIP codes starting with `90` and truncates the zip code to just the first two digits.

```
{
  "identifiers": {
    "creditCard": {
      "creditCardFilterStrategies": [
        {
          "condition": "token startswith \"90\"",
          "strategy": "TRUNCATE",
          "truncateLeaveCharacters": 2
        }
      ]
    }
  }
}
```

### Enable Text Splitting

This policy enables text splitting for input over 10,000 characters.

```
{
  "config": {
    "splitting": {
      "enabled": true,
      "threshold": 10000,
      "method": "newline"
    }
  },
  "identifiers": {
    "ssn": {
      "ssnFilterStrategies": [
        {
          "strategy": "REDACT",
          "redactionFormat": "{{{REDACTED-%t}}}"
        }
      ]
    }
  }
}
```

### Globally Ignored Terms

This policy has a list of globally ignored terms.

```
{
  "ignored": [
    {
      "name": "ignored credit cards",
      "terms": ["4111111111111111", "0000000000000000"]
    }
  ],
  "identifiers": {
    "creditCard": {
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
