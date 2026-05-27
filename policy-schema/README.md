# Phileas Policy JSON Schema

A [JSON Schema (Draft 2020-12)](https://json-schema.org/) for Phileas redaction policy files. It covers all supported filter types, strategies, and configuration options.

Use it for validation in CI, editor autocompletion, or as a reference for the policy format.

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
