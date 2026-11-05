# GLiNER parity fixture

A tiny, deterministic fixture that lets `LocalPhEyeDetectorParityTest` exercise the full local
inference pipeline (real ONNX Runtime `session.run` and the real DJL tokenizer) in default CI,
without the multi-hundred-MB production model.

Files:

- `model.onnx` (~2 KB): a hand-built synthetic GLiNER graph with the exact markerV0 signature the
  detector feeds (`input_ids`, `attention_mask`, `words_mask`, `text_lengths`, `span_idx` as int64,
  `span_mask` as bool; output `logits[1, num_words, max_width, num_labels]`). It returns
  deterministic logits: `+10` at the span (word 0, width 1, label 0) and `+6` at the span it
  contains (word 0, width 0, label 0), `-10` everywhere else. `num_words` is derived from
  `span_idx` length / `max_width`, and `num_labels` from the count of the `<<ENT>>` id (128002) in
  `input_ids`, so the graph adapts to any input the detector builds. On a 2+ word input the greedy
  non-overlapping decode keeps the wider span, which exercises threshold gating, span-to-offset
  mapping, label selection, and overlap resolution end to end. This is the same synthetic graph used
  by the phileas-dotnet parity tests; it is language-neutral.
- `tokenizer.json` (~8 MB): the real `ph-eye-pii-en` deberta-v3 fast tokenizer (with `<<ENT>>`=128002
  and `<<SEP>>`=128003 as added tokens), so tokenization is exercised against the production
  tokenizer rather than a mock. The synthetic model's output does not depend on the ordinary word
  token ids, only on the `<<ENT>>` count, so the real tokenizer pairs correctly with the synthetic
  model.
- `gliner_config.json`: the fields `GlinerConfig` reads (`max_width`, `max_len`, `ent_token`,
  `sep_token`, `words_splitter_type`).

## Regenerating `model.onnx`

The graph is produced by `generate_onnx_fixture.py` (committed alongside this file). It requires
only the `onnx` Python package:

```sh
pip install onnx
python generate_onnx_fixture.py
```

Regenerate and commit if the detector's ONNX signature changes.

## What this fixture does and does not cover

It verifies the detector's tensor feed/fetch wiring, the words-mask and span enumeration, the
decode (sigmoid, threshold, greedy non-overlap), and the word-to-character offset mapping, against
a known graph. It does **not** reproduce the real model's weights. Numerical parity against the real
GLiNER weights (and the no-entity negative case) belongs in a separate test gated on a real model
directory, since the synthetic graph fires deterministically on any non-empty input.
