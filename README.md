# hanab-client

Reusable Java client library for [hanab.live](https://hanab.live), intended for reusable JVM
projects.

The client exposes a JDK-based asynchronous API for login, session cookies, WebSocket events,
lobby/table actions, game actions, replay commands and chat. HanabBot is the bot-specific entry
point: it always sends client=bot during login and WebSocket negotiation, allowing one bot
identity to attend multiple tables as supported by Hanab Live.

The server remains authoritative. The library does not calculate game legality locally and keeps
unknown event payloads accessible through the raw wire message so newer server fields are not
silently discarded; typed projections may still reject invalid or unsupported shapes.

## Public HTTP helpers

`PublishedHanabAvailabilityProbe` performs an unauthenticated, HTTP/1.1 `GET /` with bounded
timeouts and returns an explicit `HttpProbeStatus` plus any received HTTP status. It accepts HTML
responses and does not follow redirects. `PublishedHanabReplayFetcher` owns its HTTP client and
decodes the bounded `/export/{gameId}` response into `ReplayDocument`; non-2xx responses expose
their status with `TransportException.statusCode()`.

## Architecture

The first implementation will be organized around four seams:

- **Public client interface**: lifecycle, authentication/session handling and high-level commands.
- **Protocol model**: immutable inbound/outbound messages and domain values, independent from the
  network implementation.
- **Transport adapter**: WebSocket/HTTP details, timeouts, reconnect policy and I/O failures.
- **Integration adapters**: optional mappings for consuming applications; the core jar must
  remain framework-neutral.

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
Central Publisher.

## Releases

Releases are automated with [semantic-release](https://semantic-release.gitbook.io/semantic-release/).
Pushes to `main` use Conventional Commits to determine the next version, update `pom.xml`, generate
`CHANGELOG.md`, create a `vX.Y.Z` Git tag and GitHub release, then publish the signed Maven
artifacts through the Maven `release` profile. The profile is invoked by semantic-release during
its publish phase; there is no separate Maven release workflow.

The release workflow uses the same Central Portal secret names as the other Pimak Maven projects:
`MAVEN_CENTRAL_TOKEN_USERNAME`, `MAVEN_CENTRAL_TOKEN_PASSWORD`, `GPG_PRIVATE_KEY` and
`GPG_PASSPHRASE`. Use `feat:` for a minor release, `fix:` for a patch release, and `BREAKING
CHANGE:` (or `!`) for a major release.

## License

Apache License 2.0. See [`LICENSE`](LICENSE).
