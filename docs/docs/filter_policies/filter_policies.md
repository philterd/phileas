# Filter Policies

The types of sensitive information identified by Phileas and how that information is de-identified are controlled through policies. A policy is a file stored under Phileas’s `policies` directory, which by default is located at `/opt/Phileas/policies/`. You can have an unlimited number of policies.

Each policy has a `name` that is used by Phileas to apply the appropriate de-identification methods. The `name` is passed to Phileas’s [API](filtering-api.md) along with the text to be filtered when submitting text to Phileas. This provides flexibility and allows you to de-identify different types of documents in differing manners with a single instance of Phileas. For example, you may have a policy for bankruptcy documents and a separate policy for financial documents.

> There are [sample policies](sample_filter_policies.md) available for immediate use or customization to fit your use-cases.


### The Structure of a Policy

A policy:

* Must have a `name` that uniquely identifies it.
* Must have a list of `identifiers` that are filters for sensitive information.
    * Each `identifier` , or filter, can have zero or more [filter strategies](filter-strategies.md). A filter strategy tells Phileas how to manipulate that type of sensitive information when it is identified.
* Can have an optional list of [terms](ignore-lists.md) or [patterns](ignoring-patterns.md).
* Can have encryption keys to support [encryption](filter-strategies.md#fpe) of sensitive information.

### An Example Policy

The following is an example policy. In the example below you can see the [types of sensitive information](filters_README.md) that are enabled and the strategy for manipulating each type when found. This policy identifies email addresses and phone numbers and redacts each with the format given.

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

When an email address is identified by this policy, the email address is replaced with the text `{{{REDACTED-email-address}}}`. The `%t` gets replaced by the type of the filter. Likewise, when a phone number is found it is replaced with the text `{{{REDACTED-phone-number}}}`. You are free to change the redaction formats to whatever fits your use-case. See [Filter Strategies](filter-strategies.md) for all replacement options.

The name of the policy is `email-and-phone-numbers`. Policies can be named anything you like but their names must be unique from all other policies. As a best practice, the policy should be saved as `[name].json`, e.g. `email-and-phone-numbers.json`.

### Applying a Policy to Text

To use this policy we will save it as `/opt/Phileas/profiles/email-and-phone-numbers.json`. We must restart Phileas for the new profile to be available for use. To apply the policy we will pass the policy's name to Phileas when making a filter request, as shown in the example request below.

```
curl -k -X POST "https://localhost:8080/api/filter?c=context&p=email-and-phone-numbers" \
  -d @file.txt -H Content-Type "text/plain"
```

In this command, we have provided the parameter `p` along with a value that is the name of the policy we want to use for this request. If we had multiple policies in Phileas we could choose a different policy for this request simply by changing the name given to the parameter `p`. For more details see Phileas’s [API](filtering-api.md).

Phileas will process the contents of `file.txt` by applying the policy named `email-and-phone-numbers`. As we saw in the policy above, this policy redacts email addresses and phone numbers. Phileas will return the redacted text in response to the API call.

To manipulate the sensitive information by methods other than redaction, see the [Filter Strategies](filter-strategies.md).
