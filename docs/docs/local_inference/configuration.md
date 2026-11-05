# Configuration

Local inference is enabled per PhEye filter by setting a `modelPath`. This is the single switch between remote (HTTP) and local (in-process) detection.

## Enabling local inference

Set `modelPath` on a PhEye filter to the [model directory](model-directory.md) you want to load. In a redaction policy:

```json
{
  "phEyeConfiguration": {
    "modelPath": "/models/ph-eye-pii-en-small",
    "labels": ["person"]
  }
}
```

- `modelPath`: the path to a local model directory. When set, Phileas runs inference in-process using this module. When omitted, Phileas calls a remote PhEye service as before.
- `labels`: the entity labels the model should detect, for example `person`.

In [PhiSQL](https://www.philterd.ai/phisql/), the equivalent is the `MODEL` clause:

```sql
DETECT PHEYE MODEL '/models/ph-eye-pii-en-small'
```

## How detection runs

With `modelPath` set, `PhEyeFilter` discovers this module's `LocalPhEyeDetectorProvider` through `java.util.ServiceLoader`, builds a `LocalPhEyeDetector` for the model directory, and runs inference locally. The detector returns spans; the filter then applies its per-label confidence thresholds and replacement strategies on top, exactly as for remote detections. See [How It Works](how-it-works.md) for the discovery mechanism and the inference pipeline.

## Threads

Each model runs with as many ONNX Runtime intra-op threads as `Runtime.availableProcessors()` reports, which follows a container's CPU set and CPU quota. Idle threads do not busy-wait. To use a different count, set the `PHEYE_ONNX_THREADS` environment variable or the `pheye.onnx.threads` system property, which takes precedence, to a positive whole number. Any other value fails the model's load with an `IllegalArgumentException` naming the setting.

More threads lower the time one detection takes; fewer give more throughput when many detections run at once, especially under a CPU quota, where concurrent detections with several threads each exceed the quota and are throttled. A model directory is loaded once per process, so the setting applies to every policy that names it.

## GPU

The model runs on the CPU unless `PHEYE_ONNX_DEVICE`, or the `pheye.onnx.device` system property, which takes precedence, is `cuda` (device 0) or `cuda:<n>`. `cpu` is the default. Any other value fails the model's load with an `IllegalArgumentException` naming the setting.

CUDA also needs:

- ONNX Runtime's GPU package. This module depends on `com.microsoft.onnxruntime:onnxruntime`, which is CPU only; replace it in your build with `com.microsoft.onnxruntime:onnxruntime_gpu` of the same version, by excluding `onnxruntime` from this dependency and adding `onnxruntime_gpu`. The GPU package has native libraries for Linux x64 and Windows x64 only, and is about 526 MB against 89 MB, so it is not the default. It also runs on the CPU.
- The CUDA and cuDNN libraries ONNX Runtime's GPU package is built for, on a host with an NVIDIA GPU. Version 1.20.0 ran with CUDA 12.6 and cuDNN 9 (the `nvidia/cuda:12.6.3-cudnn-runtime-ubuntu24.04` image).

When CUDA is asked for but cannot be used, because ONNX Runtime is the CPU package or the CUDA libraries or device are missing, a warning is logged and the model runs on the CPU. The device in use is logged when the model loads, for example `Loaded the PhEye model in /models/ph-eye-pii-en-xsmall on cuda:0.`

Tokenizing runs on the CPU in either case. DJL's tokenizer otherwise downloads a CUDA build of its native library from publish.djl.ai on a host where it finds CUDA, which fails without network access, so this module sets `RUST_FLAVOR` to `cpu` when neither the environment variable nor the system property is set, and DJL loads the CPU build it bundles.

## Thresholds

Confidence thresholds and replacement strategies are configured on the PhEye filter the same way regardless of where detection runs, so a policy written for remote PhEye keeps the same threshold and strategy settings when you switch it to a local model. Each published model has a recommended confidence threshold; use the value documented on the model you load.
