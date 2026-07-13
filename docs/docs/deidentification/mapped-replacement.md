# Mapped Replacement

Mapped replacement replaces a detected value with a replacement drawn from a lookup table you provide. It uses the `MAP_REPLACE` filter strategy and is useful when specific values have known, deterministic replacements: for example, mapping vendor names to code names, or account identifiers to non-sensitive tokens.

A `MAP_REPLACE` strategy resolves a replacement in this order:

1. **Lookup table.** If the detected value is a key in the strategy's lookup table, its mapped value is used.
2. **Generator.** If the value is not in the table and a `generator` is configured, the generator produces a replacement, which is validated before use (see [Generator output validation](#generator-output-validation)).
3. **Fallback strategy.** If the value is not in the table and no generator produces an accepted value (none is configured, or it fails, times out, or returns invalid or rejected output), the strategy's `fallbackStrategy` is applied. A detected value is never left in the clear.

Within a `CONTEXT`-scoped strategy (`replacementScope` set to `CONTEXT`), a repeated value resolves to the same replacement it received earlier in the context, and the generator is not invoked again for it.

## Lookup table

The lookup table is built from two sources, with inline `mappings` taking precedence over entries loaded from `mappingFiles`:

* `mappings` is an inline object of key/value pairs.
* `mappingFiles` is a list of local TSV file paths (one tab-delimited key/value pair per row) merged into the table when the policy is loaded.

By default keys are matched case-insensitively. Set `caseSensitive` to `true` to match keys exactly.

```json
{
  "strategy": "MAP_REPLACE",
  "mappings": {
    "Acme Corp": "Widget Co",
    "Globex": "Initech"
  },
  "mappingFiles": [ "/etc/philter/vendors.tsv" ],
  "caseSensitive": false,
  "fallbackStrategy": "REDACT"
}
```

## Fallback strategy

`fallbackStrategy` is a terminal strategy (`REDACT`, `RANDOM_REPLACE`, `STATIC_REPLACE`, `CRYPTO_REPLACE`, `FPE_ENCRYPT_REPLACE`, `HASH_SHA256_REPLACE`, `LAST_4`, `MASK`, `TRUNCATE`, or `ABBREVIATE`) applied to values that are absent from the lookup table. It defaults to `REDACT`. The fallback reuses the same strategy's other properties (for example `staticReplacement` for `STATIC_REPLACE`, or `maskCharacter` for `MASK`).

## Generators

A generator produces a replacement for a detected value that is absent from the lookup table. Generators are declared once in the policy's top-level `generators` block and referenced by name from a `MAP_REPLACE` strategy's `generator` property.

A generator targets a local model endpoint inside your deployment boundary, so detected values are not sent to a third party.

```json
{
  "generators": {
    "vendor-namer": {
      "type": "ollama",
      "endpoint": "http://localhost:11434",
      "model": "llama3.1",
      "prompt": "Return a fictional company name to replace {{token}}.",
      "timeoutMs": 2000
    }
  }
}
```

| Property | Description |
| --- | --- |
| `type` | Generator backend. `ollama` calls a local Ollama-compatible `/api/generate` endpoint. |
| `endpoint` | Base URL of the local generator endpoint. Must resolve inside your deployment boundary. |
| `model` | Model name the endpoint should use. |
| `prompt` | Prompt template. `{{token}}` is replaced with the detected value and `{{label}}` with its entity label. The model should return only the replacement value. |
| `timeoutMs` | Maximum time to wait for the generator before applying the strategy's `fallbackStrategy`. Required, so a generator can never block the pipeline indefinitely. |

A `MAP_REPLACE` strategy references the generator by name:

```json
{
  "strategy": "MAP_REPLACE",
  "mappings": { "Acme Corp": "Widget Co" },
  "generator": "vendor-namer",
  "fallbackStrategy": "REDACT"
}
```

## Generator output validation

Because a generator's output is not fixed in advance, each generated value is validated before it is used. A value is rejected, and the strategy applies its `fallbackStrategy`, when it:

* is blank,
* equals the original detected value (after case normalization), or
* is found, when re-scanned against the policy's filters, to contain detectable sensitive information.

The re-scan runs the same policy's filters over the generated value, so a generator that reintroduces sensitive information (for example returning text that itself contains an email address) is rejected rather than used. This helps prevent a generator from leaving a value effectively unredacted or introducing new sensitive information into the output. Detection is probabilistic; validate output against your own data.
