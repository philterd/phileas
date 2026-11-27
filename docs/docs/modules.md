# Modules

Phileas is published as several Maven artifacts under the `ai.philterd` group. All of them share one version.

| Artifact | Use it for | Brings in |
|---|---|---|
| `phileas` | Text and PDF redaction. This is the default choice. | `phileas-core` and `phileas-pdf`. It holds no code of its own. |
| `phileas-core` | Text redaction only. | The redaction engine, with no PDFBox. |
| `phileas-pdf` | PDF redaction. | `phileas-core` and PDFBox. |
| `phileas-ooxml` | [Word and Excel redaction](word_and_excel.md). | `phileas-core` and the Apache POI package layer. |
| `phileas-rtf` | [RTF redaction](rtf.md). | `phileas-core` only. |
| `phileas-email` | [Email redaction](email.md) (`.eml` and Outlook `.msg`). | `phileas-pdf`, `phileas-ooxml`, `phileas-rtf`, Jakarta Mail (Eclipse Angus), and Apache POI's HSMF. |
| `phileas-pheye-onnx` | [Local PhEye inference](local_inference/index.md), added alongside `phileas` or `phileas-core`. | ONNX Runtime and the DJL tokenizers. |

`phileas` does not include `phileas-ooxml`, which adds Apache POI, so an application that redacts Word and Excel documents depends on it as well, with the same version. The same holds for `phileas-rtf` and `phileas-email`. `phileas` also does not include `phileas-pheye-onnx`, so the ONNX Runtime and tokenizer native libraries reach an application only if it asks for them. `phileas-pheye-onnx` declares `phileas-core` with `provided` scope, so the application must depend on `phileas` or `phileas-core` itself. Use the same version for all of them.

`phileas-parent` is the parent POM of the others. Applications do not depend on it.

`PdfFilterService` is in `phileas-pdf`, `OoxmlFilterService` in `phileas-ooxml`, `RtfFilterService` in `phileas-rtf`, and `EmailFilterService` in `phileas-email`, in the package `ai.philterd.phileas.services.filters.filtering`, which `phileas-core` also uses. This has no effect on the classpath, but these jars cannot be used together on the Java module path.

## Moving from phileas-pheye-onnx 1.x

Through version 1.1.0, `phileas-pheye-onnx` was versioned separately, in its own repository, and each release targeted one Phileas version. It now has the same version as Phileas, starting at 4.6.0. The coordinate does not change, and there is no version between 1.1.0 and 4.6.0.

To upgrade, set `phileas-pheye-onnx` to the version of `phileas` you use:

```xml
<dependency>
    <groupId>ai.philterd</groupId>
    <artifactId>phileas</artifactId>
    <version>4.6.0</version>
</dependency>
<dependency>
    <groupId>ai.philterd</groupId>
    <artifactId>phileas-pheye-onnx</artifactId>
    <version>4.6.0</version>
</dependency>
```
