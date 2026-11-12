import fs from "node:fs/promises";
import openapiTS, { astToString } from "openapi-typescript";
import YAML from "yaml";

const url = new URL("../../../contracts/openapi.yaml", import.meta.url);
const spec = YAML.parse(await fs.readFile(url, "utf8"));
const endpoints = {};
for (const [path, item] of Object.entries(spec.paths)) {
  for (const [method, op] of Object.entries(item)) {
    if (!["get", "post", "patch", "delete"].includes(method)) continue;
    endpoints[op.operationId] = {
      path,
      method: method.toUpperCase(),
      public: (op.security ?? spec.security)?.length === 0,
    };
  }
}
const outputs = {
  "schema.ts": astToString(await openapiTS(url, { defaultNonNullable: false })),
  "endpoints.ts":
    "// Generated from contracts/openapi.yaml. Do not edit.\nexport const endpoints = " +
    JSON.stringify(endpoints, null, 2) +
    " as const;\n",
};
for (const [name, contents] of Object.entries(outputs)) {
  const dest = new URL("../src/shared/api/" + name, import.meta.url);
  if (process.argv.includes("--check")) {
    if ((await fs.readFile(dest, "utf8")) !== contents)
      throw new Error("Contract drift: run npm run api:generate");
  } else {
    await fs.mkdir(new URL("../src/shared/api/", import.meta.url), {
      recursive: true,
    });
    await fs.writeFile(dest, contents);
  }
}
console.log(
  "OpenAPI DTO + routes synchronized: " +
    Object.keys(endpoints).length +
    " operations",
);
