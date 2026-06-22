# Filter Policies

The types of sensitive information identified by Phileas and how that information is de-identified are controlled
through policies.

Each policy has a `name` that is used by Phileas to apply the appropriate de-identification methods. The `name` is
passed to Phileas along with the text to be filtered. This
provides flexibility and allows you to de-identify different types of documents in differing manners with a single
instance of Phileas. For example, you may have a policy for bankruptcy documents and a separate policy for financial
documents.

> There are [sample policies](sample_filter_policies.md) available for immediate use or customization to fit your
> use-cases.

### The Structure of a Policy

A policy:

* Must have a list of `identifiers` that are filters for sensitive information.
    * Each `identifier` , or filter, can have zero or more [filter strategies](filter_strategies.md). A filter strategy
      tells Phileas how to manipulate that type of sensitive information when it is identified.
* Can have an optional list of [terms](ignoring_sensitive_information.md) or [patterns](ignoring_sensitive_information.md).
* Can have encryption keys to support [encryption](filter_strategies.md#fpe) of sensitive information.

### An Example Policy

The following is an example policy. In the example below you can see
the [types of sensitive information](filters.md) that are enabled and the strategy for manipulating each type
when found. This policy identifies email addresses and phone numbers and redacts each with the format given.

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

When an email address is identified by this policy, the email address is replaced with the text
`{{{REDACTED-email-address}}}`. The `%t` gets replaced by the type of the filter. Likewise, when a phone number is found
it is replaced with the text `{{{REDACTED-phone-number}}}`. You are free to change the redaction formats to whatever
fits your use-case. See [Filter Strategies](filter_strategies.md) for all replacement options.

The name of the policy is `email-and-phone-numbers`. Policies can be named anything you like but their names must be
unique from all other policies. As a best practice, the policy should be saved as `[name].json`, e.g.
`email-and-phone-numbers.json`.

### Applying a Policy to Text

A policy is applied by passing it to Phileas' filter service along with the text to filter. Using the
`email-and-phone-numbers` policy from above:

```
Properties properties = new Properties();
PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

PlainTextFilterService filterService = new PlainTextFilterService(
        phileasConfiguration, new DefaultContextService(), new InMemoryVectorService(), null);

TextFilterResult result = filterService.filter(policy, "context", body);
```

Phileas processes the `body` text by applying the policy, which (as shown above) redacts email addresses and phone
numbers, and the `result` contains the redacted text (from `result.getFilteredText()`). The `context` is an arbitrary
value used to uniquely identify the text being filtered. To use a different policy for a given request, simply pass a
different `Policy` to `filter()`.

To manipulate the sensitive information by methods other than redaction, see
the [Filter Strategies](filter_strategies.md).
