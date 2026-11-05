# Limitations and Accuracy

This module is a port of the GLiNER reference algorithm, and local inference is parity-tested against the Python `gliner` model.

## Parity status

The detector is verified two ways. `LocalPhEyeDetectorParityTest` runs it against a tiny synthetic ONNX fixture (with real ONNX Runtime and the real DJL tokenizer) and asserts the deterministic spans, exercising the tensor wiring, the words-mask and span enumeration, the sigmoid/threshold/greedy-non-overlap decode, and the word-to-character offset mapping. `LocalPhEyeDetectorRealModelParityTest` runs it against a real exported GLiNER model (provided via `PHILEAS_GLINER_MODEL_DIR`) and asserts its spans match the Python `gliner.predict_entities` reference exactly: same offsets and labels, scores within tolerance. Both pass.

Verifying parity surfaced and fixed a real bug: `span_mask` is a boolean tensor in the GLiNER ONNX signature, and the detector had been feeding it as int64, which ONNX Runtime rejects. The detector now reproduces the Python reference on real weights.

## Accuracy

Detection with these models is probabilistic. A name detector will miss some names and flag some non-names, and accuracy depends on how close your text is to the data the model was trained on. Local inference does not change a model's accuracy: it runs the same model the remote PhEye service would, so the same calibration and recommended thresholds apply. Validate any model on your own text and set thresholds accordingly. You remain responsible for the personal data you process.
