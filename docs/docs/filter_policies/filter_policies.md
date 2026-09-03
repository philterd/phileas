# Filter Policies

The types of sensitive information identified by Phileas and how that information is de-identified are controlled
through policies.

A policy is passed to Phileas along with the text to be filtered. This provides flexibility and allows you to
de-identify different types of documents in differing manners with a single instance of Phileas. For example, you may
have a policy for bankruptcy documents and a separate policy for financial documents.

> There are [sample policies](sample_filter_policies.md) available for immediate use or customization to fit your
> use-cases.

### The Structure of a Policy

A policy:

* Must have a list of `identifiers` that are filters for sensitive information.
    * Each `identifier` , or filter, can have zero or more [filter strategies](filter_strategies.md). A filter strategy
      tells Phileas how to manipulate that type of sensitive information when it is identified.
* Can have an optional list of [terms](ignoring_sensitive_information.md) or [patterns](ignoring_sensitive_information.md).
* Can have encryption keys to support [encryption](filter_strategies.md#fpe) of sensitive information.
* Can have an optional [`metadata`](#policy-metadata) object describing the policy itself.
* Can give each filter an optional [`id`](#filter-identifiers) naming it in logs.

### An Example Policy

The following is an example policy. In the example below you can see
the [types of sensitive information](filters.md) that are enabled and the strategy for manipulating each type
when found. This policy identifies email addresses and phone numbers and redacts each with the format given.

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

When an email address is identified by this policy, the email address is replaced with the text
`{{{REDACTED-email-address}}}`. The `%t` gets replaced by the type of the filter. Likewise, when a phone number is found
it is replaced with the text `{{{REDACTED-phone-number}}}`. You are free to change the redaction formats to whatever
fits your use-case. See [Filter Strategies](filter_strategies.md) for all replacement options.

A policy is not named inside the JSON. A policy stored as a file is identified by its file name, so name the file for
what the policy does, for example `email-and-phone-numbers.json`. To describe a policy in the policy itself, use
[`metadata.description`](#policy-metadata).

### Policy Metadata

A policy can carry an optional top-level `metadata` object describing the policy itself. Nothing in `metadata` affects
detection or redaction. Phileas reads and writes it unchanged, so a description travels with the policy through export,
import, and sharing instead of living in separate storage.

```
{
   "metadata": {
      "description": "Redacts intake forms before archival.",
      "author": "records team"
   },
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "REDACT"
            }
         ]
      }
   }
}
```

`metadata.description` is a human-readable description of what the policy does. It is available from
`Policy.getDescription()` and set with `Policy.setDescription(String)`. Compiling a
[PhiSQL](https://philterd.github.io/phisql/) policy with a `DESCRIPTION` clause writes the text here.

Additional properties are allowed, as shown by `author` above, so the object can grow without a schema change. Keys
Phileas does not model survive a load and save unchanged. Do not put sensitive information in `metadata`: it is not
redacted, and it is written back out with the policy.

### Filter Identifiers

Any filter can carry an optional `id`, a label that names that filter in logs and diagnostics. It has no effect on
detection or redaction, and nothing else in the policy refers to it.

```
{
   "identifiers": {
      "ssn": {
         "id": "intake-ssn",
         "ssnFilterStrategies": [
            {
               "strategy": "REDACT"
            }
         ]
      }
   }
}
```

A filter names itself in a log message by its type, qualified by its `id` when one is set, for example
`ssn (id: intake-ssn)`. This traces a message to one filter in a policy that has several of the same type, such as two
custom dictionaries. Do not put sensitive information in an `id`: it is written to logs, and it is written back out
with the policy.

### Splitting Large Documents

A large document can be split into pieces that are filtered one at a time. Splitting is off by default and applies
only to input at or over the `threshold`.

| Property    | Description                                                                                        | Default   |
|-------------|----------------------------------------------------------------------------------------------------|-----------|
| `enabled`   | Whether to split input at or over the `threshold`.                                                 | `false`   |
| `threshold` | The input length (characters) at which splitting starts. Also sets the piece size.                 | `10000`   |
| `method`    | How to split: `newline`, `characters` (sentence-aware), or `width` (wraps on spaces).              | `newline` |
| `overlap`   | Characters each piece shares with the end of the previous piece.                                   | `0`       |

Without an overlap, pieces are contiguous, so a value sitting across a piece boundary is seen only in part by each
piece and can be missed. With `width` splitting, for example, `May 22, 1999` can be cut after `May 22,`, leaving the
year in the output. An `overlap` gives each piece the trailing characters of the one before it, so such a value is seen
whole. Set it larger than the longest value you expect to detect.

```
{
   "config": {
      "splitting": {
         "enabled": true,
         "threshold": 10000,
         "method": "width",
         "overlap": 200
      }
   },
   "identifiers": {
      "date": {
         "dateFilterStrategies": [
            {
               "strategy": "REDACT"
            }
         ]
      }
   }
}
```

A value found in an overlap is detected by both pieces. Phileas keeps one of them and reports it at its position in the
whole document, so an overlap does not produce duplicate spans or shifted offsets. The cost is that the overlapping
text is scanned twice, so prefer the smallest overlap that covers your values.

### Span Disambiguation

Some values match more than one filter: nine digits can be an SSN or a phone number. Span disambiguation resolves
which type applies by comparing the text around the value to what it has seen for each type in that context.

It is a deployment-wide feature, off by default, enabled with `span.disambiguation.enabled` in the Phileas
configuration. A policy can opt out of it:

```
{
   "config": {
      "analysis": {
         "spanDisambiguation": false
      }
   }
}
```

Disambiguation runs only when the deployment has it enabled and the policy has not set `spanDisambiguation` to
`false`. A policy can turn it off, but cannot turn it on when the deployment has it disabled. The default is `true`,
so a policy that omits the property behaves as before. Turning it off lowers the per-document cost for policies whose
filters are not ambiguous.

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
