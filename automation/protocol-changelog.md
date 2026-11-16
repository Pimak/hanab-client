# Protocol changelog

This file is maintained by the server-synchronization workflow. Each entry records the
Hanab Live revision used to verify the client and the compatibility tests that were run.

## 0485792527b247fcf2b836969b805b96b209365a

- Baseline revision recorded for `hanab-client` 0.1.0-SNAPSHOT.
- Verification: Maven `verify`, including the local bot/WebSocket integration suite when
  `.env.local` is present.
- No unresolved protocol change is known at this revision.
