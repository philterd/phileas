# Sample Policies

This page lists some sample policies. You can use these policies either as-is or as starting points for customizing them to meet your specific de-identification needs.

<!--To use a policy, download the policy to Phileas' `policies` directory, which by default is `/opt/Phileas/policies`. Then restart Phileas with `sudo systemctl restart Phileas`. The new policy will now be available for use when submitting filter API requests to Phileas. (Specify the policy's name in the request. See the [API](filtering-api.md) for examples.)-->

> These policies are examples and not an exhaustive list of all the sensitive information Phileas can identify. Items from each of these policies can be combined to make policies to meet your use-cases.


### Email Addresses and Phone Numbers

This policy finds email addresses and phone numbers and redacts them with `{{{REDACTED-email-address}}}` and `{{{REDACTED-phone-number}}}`, respectively.

```
{
  "name": "email-and-phone-numbers",
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

This policy finds persons names and SSNs and redacts them with `{{{REDACTED-entity}}}` and `{{{REDACTED-ssn}}}`, respectively.

```
{
  "name": "persons-names-ssn",
  "identifiers": {
    "ner": {
      "nerFilterStrategies": [
        {
          "strategy": "REDACT",
          "redactionFormat": "{{{REDACTED-%t}}}"
        }
      ]
    },
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
  "name": "dates-urls-vin",
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
  "name": "ip-addresses",
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
  "name": "zip-codes",
  "identifiers": {
    "creditCard": {
      "creditCardFilterStrategies": [
        {
          "condition": "token startswith \"90\"",
          "strategy": "TRUNCATE",
          "truncateDigits": 2
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
  "name": "default-split-enabled",
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
  "name": "default-global-ignore",
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

### Generating Alerts

This policy generates an alert when a matching email address is identified.

```
{
  "name": "email-address-alert",
  "identifiers": {
    "emailAddress": {
      "emailAddressFilterStrategies": [
        {
          "strategy": "REDACT",
          "redactionFormat": "{{{REDACTED-%t}}}",
          "condition": "token == \"test@test.com\"",
          "alert": true
        }
      ]
    }
  }
}
```