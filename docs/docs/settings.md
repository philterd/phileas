# Settings

Phileas has settings to control how it operates. The settings and how to configure each are described below.

> The configuration for the types of sensitive information that Phileas identifies are defined
> in [filter policies](filter_policies/filter_policies.md) outside of Phileas' configuration properties described on
> this
> page.

## Configuring Phileas

### The Phileas Settings File

Phileas looks for its settings in an `application.properties` file.

### Property Sources and Precedence

Each setting can be supplied as an environment variable, a JVM system property, or an entry in the settings
file. They are resolved in that order of precedence: an environment variable overrides a JVM system property
of the same name, which overrides a value in the settings file, which overrides the built-in default.

Environment variables and system properties use the property name exactly as written, including the periods —
there is no `PHILTER_` prefix and no change of case or separators. For example, the property `span.window.size`
is read from an environment variable named `span.window.size` (or a system property of the same name).

> Note: most shells do not permit periods in a variable name used with `export`, so providing these as
> environment variables generally requires a process launcher, container, or orchestrator that can define a
> variable whose name contains periods. Supplying the value as a JVM system property (for example,
> `-Dspan.window.size=5`) or in the settings file is often more convenient.

## Span Disambiguation

These values configure Phileas' span disambiguation feature to determine the most appropriate type of sensitive
information when duplicate spans are identified. In a deployment of multiple Phileas instances, span disambiguation
should use a shared, persistent [vector store](other_features/span_disambiguation.md#cache-service) so the information
learned by one instance is available to the others.

| Setting                                | Description                                                                                                                     | Allowed Values     | Default Value    |
|----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|--------------------|------------------|
| `span.disambiguation.enabled`          | Whether or not to enable span disambiguation.                                                                                   | `true`, `false`    | `false`          |
| `span.disambiguation.hash.algorithm`   | The hashing algorithm used to map context tokens into the vector. Any value other than `murmur3` falls back to Java's `hashCode`. | `murmur3`, `hashCode` | `murmur3`     |
| `span.disambiguation.vector.size`      | The size of the context vector used to represent the tokens surrounding a span. Must match across instances sharing learned vectors. | An integer value. | `512`            |
| `span.disambiguation.ignore.stopwords` | Whether common stop words are excluded from the context vector.                                                                 | `true`, `false`    | `true`           |
| `span.disambiguation.stopwords`        | The comma-separated list of stop words to exclude when `span.disambiguation.ignore.stopwords` is enabled.                       | A comma-separated list of words. | (a built-in English stop word list) |

## Advanced Settings

> In most cases, the settings below do not need changed. Contact us for more information on any of these settings.

| Setting                      | Description                                                                                                                                  | Allowed Values    | Default Value |
|------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|-------------------|---------------|
| `pdf.max.pages` | The most pages a PDF may have to be redacted. A larger PDF is refused with `PdfLimitExceededException` before its text is read. `0` or less sets no limit. | An integer value. | `2000` |
| `pdf.max.pixels.per.page` | The most pixels one PDF page may render to at the policy's `dpi`. Each redacted page is rendered to an image in memory, so a small file with a very large page size, or a policy with a high `dpi`, could otherwise exhaust memory. A PDF with a larger page is refused with `PdfLimitExceededException` before any page is rendered. A page that uses an image larger than this, by the width and height the image declares, is refused the same way, since decoding it would allocate that much. A Letter page at 600 DPI is about 34 million pixels. `0` or less sets no limit. | An integer value. | `50000000` |
| `pdf.max.stream.bytes` | The most bytes one Flate-compressed stream a PDF page uses may decompress to. PDFBox decompresses a stream into memory before reading it, so a few kilobytes of compressed data could otherwise expand to fill the heap. Each stream is checked by inflating it without keeping the output, before the PDF is read or rendered, and a PDF with a larger one is refused with `PdfLimitExceededException`. Not checked: images inline in a content stream, streams with more than one filter, and a JPEG whose data is larger than the size it declares. `0` or less sets no limit. | An integer value. | `268435456` |
| `policy.files.restricted` | When `true`, a file a policy names (custom dictionary `files`, a filter's `ignoredFiles`, a `MAP_REPLACE` strategy's `mappingFiles`, and the top-level `ignored` `files`) must be inside one of `policy.files.directories`, after symbolic links and `..` are resolved. With it on and no directories, a policy may name no file. Set it when the people who write policies should not be able to read arbitrary files on the server. | `true` or `false`. | `false` |
| `policy.files.directories` | Comma-separated directories a file a policy names must be inside when `policy.files.restricted` is `true`. A directory that does not exist allows nothing. | A list of paths. | (none) |
| `policy.files.max.bytes` | The largest file, in bytes, a policy may name. The limit is enforced while the file is read. `0` or less sets no limit. | An integer value. | `0` |
| `regex.timeout.ms`           | The maximum time in milliseconds a single regular-expression match attempt may run before it is aborted as a suspected catastrophic-backtracking (ReDoS) pattern. While filtering, applies to every filter's patterns and to ignored patterns. For a filter's pattern, exceeding the budget fails filtering for that input rather than skipping the pattern. For an ignored pattern, a match that exceeds the budget counts as not matching, so the token is not ignored and stays redacted, and a warning naming the pattern is logged. The same budget bounds the probe every user-supplied pattern (identifier, section, and ignored) is given when the policy is loaded. A value of `0` or less disables the guard, and also skips the probe, since nothing would stop one that does not finish. | An integer value. | `1000`        |
| `span.window.size`           | The number of tokens of context captured on each side of an identified span. Used for context-based features such as span disambiguation.   | An integer value. | `5`           |
| `incremental.redactions.enabled` | Whether to record an incremental redaction (the document state and its hash after each applied redaction) for each filtered document.    | `true`, `false`   | `false`       |

A file a policy names is checked when the policy is loaded, before any filter is built, and read only if it is a regular file: a device such as `/dev/zero` or a named pipe is refused in every configuration, since reading one never ends or blocks. A refusal throws `InvalidPolicyFileException`, naming where in the policy the file is named, with the same message whether the file is missing, outside the allowed directories, or not a regular file. Without `policy.files.restricted`, a missing file is handled as before: a missing custom dictionary or top-level `ignored` file fails the policy, and a missing `ignoredFiles` or `mappingFiles` file is logged and skipped.

## Environment variables in a policy

A policy can name an environment variable as `env:NAME` for a `crypto` `key` or `iv`, or an `fpe` `key` or `tweak`, and an application that registers `PlaceholderDeserializer` resolves `${NAME}` in any string. An application that lets people author policies should limit which variables those may be, so a policy author cannot use a variable meant for something else, such as another policy's key, as their own.

Set the allowlist once at startup with `PolicyEnvironment.restrictTo(names)`, or with `policy.env.allowed`, a comma-separated list read from an environment variable or system property of that name. An empty list allows no variable. When neither sets it, every variable may be named, as before.

With the allowlist set, a policy naming any other variable throws `InvalidPolicyEnvironmentException` when the policy is loaded, and whenever a key is resolved, without reading the variable, so the message is the same whether or not it exists. `PlaceholderDeserializer` leaves such a placeholder as written. The allowlist applies to the whole process, so every policy may use each listed variable.
