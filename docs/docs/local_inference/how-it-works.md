# How It Works

This module plugs into Phileas through a small service-provider interface (SPI) and reimplements the GLiNER inference pipeline that PhEye runs through the Python `gliner` library.

## The SPI and service discovery

Phileas core defines a small SPI in the package `ai.philterd.phileas.services.filters.ai.pheye`:

- `PhEyeDetector`: produces raw detections (`PhEyeSpan`) for a piece of text.
- `PhEyeDetectorProvider`: builds a detector. It is discovered at runtime via `java.util.ServiceLoader`.

When a PhEye filter is configured with a `modelPath` (the policy field, or PhiSQL's `DETECT PHEYE ... MODEL '<path>'`), `PhEyeFilter` looks up a `PhEyeDetectorProvider` on the classpath and asks it to build a local detector. With no `modelPath`, Phileas uses the remote HTTP detector instead.

This module ships `LocalPhEyeDetectorProvider` and registers it in `META-INF/services/ai.philterd.phileas.services.filters.ai.pheye.PhEyeDetectorProvider`. Because the provider is registered for `ServiceLoader`, simply adding this module as a dependency makes local inference available. No extra wiring is required.

If a policy sets a `modelPath` but this module is not on the classpath, Phileas does not silently fall back to the remote detector. It fails fast while the policy's filters are built (for example when you call `prepare(policy)`, or on the first `filter()` call for the policy) with a `MissingPhEyeProviderException` and a logged error naming this dependency, so the missing module surfaces when the policy is loaded rather than part-way through a document.

The detector returns `PhEyeSpan` results. The `PhEyeFilter` then applies its per-label thresholds and replacement strategies on top, exactly as it does for remote detections, so the rest of the redaction policy behaves identically whether detection ran locally or remotely.

## The GLiNER pipeline

`LocalPhEyeDetector` reimplements the pipeline PhEye runs through the Python `gliner` library (GLiNER 0.2.25, uni-encoder span model, markerV0):

1. Split text into words with a Unicode-aware whitespace splitter (regex `\w+(?:[-_]\w+)*|\S`), keeping each word's character offsets.
2. Build the prompt as `[<<ENT>> label]*  <<SEP>>  <text words>`.
3. Tokenize the prompt and text pre-split, and build a `words_mask` marking the first subtoken of each text word.
4. Enumerate candidate spans `[i, i+width]` for `width` in `0..max_width-1` (`max_width` is 12 for the PII model).
5. Run the ONNX model with inputs `input_ids`, `attention_mask`, `words_mask`, `text_lengths`, `span_idx`, and `span_mask`, and read the `logits` output of shape `[words, width, classes]`.
6. Decode: apply `sigmoid(logits) > threshold`, then perform greedy flat (non-overlapping, highest score first) selection.
7. Map the selected word spans back to character offsets and return them as `PhEyeSpan` results.

The span width, maximum length, and prompt tokens come from the model's `gliner_config.json`. See [Model Directory](model-directory.md) for the files this requires, and [Limitations and Accuracy](limitations.md) for the parity status of this port.
