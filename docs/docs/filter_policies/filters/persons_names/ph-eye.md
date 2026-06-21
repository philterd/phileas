# Person's Names (NER)

## Filter

This filter identifies Person's names based on natural language processing (NLP) and named-entity recognition (NER) in
text.

### Required Parameters

This filter requires the connection properties for the ph-eye service in a `phEyeConfiguration` object:

| Parameter  | Description                                                       | Default Value             |
|------------|-------------------------------------------------------------------|---------------------------|
| `endpoint` | The ph-eye service endpoint.                                      | `http://localhost:18080` |
| `timeout`  | The ph-eye service connection timeout in seconds.                 | `600`                     |
| `labels`   | A comma-separated list of labels supported by the ph-eye service. The default `ph-eye-pii-en-*` models are trained on the `name` label; both `name` and `Person` are recognized as person names. | `name`                  |

### Optional Parameters

| Parameter               | Description                                                                                                                                                                                                  | Default Value                                            |
|-------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `removePunctuation`     | When set to true, punctuation will be removed prior to analysis.                                                                                                                                             | `false`                                                  |
| `phEyeFilterStrategies` | A list of filter strategies.                                                                                                                                                                                 | None                                                     |
| `bearerToken`           | A bearer token for the Ph-Eye service.                                                                                                                                                                       | None                                                     |
| `modelPath`             | Path to a local GLiNER model directory for on-device inference. When set, detection runs locally instead of calling the remote `endpoint`. See [Local inference](#local-inference) below.                     | None                                                     |
| `enabled`               | When set to false, the filter will be disabled and not applied                                                                                                                                               | `true`                                                   |
| `ignored`               | A list of terms to be ignored by the filter.                                                                                                                                                                 | None                                                     |
| `windowSize`            | Sets the size of the window (in terms) surrounding a span to look for contextual terms. If set, this value overrides the value of `span.window.size` in the configuration.                                   | The value of `span.window.size` which is by default `5`. |
| `priority`              | The priority (integer) of this filter. Valid values are any positive integer, where a higher value indicates a higher priority. Priority is used for tie-breaking when two spans may be otherwise identical. | `0`                                                      |

### Filter Strategies

The filter may have zero or more filter strategies. When no filter strategy is given the default strategy of `REDACT` is
used. When multiple filter strategies are given the filter strategies will be applied in as they are listed.
See [Filter Strategies](#filter-strategies) for details.

| Strategy              | Description                                               |
|-----------------------|-----------------------------------------------------------|
| `REDACT`              | Replace the sensitive text with a placeholder.            |
| `MASK`                | Replace each character of the sensitive text with a mask character (`*` by default).                                |
| `TRUNCATE`            | Replace all but a few characters of the sensitive text with a truncation character (`*` by default).                |
| `ABBREVIATE`          | Replace a person's name with its initials (for example, `George Washington` becomes `GW`).                          |
| `RANDOM_REPLACE`      | Replace the sensitive text with a similar, random value.  |
| `STATIC_REPLACE`      | Replace the sensitive text with a given value.            |
| `CRYPTO_REPLACE`      | Replace the sensitive text with its encrypted value.      |
| `HASH_SHA256_REPLACE` | Replace the sensitive text with its SHA256 hash value.    |
| `FPE_ENCRYPT_REPLACE` | Replace the sensitive text with a value generated by [format-preserving encryption](../../filter_strategies.md#fpe) (FPE) |

### Conditions

Each filter strategy may have one condition. See [Conditions](#conditions) for details.

| Conditional  | Description                                                              | Operators                          |
|--------------|--------------------------------------------------------------------------|------------------------------------|
| `TOKEN`      | Compares the value of the sensitive text.                                | `==` , `!=`                        |
| `CONTEXT`    | Compares the filtering context.                                          | `==` , `!=`                        |
| `CONFIDENCE` | Compares the confidence in the sensitive text against a threshold value. | `<` , `<=`, `>` , `>=`, `==`, `!=` |

## Example Policy

```
{
   "name": "ner-example",
   "identifiers": {
      "pheye": {
        "phEyeConfiguration": {
            "endpoint": "http://localhost:18080"
        },
        "pheyeFilterStrategies": [
           {
              "strategy": "REDACT",
              "redactionFormat": "{{{REDACTED-%t}}}"
           }
        ]
      }
   }
}
```

## Local inference

By default the filter calls a remote Ph-Eye service over HTTP at its `endpoint`. It can instead run a
GLiNER model on-device by setting `modelPath` on the `phEyeConfiguration` to a local model directory
(the ONNX model, the tokenizer, and `gliner_config.json`). When `modelPath` is set, detection runs
locally and the `endpoint` is not used.

Local inference is provided by the optional
[`phileas-pheye-onnx`](https://github.com/philterd/phileas-pheye-onnx) module (ONNX Runtime), which
is not a dependency of core Phileas. Add it to your build to enable local inference. If `modelPath`
is set but that module is not on the classpath, Phileas fails fast while the policy's filters are
built (for example when you call `prepare(policy)`, or on the first `filter()` call for the policy),
rather than silently falling back to the remote service or failing part-way through a document. The
failure is a `MissingPhEyeProviderException` with a logged error naming the missing
`phileas-pheye-onnx` dependency, so loading the policy up front surfaces the problem before any text
is processed.

```
{
   "name": "ner-local-example",
   "identifiers": {
      "pheye": {
        "phEyeConfiguration": {
            "modelPath": "/models/ph-eye-pii-en-small"
        },
        "pheyeFilterStrategies": [
           {
              "strategy": "REDACT",
              "redactionFormat": "{{{REDACTED-%t}}}"
           }
        ]
      }
   }
}
```
