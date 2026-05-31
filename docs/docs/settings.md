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
| `regex.timeout.ms`           | The maximum time in milliseconds a single regular-expression match attempt may run before it is aborted as a suspected catastrophic-backtracking (ReDoS) pattern. Applies to user-supplied identifier and section patterns. A value of `0` or less disables the guard. | An integer value. | `1000`        |
| `span.window.size`           | The number of tokens of context captured on each side of an identified span. Used for context-based features such as span disambiguation.   | An integer value. | `5`           |
| `incremental.redactions.enabled` | Whether to record an incremental redaction (the document state and its hash after each applied redaction) for each filtered document.    | `true`, `false`   | `false`       |
