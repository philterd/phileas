# Local PhEye Inference

`phileas-pheye-onnx` adds optional local, on-device GLiNER inference to the Phileas PhEye filter, using ONNX Runtime. It lets Phileas run a GLiNER model in-process, with no separate PhEye server.

Core Phileas talks to a [PhEye](https://www.philterd.ai/ph-eye/) service over HTTP. This module lets Phileas load and run the model itself instead. It is a separate artifact that `ai.philterd:phileas` does not include, so that only applications that want local inference pull in the native ONNX Runtime and tokenizer dependencies.

## When to use local vs. remote PhEye

Phileas supports two ways to run PhEye detection. Both produce the same kind of detections; they differ in where the model runs.

- **Remote PhEye (default).** Phileas calls a PhEye HTTP service. Use this when you run PhEye as a shared service, want to scale model serving independently, or run the model on dedicated hardware (for example a GPU host) separate from the application.
- **Local PhEye (this module).** Phileas loads a GLiNER model from a local directory and runs inference in-process. Use this when you want a single self-contained process, have no PhEye server to call, or need inference to stay entirely inside one application boundary with no network hop.

You select local inference per filter by setting a `modelPath` (see [Configuration](configuration.md)). With no `modelPath`, Phileas uses the remote HTTP detector exactly as before, so adding this module changes nothing until you point a filter at a model directory.

## Installing

Add the dependency alongside `phileas` or `phileas-core`, at the same version:

```xml
<dependency>
    <groupId>ai.philterd</groupId>
    <artifactId>phileas-pheye-onnx</artifactId>
    <version>4.6.0</version>
</dependency>
```

This module requires Java 25, while the other Phileas modules require Java 17.

Adding the artifact to the classpath is all that is needed to enable local inference. The module registers its detector provider through `java.util.ServiceLoader`, so Phileas discovers it automatically. See [How It Works](how-it-works.md) for the mechanism.

Through version 1.1.0 this module was versioned separately, in its own repository. See [Modules](../modules.md#moving-from-phileas-pheye-onnx-1x).

## Next steps

- [How It Works](how-it-works.md): the SPI, service discovery, and the GLiNER pipeline this module reimplements.
- [Model Directory](model-directory.md): the files a local model directory must contain.
- [Configuration](configuration.md): pointing a PhEye filter at a local model.
- [Limitations and Accuracy](limitations.md): the current parity status, and what to confirm before production use.
