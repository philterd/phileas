# Alerts

Phileas can optionally generate alerts when a particular type of sensitive information is identified.

### Alert Conditions

In a policy, each type of sensitive information can have zero or more filter strategies. Each filter strategy can optionally have a condition associated with it. When a condition is present, the filter strategy will only be satisfied when the condition is satisfied. For example, a condition may be created to only filter phone numbers that start with the digits `123` or only filter names that start with `John`. Filter strategy conditions give you granular control over the filtering process.

When a filter strategy condition is satisfied, Phileas can optionally generate an alert. This feature allows you to be notified when a particular type of sensitive information is identified.

### Enabling Alerts

Alerts are enabled on a per-condition basis. For instance, given the following policy to identify email addresses, a condition has been added to only match the email address `test@test.com`. Because of the property `alert` set to `true`, an alert will be generated when this condition is satisfied. By default, the alert property is set to `false` disabling alerts for the condition.

```
{
  "name": "email-address-alert",
  "identifiers": {
    "emailAddress": {
      "emailAddressFilterStrategies": [
        {
          "id": "my-email-strategy",
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

### Structure of an Alert

An alert contains the following information:

| Property Name   | Description                                                                                                     |
| --------------- | --------------------------------------------------------------------------------------------------------------- |
| `id`            | A unique ID for the alert formatted as an UUID.                                                                 |
| `filterProfile` | The name of the policy triggering the alert.                                                            |
| `strategyId`    | The ID of the filter strategy triggering the alert. In the example above the `id` would be `my-email-strategy`. |
| `context`       | The context.                                                                                                    |
| `documentId`    | The ID of the document which triggered the alert.                                                               |
| `filterType`    | The filter type ("email-address", "credit-card", etc.) triggering the alert.                                    |
| `date`          | A timestamp when the alert was generated formatted as `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`**.**                       |

### Retrieving and Deleting Alerts

The alerts that Phileas has generated are available through Phileas' [alerts API](alerts-api.md). This API allows for retrieving and deleting alerts. Using this API you can build sophisticated notification systems around Phileas' capabilities.
