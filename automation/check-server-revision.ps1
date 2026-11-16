param(
    [string]$ServerRepository = "https://github.com/Hanabi-Live/hanabi-live",
    [string]$RevisionFile = "$PSScriptRoot/server-revision.json"
)

$ErrorActionPreference = "Stop"
$tracked = Get-Content -Raw -LiteralPath $RevisionFile | ConvertFrom-Json
$shaPattern = '^[0-9a-f]{40}$'
if ([string]::IsNullOrWhiteSpace($tracked.repository) -or [string]::IsNullOrWhiteSpace($tracked.revision)) { throw "Revision file must contain repository and revision" }
if ($tracked.repository -ne $ServerRepository) { throw "Configured repository does not match the tracked repository" }
if ($tracked.revision -notmatch $shaPattern) { throw "Tracked revision is not a complete SHA-1" }
$remote = git ls-remote $ServerRepository HEAD
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($remote)) { throw "Unable to read the Hanab Live server revision" }
$head = $remote.Split("`t")[0]
if ([string]::IsNullOrWhiteSpace($head)) { throw "Unable to read the Hanab Live server revision" }

[pscustomobject]@{
    repository = $ServerRepository
    previousRevision = $tracked.revision
    currentRevision = $head
    changed = ($tracked.revision -ne $head)
} | ConvertTo-Json -Depth 3
