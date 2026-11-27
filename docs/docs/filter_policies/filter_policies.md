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
* Can have [column rules](#column-rules) (`tabular`) for spreadsheets.

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

The `method` is one of `newline`, `characters`, or `width`, ignoring case, and `character` is accepted as an alias for
`characters`. Any other name is an error: when splitting is enabled, building the policy's filters throws an
`IllegalArgumentException` naming the accepted methods. `PlainTextFilterService.prepare` throws it, and so does
filtering a document with that policy, whether or not the document reaches the `threshold`. Redacting a PDF with that
policy throws it too, although PDFs are not split. With splitting disabled, `method` is not read.

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
whole document, so an overlap does not produce duplicate spans. The cost is that the overlapping text is scanned twice,
so prefer the smallest overlap that covers your values.

Splitting does not change the positions Phileas reports or the text it returns. The pieces are located in the input,
so `characterStart` and `characterEnd` index into the input rather than into the filtered output, and the replacements
are applied to the input, so its newlines and runs of spaces are preserved. This holds with or without an overlap.

A piece that cannot be located in the input falls back to filtering each piece on its own and joining the results.
That does shift the reported positions and normalizes whitespace, and a shifted position can fall outside the input
altogether, so do not index into the input with it. In practice only the `characters` method produces such a piece,
and only when the character separating two sentences is not a space, since it rejoins them with one. The fallback logs
a `WARN` beginning "Split pieces are not verbatim in the input".

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

`config.analysis` also accepts `identification`, which is deprecated and has no effect.

### Review Sampling

A policy can select a share of filter requests for human spot review, so a person can check whole documents for
values that were not detected:

```
{
   "config": {
      "review": {
         "samplingRate": 0.10
      }
   }
}
```

`samplingRate` is the probability, from `0.0` to `1.0`, that a request is selected. The default is `0.0`, which selects
none; `1.0` selects every request. A request is selected when a value drawn uniformly from [0, 1) is below the rate.
The draw is made once per request, whether the input is split into pieces or is a PDF, Word, or Excel document. No
count is kept between requests, so the share selected converges on the rate but is not exact over a small number of
requests. A rate outside `0.0` to `1.0` is refused when the policy is loaded.

Selection does not change the redacted output. It is reported by `isReviewSampled()` on the `TextFilterResult` or
`BinaryDocumentFilterResult`, and as `reviewSampled` when the result is serialized to JSON. What to do with a selected
request, such as queuing it for a reviewer, is up to the application. For a PDF filtered with page policies, the rate
of the policy passed to `filter` applies.

### Leftover Check

A policy can run a second detection pass over the redacted output and report anything still detectable, so a
request can be routed for review because something was left in it:

```
{
   "config": {
      "review": {
         "leftoverCheck": {
            "mode": "always",
            "using": "broad"
         }
      }
   }
}
```

`mode` sets when the check runs:

* `off` (default): never.
* `onRequest`: only when the caller asks for it on that call (see below).
* `always`: on every call. The caller cannot turn it off.

`using` sets what the check detects with:

* `broad` (default): every filter the policy defines, including custom identifiers, dictionaries, and PhEye filters,
  configured as written, plus every other built-in identifier at its defaults. It finds kinds of information the
  policy does not cover. Built-ins that need a model (`person`) or configuration (`identifiers`, `sections`,
  `dictionaries`, `pheyes`) run only as the policy defines them, as do the `firstName` and `surname` dictionaries,
  which match common words at their defaults. Any other built-in the policy disables runs at its defaults.
* `samePolicy`: only the policy's own filters, configured as written. It confirms the policy took effect.

The policy's filters run in the check without their own ignore lists (`ignored`, `ignoredFiles`, `ignoredPatterns`),
and a strategy condition that leaves a value unredacted does not hide it from the check. Only the policy's top-level
`ignored` and `ignoredPatterns` suppress a finding. Values the redaction inserted, such as `RANDOM_REPLACE` stand-ins
and redaction labels, are not reported. The check never changes the redacted output, and it uses its own context, so
it does not read or add to the context used for referential integrity.

The check runs on the whole output after any split pieces are put back together, so a value that spans a boundary
between pieces is found. In a Word or Excel document it scans the same text the redaction does. In a PDF it scans the
text of the output: a page with a redaction is flattened to an image and has no text to read, so only pages kept as
they were (`preserveUnredactedPages`) are checked. For a document filtered to
[text output](pdf.md#redacted-text-output), it scans the returned text instead. An output with no readable text, such as a PDF with every page
flattened or a PDF redacted to images, is reported as not performed rather than as zero findings. In an email message
it scans the redacted fields and takes the check of each redacted attachment; see
[Email Redaction](../email.md#leftover-check).

A `leftoverCheck` that is not an object, has a key other than `mode` and `using`, or has a value outside the lists
above is refused when the policy is loaded. Values are case-sensitive. In PhiSQL:

```
CONFIGURE REVIEW (leftoverCheck = (mode = 'always', using = 'broad'));
```

To ask for the check on a call when the mode is `onRequest`, pass `FilterOptions` to the filter service:

```
final FilterOptions options = FilterOptions.defaults().withLeftoverCheck(true);
final TextFilterResult result = filterService.filter(policy, contextService, vectorService, "context", input, options);
```

`PlainTextFilterService`, its `PreparedPolicy`, `PdfFilterService` (including with page policies),
`OoxmlFilterService`, `CsvFilterService`, `RtfFilterService`, and `EmailFilterService` accept `FilterOptions`.

The outcome is reported by `getLeftoverCheck()` on the `TextFilterResult` or `BinaryDocumentFilterResult`, and as
`leftoverCheck` when the result is serialized to JSON (absent when the check was not called for):

* `isPerformed()` (`performed`): whether the check ran.
* `getReason()` (`reason`): why it did not run: `notRequested`, `unreadableOutput`, or `checkFailed` (the check
  failed with an error; the redaction is returned unaffected, and the error's type is logged as a warning).
* `getFindingCount()` (`findingCount`): the number of findings.
* `getFindings()` (`findings`): each finding's `filterType`, `classification`, `characterStart` and `characterEnd`,
  and `confidence`. For a PDF, `pageNumber` (from 0) and `lineNumber`, with offsets into the output's text with its
  lines joined. For a Word or Excel document, `paragraphNumber` (the paragraph or cell, or -1 for other content such
  as drawings and comments), with offsets into it. For an email message, `paragraphNumber` (the field), with offsets
  into it, and for a finding in a redacted attachment, `attachment` (its filename). For text, offsets into the
  filtered text. For text output, `segment` (the label of the segment), with offsets into its text; absent otherwise.
* `getUncheckedAttachments()` (`uncheckedAttachments`): for an email message, the redacted attachments whose output
  had no readable text, or whose check failed. Absent when there are none.

Findings do not carry the matched text. An application that shows findings to the user it redacted for can ask for
it with `FilterOptions.defaults().withLeftoverCheckMatchedText(true)`, and read it from `getMatchedText()`. It is held
in memory only and is never serialized with the result.

The check is a second detection pass, so it roughly doubles detection cost, and `broad` runs more filters than most
policies do. Like the redaction itself, it is probabilistic: a finding is worth reviewing, and no finding does not
mean the output is free of sensitive information. What to do with a result, such as queuing the request for a
reviewer, is up to the application.

### Email

The policy's `config.email` decides how an email message is redacted beyond its text: which headers are removed
outright, and what happens to attachments and inline images. See [Email Redaction](../email.md).

### Word and Excel

The policy's `config.word` and `config.excel` decide how Word and Excel documents are redacted beyond their text:
whether comments are removed, tracked changes accepted, hidden text removed, and headers, charts, formula and pivot
caches, embedded objects, and document properties redacted. See [Policy settings](../word_and_excel.md#policy-settings).

### Column Rules

The policy's top-level `tabular` section sets rules per column for Excel workbooks and CSV documents: replace every
value in a column, leave it unchanged, or scan it with only some filters. See [Column rules](../word_and_excel.md#column-rules)
and [CSV Redaction](../csv.md#column-rules).

`config.csv` sets how a CSV document is read: `delimiter` (default `,`), `quote` (default `"`), and `encoding` (default
`UTF-8`). Each of `delimiter` and `quote` is one character other than a line break, and they must differ. See
[CSV Redaction](../csv.md#reading-the-document).

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
