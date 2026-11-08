"""Offline checks of canonical contracts, examples and documented MVP boundaries.

Uses existing dependencies only. No service startup or network reference retrieval.
Cross-row DB semantics, timestamp-to-now validation and client-header/body coupling still
need implementation tests; these checks do not claim to prove those runtime invariants.
"""
import copy
import json
import re
from functools import lru_cache
from pathlib import Path
from urllib.parse import unquote

import yaml
from jsonschema import Draft202012Validator, FormatChecker
from openapi_spec_validator import validate_spec

REPO = Path(__file__).resolve().parents[1]
ROOT = REPO / "contracts"
METHODS = {"get", "post", "put", "patch", "delete", "head", "options"}
CHECKER = FormatChecker()
COUNTS = {"examples": 0, "negative_cases": 0, "operations": 0, "local_links": 0}


class UniqueLoader(yaml.SafeLoader):
    """Fail on duplicate YAML keys instead of silently losing paths/properties."""


def unique_mapping(loader, node, deep=False):
    result = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if key in result:
            raise AssertionError(f"Duplicate YAML key: {key}")
        result[key] = loader.construct_object(value_node, deep=deep)
    return result


UniqueLoader.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, unique_mapping)


@lru_cache(maxsize=None)
def load(path):
    return yaml.load(path.read_text(encoding="utf-8"), Loader=UniqueLoader)


def expand(value, source, stack=()):
    """Resolve local refs in memory so Windows file URIs never trigger network loaders."""
    if isinstance(value, list):
        return [expand(item, source, stack) for item in value]
    if not isinstance(value, dict):
        return value
    if "$ref" in value:
        location, _, fragment = value["$ref"].partition("#")
        if ":" in location or location.startswith("/"):
            raise AssertionError(f"Only relative/local refs allowed: {value['$ref']}")
        target = (source.parent / unquote(location)).resolve() if location else source
        if not target.is_relative_to(ROOT):
            raise AssertionError(f"Reference escapes contracts/: {target}")
        marker = (target, fragment)
        if marker in stack:
            raise AssertionError(f"Unexpected cyclic reference: {marker}")
        document = load(target)
        if fragment:
            if not fragment.startswith("/"):
                raise AssertionError(f"Unsupported JSON pointer: {fragment}")
            for part in fragment[1:].split("/"):
                part = unquote(part).replace("~1", "/").replace("~0", "~")
                document = document[int(part)] if isinstance(document, list) else document[part]
        resolved = expand(document, target, stack + (marker,))
        siblings = expand({key: val for key, val in value.items() if key != "$ref"}, source, stack)
        # Current refs only add annotations; do not silently drop conflicting constraints.
        for key, val in siblings.items():
            if key in resolved and resolved[key] != val and key not in ("description", "summary"):
                raise AssertionError(f"Conflicting $ref sibling {key}")
        return {**resolved, **siblings}
    return {key: expand(val, source, stack) for key, val in value.items()}


def validate_example(schema, example, label):
    Draft202012Validator.check_schema(schema)
    try:
        Draft202012Validator(schema, format_checker=CHECKER).validate(example)
    except Exception as error:
        raise AssertionError(f"Invalid example: {label}: {error}") from error
    COUNTS["examples"] += 1


def reject(schema, instance, label):
    errors = list(Draft202012Validator(schema, format_checker=CHECKER).iter_errors(instance))
    if not errors:
        raise AssertionError(f"Unsafe/invalid request accepted: {label}")
    COUNTS["negative_cases"] += 1


def walk_schema(schema, direction):
    if not isinstance(schema, dict):
        return
    for key, prop in schema.get("properties", {}).items():
        if direction == "request" and prop.get("readOnly"):
            raise AssertionError(f"readOnly property in request schema: {key}")
        if direction == "response" and (prop.get("writeOnly") or key in
                {"password", "password_hash", "token_hash", "refresh_token_hash", "reset_token", "hardware_secret"}):
            raise AssertionError(f"Secret/writeOnly response property: {key}")
        walk_schema(prop, direction)
    if schema.get("type") == "object" and schema.get("additionalProperties") is not False:
        raise AssertionError("Contract object must reject unknown properties")
    for keyword in ("oneOf", "anyOf", "allOf"):
        for branch in schema.get(keyword, []):
            walk_schema(branch, direction)
    walk_schema(schema.get("items"), direction)


def check_media(media, direction, label):
    schema = media["schema"]
    walk_schema(schema, direction)
    examples = ([media["example"]] if "example" in media else [])
    examples += [item["value"] for item in media.get("examples", {}).values()]
    if not examples:
        raise AssertionError(f"Missing example: {label}")
    for example in examples:
        validate_example(schema, example, label)


def expected_operations():
    routes = {
        "/auth/register": "post", "/auth/login": "post", "/auth/refresh": "post",
        "/auth/logout": "post", "/auth/forgot-password": "post", "/auth/reset-password": "post",
        "/users/me": "get patch", "/users/me/change-password": "post",
        "/users/me/sessions": "get", "/users/me/sessions/{session_id}": "delete",
        "/devices": "get", "/devices/{device_id}": "get patch", "/devices/bind": "post",
        "/devices/{device_id}/binding": "delete", "/device-bindings": "get",
        "/devices/{device_id}/config": "get patch", "/devices/{device_id}/config/ack": "post",
        "/devices/{device_id}/reports": "post", "/feedbacks": "get post",
        "/feedbacks/{feedback_id}": "get", "/admin/users": "get",
        "/admin/users/{user_id}/status": "patch", "/admin/devices": "get post",
        "/admin/devices/{device_id}/status": "patch", "/admin/feedbacks": "get",
        "/admin/feedbacks/{feedback_id}": "patch",
    }
    return {(f"/api/v1{path}", method) for path, methods in routes.items() for method in methods.split()}


def check_openapi(path, proposed=False):
    document = load(path)
    spec = expand(document, path)
    validate_spec(spec)  # All local refs resolved above; no network retrieval needed.
    ids, operations = set(), set()
    public = {"register", "login", "forgotPassword", "resetPassword"}
    for route, entry in spec["paths"].items():
        for method, op in entry.items():
            if method not in METHODS:
                continue
            operations.add((route, method))
            oid = op["operationId"]
            if oid in ids:
                raise AssertionError(f"Duplicate operationId: {oid}")
            ids.add(oid)
            COUNTS["operations"] += 1
            if not proposed:
                continue
            assert op["x-status"] in {"proposed", "implemented-unverified", "integration-and-runtime-smoke-tested"}
            security = op.get("security", spec.get("security"))
            if oid in public:
                assert security == [], oid
            elif oid in {"refresh", "logout"}:
                assert op["x-authentication-required"] is True
                assert set(op["x-token-transport"]) == {"mobile", "admin_web"}
                assert security == [{}, {"refreshCookie": [], "csrfProtection": []}]
            else:
                assert security == [{"bearerAuth": []}], oid
                assert "401" in op["responses"] and "403" in op["responses"], oid
            if "/admin/" in route:
                assert op["x-required-role"] == "ADMIN"
            for parameter in op.get("parameters", []):
                if "example" in parameter:
                    validate_example(parameter["schema"], parameter["example"], f"{oid} parameter")
                if parameter["name"] == "limit":
                    assert parameter["schema"]["default"] == 20
                    assert parameter["schema"]["maximum"] == 100
            if "requestBody" in op:
                assert op["requestBody"]["required"] is True
                for media in op["requestBody"]["content"].values():
                    check_media(media, "request", f"{oid} request")
            for status, response in op["responses"].items():
                if status == "204":
                    assert "content" not in response
                for media in response.get("content", {}).values():
                    check_media(media, "response", f"{oid} response {status}")
                    values = ([media["example"]] if "example" in media else [])
                    values += [item["value"] for item in media.get("examples", {}).values()]
                    if status.isdigit() and int(status) >= 400:
                        assert all(v["status"] == int(status) for v in values)
                for header in response.get("headers", {}).values():
                    if "example" in header:
                        validate_example(header["schema"], header["example"], f"{oid} header")
    if proposed:
        assert operations == expected_operations(), operations ^ expected_operations()
        for route, method in (("/api/v1/devices/bind", "post"), ("/api/v1/feedbacks", "post")):
            assert any(p["name"] == "Idempotency-Key" and p["required"]
                       for p in spec["paths"][route][method]["parameters"])
        unbind = spec["paths"]["/api/v1/devices/{device_id}/binding"]["delete"]
        assert any(p["name"] == "expected_binding_id" and p["required"] and p["in"] == "query"
                   for p in unbind["parameters"])
        schemas = spec["components"]["schemas"]
        for route in ("/api/v1/auth/login", "/api/v1/auth/refresh"):
            media = spec["paths"][route]["post"]["responses"]["200"]["content"]["application/json"]
            validate_example(schemas["MobileTokenResponse"], media["examples"]["mobile"]["value"], "mobile token transport")
            validate_example(schemas["WebTokenResponse"], media["examples"]["admin_web"]["value"], "web token transport")
        for route in ("/api/v1/auth/refresh", "/api/v1/auth/logout"):
            media = spec["paths"][route]["post"]["requestBody"]["content"]["application/json"]
            validate_example(schemas["MobileRefreshRequest"], media["examples"]["mobile"]["value"], "mobile credential required")
            validate_example(schemas["WebRefreshRequest"], media["examples"]["admin_web"]["value"], "web cookie body shape")
    else:
        expected = {(p, "get") for p in ("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness")}
        expected |= expected_operations()
        assert operations == expected, "Implemented contract must match health and MVP routes"
        for route, method in expected_operations():
            op = spec["paths"][route][method]
            assert op["x-status"] == "integration-and-runtime-smoke-tested"
            assert op["x-runtime-tested-statuses"]
            assert op["x-verification"]["evidence"] == "docs/testing/backend-verification-2026-11-09.md"
    print(f"Validated {path.name}: {len(operations)} operations and all local refs")
    return spec


def negative_cases(spec):
    s = spec["components"]["schemas"]
    good = {"email": "reader@example.invalid", "full_name": "Reader", "password": "example-password-2026"}
    for field in ("role", "user_id", "status"):
        reject(s["RegisterRequest"], {**good, field: "ADMIN"}, f"register cannot set {field}")
    for password in ("short", "x" * 129, None):
        reject(s["RegisterRequest"], {**good, "password": password}, "password bounds/null")
    patch = {"expected_version": 2, "settings": {"display_brightness": 80}}
    validate_example(s["ConfigPatchRequest"], patch, "partial settings patch")
    for bad in (-1, 101, 1.5, None):
        reject(s["ConfigPatchRequest"], {**patch, "settings": {"display_brightness": bad}}, "brightness")
    for bad in ({}, {"risk_threshold": 3}, {"vibration_enabled": None}):
        reject(s["ConfigPatchRequest"], {**patch, "settings": bad}, "unsafe/empty settings")
    reject(s["ConfigPatchRequest"], {"settings": patch["settings"]}, "missing expected_version")
    reject(s["ConfigPatchRequest"], {**patch, "binding_id": "injected"}, "unknown config field")
    report = copy.deepcopy(s["DeviceReportRequest"]["examples"][0])
    for field, value in (("battery_percent", 101), ("battery_percent", 1.2), ("radar_status", "SAFE"),
                         ("firmware_version", "x" * 65), ("observed_at", "2026-11-07T00:00:00+07:00"),
                         ("latitude", 0)):
        reject(s["DeviceReportRequest"], {**report, field: value}, f"report {field}")
    missing = copy.deepcopy(report)
    del missing["imu_status"]
    reject(s["DeviceReportRequest"], missing, "missing hardware status")
    web_response = copy.deepcopy(spec["paths"]["/api/v1/auth/login"]["post"]["responses"]
                                 ["200"]["content"]["application/json"]["examples"]["admin_web"]["value"])
    reject(s["WebTokenResponse"], {**web_response, "refresh_token": "must-not-be-in-web-response"}, "web body token")
    reject(s["WebRefreshRequest"], {"refresh_token": "body-token-is-mobile-only"}, "web refresh transport")
    reject(s["MobileRefreshRequest"], {}, "missing mobile refresh token")
    for name in ("CreateFeedbackRequest", "RenameDeviceRequest", "ConfigAckRequest"):
        sample = {"CreateFeedbackRequest": {"device_id": None, "category": "BUG", "content": "Example"},
                  "RenameDeviceRequest": {"nickname": None},
                  "ConfigAckRequest": {"binding_id": report["binding_id"], "reported_applied_version": 1}}[name]
        validate_example(s[name], sample, name)
        reject(s[name], {**sample, "user_id": report["binding_id"]}, "ownership injection")


def check_markdown_links():
    roots = [REPO / name for name in ("docs", "contracts", "apps", "ai", "firmware", "hardware", "infra", "tools", "scripts")]
    excluded = {"node_modules", ".venv", "target", "build", ".pio", ".gradle", "dist"}
    pages = list(REPO.glob("*.md"))
    for root in roots:
        pages += [p for p in root.rglob("*.md") if not excluded.intersection(p.relative_to(root).parts)]
    for page in pages:
        text = re.sub(r"```.*?```", "", page.read_text(encoding="utf-8"), flags=re.S)
        for target in re.findall(r"\]\(([^)]+)\)", text):
            target = target.strip().strip("<>").split("#", 1)[0]
            if not target or re.match(r"[A-Za-z][A-Za-z0-9+.-]*:", target):
                continue
            assert (page.parent / unquote(target)).exists(), (page, target)
            COUNTS["local_links"] += 1
    print(f"Validated {COUNTS['local_links']} local Markdown file links (not external URLs/anchors)")


def check_backend_routes(spec):
    """Compare source mappings; this is NOT evidence of PostgreSQL runtime behavior."""
    def normalize(route):
        return re.sub(r"\{[^}]+\}", "{}", route)

    expected = {(method.upper(), normalize(route))
                for route, entry in spec["paths"].items()
                for method, op in entry.items()
                if method in METHODS and op.get("x-status") in {
                    "implemented-unverified", "integration-and-runtime-smoke-tested"}}
    if not expected:
        return
    actual = set()
    for path in (REPO / "apps/backend/src/main/java").rglob("*Controller.java"):
        source = path.read_text(encoding="utf-8")
        prefix = re.search(r'@RequestMapping\("([^"]+)"\)', source)
        prefix = prefix.group(1) if prefix else ""
        for method, route in re.findall(r'@(Get|Post|Patch|Delete)Mapping\("([^"]+)"\)', source):
            actual.add((method.upper(), normalize(prefix + route)))
    assert actual == expected, (actual - expected, expected - actual)
    print(f"Validated {len(actual)} backend source route mappings (not runtime integration)")


def main():
    for path in sorted(ROOT.glob("*.schema.json")):
        schema = expand(load(path), path)
        Draft202012Validator.check_schema(schema)
        for example in schema["examples"]:
            validate_example(schema, example, path.name)
        invalid = copy.deepcopy(schema["examples"][0])
        invalid["schema_version" if "schema_version" in schema.get("properties", {}) else "unknown_field"] = 999
        reject(schema, invalid, f"{path.name} invalid version/unknown property")
        print(f"Validated {path.name}")
    implemented = check_openapi(ROOT / "openapi.yaml")
    proposed = check_openapi(ROOT / "openapi.proposed.yaml", proposed=True)
    for route, entry in proposed["paths"].items():
        assert implemented["paths"][route] == entry, "MVP mirror differs from implemented contract"
    check_backend_routes(proposed)
    negative_cases(proposed)
    check_markdown_links()
    print("PASS " + json.dumps(COUNTS, sort_keys=True))


if __name__ == "__main__":
    main()
