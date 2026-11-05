# Model Directory

Local inference loads a GLiNER model from a directory on disk. The `modelPath` you set on a PhEye filter (see [Configuration](configuration.md)) points at that directory.

## Required files

The directory is the layout produced by `ph-eye-model-training`'s `./hub/publish.sh`. It contains:

| File | Purpose |
|---|---|
| `model.onnx` (or `onnx/model.onnx`) | The exported GLiNER model. |
| `tokenizer.json` | The HuggingFace fast tokenizer. |
| `gliner_config.json` | Span width, maximum length, and prompt tokens. |

`model.onnx` may sit at the top level of the directory or under an `onnx/` subdirectory; both layouts are recognized.

## Where to get a model

The published PhEye PII name models on the Hugging Face Hub follow this layout. For example, `philterd/ph-eye-pii-en-small`, `philterd/ph-eye-pii-en-medium`, and `philterd/ph-eye-pii-en-large` each provide the ONNX export, tokenizer, and config. Download a model into a local directory and point `modelPath` at it.

## How the files are used

`gliner_config.json` drives the inference pipeline: it provides the maximum span width, the maximum sequence length, and the prompt tokens used to build the model input. `tokenizer.json` is loaded as the fast tokenizer that splits text into subtokens. `model.onnx` is run by ONNX Runtime. See [How It Works](how-it-works.md) for how these come together in the GLiNER pipeline.
