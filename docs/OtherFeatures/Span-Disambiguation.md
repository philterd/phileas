# Span Disambiguation

Span disambiguation is an optional feature in Phileas that is disabled by default. Refer to Phileas' [Settings](settings.md#cache) to enable and configure span disambiguation.

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

When multiple application using Phileas are deployed alongside each other behind a load balancer, Phileas' [cache service](settings.md#cache) should be configured and enabled. Phileas will store the information needed to disambiguate spans in the cache such that the information is available to each instance of Phileas. If only a single instance of Phileas is running then the cache service is not required, however, the information needed to disambiguate spans will be stored in memory and will be lost when Phileas is stopped or restarted. Because of this, we recommend the cache service always be used unless there is a specific reason not to.

#### Fine-Tuning the Span Disambiguation

There are properties available to fine-tune how the span disambiguation operates. These properties are not documented because improper use of the properties could have a negative impact on performance. We will be glad to walk through these properties upon request.
