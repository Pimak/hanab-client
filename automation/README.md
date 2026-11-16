# Server synchronization automation

`server-revision.json` records the Hanab Live protocol revision used by the client. The
PowerShell checker compares it with the upstream repository without modifying the worktree:

```powershell
./automation/check-server-revision.ps1
```

The scheduled GitHub workflow runs the same check and the Java verification suite. A changed
revision is uploaded as an artifact so a maintainer or an interchangeable agent adapter can
produce the protocol diff and update `docs/protocol-changelog.md` in an isolated branch.

The workflow never uses production credentials. Server-backed tests require `HANAB_TEST_SERVER`
and disposable test credentials supplied by the workflow environment or `.env.local` locally.
