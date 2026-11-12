# Architecture

## Scope

`hanab-client` is a reusable library, not an application and not an `consumer-app` module. It owns
communication with hanab.live and exposes a stable, framework-neutral interface to callers.

The initial repository deliberately contains no network client implementation. This keeps the
first public interface small and gives protocol decisions a place to be documented before they
become dependencies for downstream projects.

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

The initial artifact has no runtime dependency beyond the JDK. A JSON library or WebSocket client
will be added only when the protocol implementation requires it and after its impact on consumers
has been evaluated.

## Planned development order

1. Capture the hanab.live HTTP/WebSocket protocol as fixtures and contract tests.
2. Define immutable model types and error categories.
3. Define the smallest useful public client interface.
4. Implement a replaceable transport adapter with deterministic timeout and reconnect behavior.
5. Add integration tests against a local fake server.
6. Integrate `consumer-app` through the public interface only.

## Publication

The Maven coordinates, SCM metadata, license, wrapper and `release` profile are already present.
Before the first release, configure the `central` credentials in the user's Maven settings, choose
a non-SNAPSHOT version, update the reproducible build timestamp and publish from a signed tag in CI.
