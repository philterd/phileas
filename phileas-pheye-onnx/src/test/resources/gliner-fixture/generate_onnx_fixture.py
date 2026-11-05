#!/usr/bin/env python3
# Copyright 2026 Philterd, LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""
Builds ``model.onnx`` -- the tiny synthetic GLiNER graph that lets ``GlinerFixtureModelTests``
exercise the full local-inference pipeline (real ``InferenceSession.Run`` included) in default
CI, without the 183 MB / 611 MB production model.

This is the "synthetic mock ONNX graph" of the two approaches in issue #308: a hand-built graph
with the exact GLiNER markerV0 signature that returns *deterministic* logits. It verifies the C#
tensor feed/fetch wiring and the decode against a known graph; it does NOT reproduce the real
weights -- that parity stays in the gated ``[ModelFact]`` ``GlinerModelTests``.

Signature (matches GlinerModel.BuildInputs / GlinerInputs.ToFeeds):
    inputs : input_ids[1,S] attention_mask[1,S] words_mask[1,S] text_lengths[1,1]
             span_idx[1,P,2] (all int64) and span_mask[1,P] (bool)
    output : logits[1, num_words, max_width, num_labels] (float)

Determinism: the graph emits +10 (sigmoid ~= 0.99995) at the span (word 0, width 1 -> words 0..1)
and +6 (sigmoid ~= 0.9975) at the span it contains (word 0, width 0 -> word 0), with -10
(sigmoid ~= 0.00005) everywhere else. On a >=2 word input the greedy non-overlapping decode keeps
the wider span (label 0) and drops the contained one, so Find() yields exactly that two-word span
under the first label, at its character offsets -- a fixed result the test asserts, which also
exercises overlap resolution. The dynamic output dims are derived from the inputs:
    num_words  = (span_idx length P) / max_width        [max_width is the constant below]
    num_labels = count of the <<ENT>> id in input_ids   [one <<ENT>> precedes each label]
All six inputs are referenced by the graph (the four not needed for shape are summed and
multiplied by zero), so ONNX Runtime requires every feed -- a missing or mistyped feed fails Run.

Usage:
    pip install onnx
    python generate_onnx_fixture.py

Writes ``model.onnx`` next to this script. Regenerate and commit if the signature changes.
"""

import os

import onnx
from onnx import TensorProto, helper, numpy_helper
import numpy as np

# Must match gliner_config.json in this directory (and the real ph-eye-pii-base config).
MAX_WIDTH = 12
ENT_ID = 128002  # class_token_index; <<ENT>> precedes each label in the prompt.
OPSET = 18

I64 = TensorProto.INT64


def const(name, array):
    return helper.make_node("Constant", [], [name], value=numpy_helper.from_array(array, name))


def main() -> None:
    nodes = []

    # --- num_labels = number of <<ENT>> ids in input_ids -------------------------------------
    nodes.append(const("ent_id", np.array([ENT_ID], dtype=np.int64)))
    nodes.append(helper.make_node("Equal", ["input_ids", "ent_id"], ["is_ent"]))
    nodes.append(helper.make_node("Cast", ["is_ent"], ["is_ent_i64"], to=I64))
    nodes.append(helper.make_node("ReduceSum", ["is_ent_i64"], ["num_labels_scalar"], keepdims=0))
    nodes.append(const("axis0", np.array([0], dtype=np.int64)))
    nodes.append(helper.make_node("Unsqueeze", ["num_labels_scalar", "axis0"], ["num_labels"]))

    # --- num_words = span_idx length / max_width ---------------------------------------------
    nodes.append(helper.make_node("Shape", ["span_idx"], ["span_shape"]))  # [1, P, 2]
    nodes.append(const("idx1", np.array([1], dtype=np.int64)))
    nodes.append(helper.make_node("Gather", ["span_shape", "idx1"], ["num_spans"], axis=0))  # [P]
    nodes.append(const("max_width", np.array([MAX_WIDTH], dtype=np.int64)))
    nodes.append(helper.make_node("Div", ["num_spans", "max_width"], ["num_words"]))

    # --- output shape [1, num_words, max_width, num_labels] ----------------------------------
    nodes.append(const("one", np.array([1], dtype=np.int64)))
    nodes.append(helper.make_node(
        "Concat", ["one", "num_words", "max_width", "num_labels"], ["out_shape"], axis=0))

    # --- base filled with -10, then two overlapping firing spans scattered in -----------------
    # Output index layout is [batch, word, width, label]. Fire the wider span (word 0, width 1 ->
    # words 0..1) at +10 and the span it contains (word 0, width 0 -> word 0) at +6. Both indices
    # are always in bounds: max_width is 12 so width 1 always exists, and word 0 / label 0 always
    # exist whenever Find runs (>=1 word, >=1 label). For an input of >=2 words the greedy
    # non-overlapping decode keeps the wider span and drops the contained one, so the fixture
    # exercises threshold gating, span->offset mapping, label selection, AND overlap resolution
    # end to end. (For a 1-word input the wider span is out of range and the decode skips it,
    # leaving the single-word span -- still well defined.)
    nodes.append(helper.make_node(
        "ConstantOfShape", ["out_shape"], ["base"],
        value=numpy_helper.from_array(np.array([-10.0], dtype=np.float32), "neg_val")))
    nodes.append(const("scatter_idx", np.array([[0, 0, 1, 0], [0, 0, 0, 0]], dtype=np.int64)))
    nodes.append(const("scatter_upd", np.array([10.0, 6.0], dtype=np.float32)))
    nodes.append(helper.make_node(
        "ScatterND", ["base", "scatter_idx", "scatter_upd"], ["scattered"]))

    # --- thread the remaining inputs through a multiply-by-zero so all feeds are required -----
    zero_terms = []
    for src in ["input_ids", "attention_mask", "words_mask", "text_lengths", "span_idx"]:
        nodes.append(helper.make_node("ReduceSum", [src], [f"sum_{src}"], keepdims=0))
        zero_terms.append(f"sum_{src}")
    nodes.append(helper.make_node("Cast", ["span_mask"], ["span_mask_i64"], to=I64))
    nodes.append(helper.make_node("ReduceSum", ["span_mask_i64"], ["sum_span_mask"], keepdims=0))
    zero_terms.append("sum_span_mask")

    acc = zero_terms[0]
    for i, term in enumerate(zero_terms[1:]):
        out = f"acc_{i}" if i < len(zero_terms) - 2 else "sum_all"
        nodes.append(helper.make_node("Add", [acc, term], [out]))
        acc = out
    nodes.append(helper.make_node("Cast", ["sum_all"], ["sum_all_f"], to=TensorProto.FLOAT))
    nodes.append(const("zero_mul", np.array(0.0, dtype=np.float32)))
    nodes.append(helper.make_node("Mul", ["sum_all_f", "zero_mul"], ["zero"]))
    nodes.append(helper.make_node("Add", ["scattered", "zero"], ["logits"]))

    inputs = [
        helper.make_tensor_value_info("input_ids", I64, [1, "S"]),
        helper.make_tensor_value_info("attention_mask", I64, [1, "S"]),
        helper.make_tensor_value_info("words_mask", I64, [1, "S"]),
        helper.make_tensor_value_info("text_lengths", I64, [1, 1]),
        helper.make_tensor_value_info("span_idx", I64, [1, "P", 2]),
        helper.make_tensor_value_info("span_mask", TensorProto.BOOL, [1, "P"]),
    ]
    outputs = [helper.make_tensor_value_info(
        "logits", TensorProto.FLOAT, [1, "num_words", MAX_WIDTH, "num_labels"])]

    graph = helper.make_graph(nodes, "gliner_fixture", inputs, outputs)
    model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", OPSET)])
    model.ir_version = 9  # within ONNX Runtime 1.26's supported IR range
    onnx.checker.check_model(model)

    out_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "model.onnx")
    onnx.save(model, out_path)
    print(f"Wrote {out_path} ({os.path.getsize(out_path)} bytes)")


if __name__ == "__main__":
    main()
