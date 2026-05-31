# Span Disambiguation

Span disambiguation is an optional feature in Phileas that is disabled by default. Refer to Phileas' [Settings](../settings.md#span-disambiguation) to enable and configure span disambiguation.

In Phileas, a _span_ is a piece of the input text that Phileas has identified as sensitive information. A span has a start and end positions, a confidence, a type, and other attributes. Ideally, each piece of identified sensitive information will only have a single span associated with it. In this case, the type of sensitive information is unambiguous. The goal of span disambiguation is provide more accurate filtering by removing the potential ambiguities in the types of sensitive information for duplicate spans.

However, sometimes a piece of text can be identified by multiple spans, each having a different type of sensitive information. In an example hypothetical scenario, let's say given the input text `My SSN is 123456789.` , Phileas identifies `123456789` as an SSN and as a phone number. This type of scenario can be quite common, and its likelihood increases as the number of enabled filters in a policy increase.

### How Phileas' Span Disambiguation Works

When we read the sentence `My SSN is 123456789.` we can tell the span in question should be identified as an SSN because we can look at the text surrounding the span. We use the surrounding words to deduce the correct type of sensitive information for `123456789`.

That is exactly how Phileas' span disambiguation works. When presented with identical spans differing only by the type of sensitive information, Phileas looks at the text surrounding the span in question in combination with the previous spans it has seen in the same context to determine which type of sensitive information is most likely to be correct. Phileas then removes the ambiguous spans from the results and replaces them with a single span.

### Improves Over Time

Because Phileas is able to consider previously seen text to make its decision concerning ambiguous spans, Phileas' span disambiguation gets "smarter" as more text is filtered. This is because Phileas will have more text to consider in its calculations.

### More Details

#### Span Disambiguation and Confidence Values

Span disambiguation is only invoked for spans that differ only by the type of sensitive information. This means the span's location (start and end positions), confidence, and all other values must match. If two spans have identical locations but have different confidence values, span disambiguation will not be applied and the span having the highest confidence will be used.

#### Cache Service

Span disambiguation accumulates the context vectors it learns in a _vector store_ — a cache of the information needed to disambiguate spans. Phileas provides two vector store implementations:

- **In-memory (the default).** The learned vectors are held only in memory. This requires no additional setup, but the vectors are not shared between instances and are lost when Phileas stops or restarts.
- **File-based.** The learned vectors are persisted to a file and reloaded on startup, so the "improves over time" learning survives restarts.

The vector store is supplied to Phileas by the application that embeds it, so which store is used is determined by how Phileas is deployed rather than by a configuration property.

When multiple applications using Phileas run alongside each other (for example, behind a load balancer), they should share a single persistent vector store so the vectors learned by one instance are available to all of them. Otherwise each instance learns independently, only from the documents it happens to process. For a single instance, the default in-memory store is sufficient, though its learning is lost on restart.

#### Fine-Tuning the Span Disambiguation

Additional properties are available to fine-tune how span disambiguation operates, such as the vector size, the hash algorithm, and stop-word handling. These are listed under [Span Disambiguation in the Settings](../settings.md#span-disambiguation). Most deployments do not need to change them, and improper values — for example, changing the vector size or hash algorithm after vectors have already been learned — can reduce accuracy or performance.
