#!/usr/bin/env python3
"""Validate a Phileas redaction policy JSON file against the schema."""

import argparse
import json
import sys
from pathlib import Path

try:
    from jsonschema import Draft202012Validator, ValidationError
except ImportError:
    print("Error: jsonschema is required.  Install it with:  pip install jsonschema", file=sys.stderr)
    sys.exit(2)

SCHEMA_PATH = Path(__file__).resolve().parent / "redaction-policy-schema.json"


def load_json(path: Path) -> dict:
    with open(path) as f:
        return json.load(f)


def validate(policy_path: Path, schema_path: Path, *, quiet: bool = False) -> list[ValidationError]:
    schema = load_json(schema_path)
    policy = load_json(policy_path)

    validator = Draft202012Validator(schema)
    errors = sorted(validator.iter_errors(policy), key=lambda e: list(e.absolute_path))

    if not quiet:
        if errors:
            print(f"{policy_path}: {len(errors)} validation error(s)\n")
            for i, error in enumerate(errors, 1):
                path = ".".join(str(p) for p in error.absolute_path) or "(root)"
                print(f"  {i}. [{path}] {error.message}")
            print()
        else:
            print(f"{policy_path}: valid")

    return errors


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("policy", nargs="+", type=Path, help="Path(s) to policy JSON file(s) to validate")
    parser.add_argument("--schema", type=Path, default=SCHEMA_PATH, help=f"Path to the JSON schema (default: {SCHEMA_PATH})")
    parser.add_argument("-q", "--quiet", action="store_true", help="Suppress output; exit code only")
    args = parser.parse_args()

    if not args.schema.exists():
        print(f"Error: schema not found at {args.schema}", file=sys.stderr)
        sys.exit(2)

    failed = 0
    for policy_path in args.policy:
        if not policy_path.exists():
            print(f"Error: {policy_path} not found", file=sys.stderr)
            failed += 1
            continue
        try:
            errors = validate(policy_path, args.schema, quiet=args.quiet)
            if errors:
                failed += 1
        except json.JSONDecodeError as e:
            print(f"Error: {policy_path} is not valid JSON: {e}", file=sys.stderr)
            failed += 1

    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
