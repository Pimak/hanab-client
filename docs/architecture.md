# Architecture

## Scope

`hanab-client` is a reusable library, not an application-specific module. It owns communication
with hanab.live and exposes a stable, framework-neutral interface to callers.

The repository now contains a JDK-only HTTP/WebSocket implementation. Protocol decisions are
captured by the codec, fixtures and contract tests; the public API keeps transport details behind
`HanabClient` while exposing immutable domain projections.

## Proposed package layout

```text
com.pimak.hanab.client
├── api          Public client, session and lifecycle contracts
├── model        Immutable domain and protocol values exposed to callers
├── protocol     Wire messages and codecs; hidden unless needed by an adapter
├── transport    HTTP/WebSocket adapter and connection policy
└── internal     Implementation details, never part of the supported interface
```

The public seam should stay deep: callers should express an operation in domain terms without
constructing JSON messages, managing WebSocket frames or knowing reconnect details.

## Dependency rules

1. `api` may depend on `model`.
2. `protocol` may depend on `model`, but `model` must not depend on transport or frameworks.
3. `transport` may depend on `protocol` and JDK networking APIs.
4. `internal` may support any implementation package but must not be imported by consumers.
5. Spring, Quarkus, MCP and logging integrations belong in separate artifacts if they are needed.

The implementation uses the JDK HTTP/WebSocket APIs and has a runtime dependency on Jackson
Databind for JSON trees. Consumers must keep Jackson on the runtime classpath; Spring, Quarkus,
MCP and logging integrations remain outside this artifact.

## Planned development order

1. Capture the hanab.live HTTP/WebSocket protocol as fixtures and contract tests.
2. Define immutable model types and error categories.
3. Define the smallest useful public client interface.
4. Keep the replaceable transport adapter deterministic for timeout, reconnect and error handling.
5. Maintain unit/fixture tests and integration tests against the local Hanab Live bot server.
6. Keep application-specific integrations in separate adapter artifacts; historical consumer
   models are not part of this core module.

## Publication

The Maven coordinates, SCM metadata, license, wrapper and `release` profile are already present.
Before the first release, configure the `central` credentials in the user's Maven settings, choose
a non-SNAPSHOT version, update the reproducible build timestamp and publish from a signed tag in CI.
