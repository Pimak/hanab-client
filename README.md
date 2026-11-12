# hanab-client

Reusable Java client library for [hanab.live](https://hanab.live), intended for `consumer-app` and
future JVM projects.

This repository currently contains the project foundation only. The hanab.live client itself is
not implemented yet.

## Architecture

The first implementation will be organized around four seams:

- **Public client interface**: lifecycle, authentication/session handling and high-level commands.
- **Protocol model**: immutable inbound/outbound messages and domain values, independent from the
  network implementation.
- **Transport adapter**: WebSocket/HTTP details, timeouts, reconnect policy and I/O failures.
- **Integration adapters**: optional mappings for applications such as `consumer-app`; the core jar
  must remain framework-neutral.

See [`docs/architecture.md`](docs/architecture.md) for the initial decisions and the proposed
package layout.

## Build

The project targets Java 21 and uses the Maven Wrapper:

```shell
./mvnw verify
```

On Windows:

```powershell
.\mvnw.cmd verify
```

## Coordinates

The planned Maven coordinates are `io.github.pimak:hanab-client`. The `release` Maven profile
already attaches sources, Javadoc and signatures and is prepared for deployment through the Maven
Central Publisher. Publication is intentionally not enabled until the library has a first usable
release.

## License

Apache License 2.0. See [`LICENSE`](LICENSE).
