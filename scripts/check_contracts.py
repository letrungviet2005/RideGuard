"""Validate canonical schemas, OpenAPI, examples and safety-relevant constraints."""
import copy
import json
from pathlib import Path

import yaml
from jsonschema import Draft202012Validator, FormatChecker, ValidationError
from openapi_spec_validator import validate_spec

ROOT = Path(__file__).resolve().parents[1] / "contracts"


def main():
    for path in ROOT.glob("*.schema.json"):
        schema = json.loads(path.read_text(encoding="utf-8"))
        Draft202012Validator.check_schema(schema)
        validator = Draft202012Validator(schema, format_checker=FormatChecker())
        for example in schema["examples"]:
            validator.validate(example)
        invalid = copy.deepcopy(schema["examples"][0])
        invalid["schema_version"] = 999
        try:
            validator.validate(invalid)
        except ValidationError:
            pass
        else:
            raise AssertionError(f"{path.name}: incompatible version was accepted")
        print(f"Validated {path.name}")
    spec = yaml.safe_load((ROOT / "openapi.yaml").read_text(encoding="utf-8"))
    validate_spec(spec)
    expected = {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"}
    if set(spec["paths"]) != expected:
        raise AssertionError("Proposed endpoints must not be advertised as implemented")
    print("Validated OpenAPI and implemented endpoint boundary")


if __name__ == "__main__":
    main()
