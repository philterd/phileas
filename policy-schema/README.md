# Phileas Policy JSON Schema

A [JSON Schema (Draft 2020-12)](https://json-schema.org/) for Phileas redaction policy files. It covers all supported filter types, strategies, and configuration options.

Use it for validation in CI, editor autocompletion, or as a reference for the policy format.

## Versioning

The schema is versioned. The version appears both in the schema's `$id` URL and in a top-level `version` field, for example:

```json
{
  "$id": "https://www.philterd.ai/schemas/redaction-policy/1.0.0/schema.json",
  "version": "1.0.0"
}
```

There is a one-to-one relationship between a Phileas release and the schema version it understands: a given build of Phileas supports exactly one schema version, the version of the schema bundled with that build. Any backward-incompatible change to the schema is a new version.

This file is the canonical source. It is embedded into the Phileas jar at build time so the runtime reads the same schema it was built against.

## Reading the supported version from Phileas

The version is exposed at runtime through the `ai.philterd.phileas.policy.PolicySchema` API, which reads the schema embedded in the jar:

```java
import ai.philterd.phileas.policy.PolicySchema;

String version = PolicySchema.getSupportedSchemaVersion(); // e.g. "1.0.0"
String schema  = PolicySchema.getSchema();                 // the full schema JSON
```

## Editor Support

Add this to the top of your policy JSON file to enable autocompletion and inline validation in supported editors (VS Code, IntelliJ, etc.):

```json
{
  "$schema": "https://www.philterd.ai/schemas/redaction-policy/1.0.0/schema.json",
  ...
}
```

## Validating a Policy

Requires Python 3.10+ and the `jsonschema` library:

```bash
pip install jsonschema
python validate-policy.py my-policy.json
```

Validate multiple files at once:

```bash
python validate-policy.py policy1.json policy2.json
```

Use `-q` for quiet mode (exit code only, useful for CI):

```bash
python validate-policy.py -q my-policy.json
```
