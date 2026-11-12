# Software development with an AI agent team

## Research report

**Access date:** 2026-11-12
**Scope:** Current practices for teams that use multiple coding agents or agent sessions in one software-development workflow.
**Evidence policy:** First-party documentation and reports were preferred: Anthropic Claude Code, OpenAI Codex/Agents, GitHub Copilot, Cursor, and the SWE-bench project or its original evaluation authors. Recommendations are explicitly marked and are not claims that a vendor has validated this repository's workflow.

## Executive summary

The current first-party guidance converges on a deliberately structured form of parallelism:

1. Keep a human maintainer or a lead agent responsible for the goal, task graph, integration, and final acceptance.
2. Delegate only work that is genuinely independent, self-contained, and worth the coordination cost. Keep short, dependent, or same-file work in one session.
3. Give each worker an explicit role, scope, expected deliverable, acceptance criteria, and verification command. Do not assume that a worker inherits the lead's conversation history.
4. Isolate concurrent writers with branches or Git worktrees and assign file ownership. Treat shared context and shared Git metadata as coordination surfaces.
5. Persist project knowledge in concise repository instructions and focused design/plan documents. Treat those documents as context, not as a security boundary.
6. Require a reviewable diff, observed test output, and independent human/CI gates before merge, release, credential use, or deployment. An agent's “done” message is not evidence by itself.
7. Treat prompts, issues, source comments, fixtures, tool output, and external content as untrusted input. Restrict network access and credentials, and assume that autonomous execution increases the blast radius of prompt injection.

For `hanab-client`, the most appropriate default is a small, staged topology rather than a permanent autonomous “swarm”: one lead/integrator, optional read-only explorers, one implementer per architectural seam, and a separate verifier/reviewer. The repository is currently a small Java 21 foundation with no network implementation, so parallel implementation should wait until the planned protocol, model, API, transport, and integration boundaries have concrete files to own.

## Repository baseline

The following facts are from the repository as inspected on 2026-11-12:

- `hanab-client` is a reusable, framework-neutral Java client library for `hanab.live`.
- The build targets Java 21 and uses the Maven Wrapper. The documented verification command is `./mvnw verify`, or `.\mvnw.cmd verify` on Windows.
- The architecture document defines the seams `api`, `model`, `protocol`, `transport`, and `internal`, with integration adapters kept separate from the core artifact.
- The planned implementation order is protocol fixtures and contract tests, immutable model types, the public client interface, a replaceable transport adapter, local fake-server integration tests, and downstream integration.
- The current CI workflow runs Maven `verify` for pull requests and pushes to `main`, with `contents: read` permissions.
- The current root `AGENTS.md` contains contribution/attribution rules but not build commands, architectural guidance, ownership rules, or verification policy.

These facts shape the recommendations below. This report does not modify `AGENTS.md`, the CI workflow, application code, tests, or build configuration.

## Evidence from current first-party guidance

### 1. Team topology and roles

**Evidence.** Anthropic describes an agent team as a lead plus independent teammates, a shared task list, and a mailbox. The lead coordinates work and synthesizes results; teammates have separate context windows and can message one another. Anthropic also distinguishes this from subagents, whose results normally return to the caller and whose coordination cost is lower. Agent teams are described as most useful for parallel research/review, separate feature pieces, competing debugging hypotheses, and cross-layer work; sequential work, same-file edits, and highly dependent work are explicitly described as better fits for one session or subagents. [Anthropic, “Orchestrate teams of Claude Code sessions”](https://code.claude.com/docs/en/agent-teams) (accessed 2026-11-12)

**Evidence.** OpenAI's multi-agent guidance uses the same orchestrator pattern: independent subagents have their own context, work in parallel, and return results to a main agent. It recommends clear questions and expected results, and says that short or dependent tasks should stay in the main agent and that agents editing the same files must coordinate. [OpenAI, “Multi-agent”](https://developers.openai.com/api/docs/guides/agents-api/multi-agent) (accessed 2026-11-12)

**Evidence.** GitHub's fleet guidance describes a main agent that breaks down the goal, identifies dependencies, assigns tasks and tools, runs independent tasks in parallel, and reviews/combines the results. It also warns that each additional agent adds model interactions and usage, and that parallelism has value only when task boundaries are clear and coordination costs are lower than the time saved. [GitHub, “Delegating and parallelizing work with agents”](https://docs.github.com/en/copilot/concepts/agent-capabilities/choosing-and-delegating-agent-work/delegating-and-parallelizing-work-with-agents) (accessed 2026-11-12)

**Recommendation for this project.** Use the following topology:

| Role | Responsibility | Default access | Output |
| --- | --- | --- | --- |
| Human maintainer / release owner | Defines intent, approves scope, reviews risk, merges, publishes | Full human-controlled access | Accepted change or explicit rejection |
| Lead / integrator agent | Reads the repository, creates the task graph, assigns ownership, integrates results, and reports evidence | Read/write only in an approved integration branch; no release credentials | Plan, task status, integration diff, final verification summary |
| Explorer / protocol researcher | Inspects existing protocol behavior, fixtures, documentation, and compatibility constraints | Read-only; network only when explicitly required | Short findings with source paths, assumptions, and open questions |
| Seam implementer | Owns one bounded slice such as protocol/model or transport/tests | Write access to its own worktree and owned paths | Small diff, tests, and changed-file list |
| Verifier | Runs tests, checks contracts, reviews integration behavior, and tries negative cases | Prefer read-only review plus test execution in an isolated checkout | Reproducible command output and pass/fail findings |
| Security/API reviewer | Checks secrets, dependency risk, public API compatibility, wire compatibility, and release impact | Read-only unless explicitly asked to remediate | Severity-ranked findings and required follow-ups |

For the current foundation, start with one lead plus a read-only explorer and a verifier. Add parallel seam implementers only when the work produces separate files and independent acceptance criteria. Keep a single human or lead responsible for the public API and transport contracts; those decisions are shared dependencies, not good candidates for simultaneous invention.

### 2. Task decomposition and sequencing

**Evidence.** Anthropic recommends self-contained tasks with clear deliverables, warns against tasks that are too small or too large, and recommends separate ownership of files to avoid overwrites. It suggests starting with research and review before parallel implementation, and monitoring/redirecting agents rather than letting a team run unattended. [Anthropic, “Agent team best practices”](https://code.claude.com/docs/en/agent-teams) (accessed 2026-11-12)

**Evidence.** GitHub documents a research → plan → branch changes → diff review → pull-request workflow. It explicitly supports asking an agent to understand the repository and refine a plan before allowing changes, then iterating on a branch before creating a pull request. [GitHub, “Research, plan, and iterate on code changes with Copilot”](https://docs.github.com/en/copilot/how-tos/copilot-on-github/use-copilot-agents/research-plan-iterate) (accessed 2026-11-12)

**Evidence.** OpenAI's Codex workflow recipe recommends persistent repository guidance, goals, plans, phase-sized build files, acceptance criteria, small reviewable artifacts, and observed verification evidence. It says to keep commits, pushes, deployments, credentials, and external writes behind separate explicit approval. [OpenAI, “Iterating Development Workflows with Codex”](https://developers.openai.com/cookbook/examples/codex/iterating-development-workflows-with-codex) (accessed 2026-11-12)

**Recommendation for this project.** Decompose each feature into a dependency-aware task graph:

1. **Read-only preflight:** identify affected packages, existing conventions, protocol assumptions, public API impact, test seams, and non-goals.
2. **Contract/plan checkpoint:** write the intended behavior, compatibility constraints, acceptance tests, owned files, and validation commands. Do not start parallel implementation before this checkpoint is accepted.
3. **Protocol and fixtures:** capture the wire behavior and failure cases in fixtures/contract tests. This is a natural first bounded workstream because later model and transport work depends on it.
4. **Model and API:** define immutable domain/protocol values and the smallest public client/session contracts. Keep this worker away from transport implementation files.
5. **Transport:** implement HTTP/WebSocket behavior, timeouts, reconnect policy, and I/O error mapping against the accepted contracts.
6. **Integration verification:** use a local fake server and end-to-end contract tests to exercise the seams together.
7. **Review and handoff:** run API compatibility, security, dependency, test, and documentation checks; then present the final diff to the maintainer.

Each task prompt should include: goal, non-goals, exact paths, owned files, dependencies, assumptions, acceptance criteria, commands to run, expected evidence format, and the rule “stop and report if the contract is ambiguous.” The lead should maintain a small dependency graph rather than giving every worker the entire product roadmap.

### 3. Branches and parallel worktrees

**Evidence.** Anthropic documents Git worktrees as separate working directories with separate branches and files, while sharing repository history and remotes. It recommends one worktree per parallel session so edits do not collide. It also documents that worktrees still share the repository's `.git` directory and some project configuration, and that agents in their own worktrees need explicit isolation and cleanup. [Anthropic, “Run parallel sessions with worktrees”](https://code.claude.com/docs/en/worktrees) (accessed 2026-11-12)

**Evidence.** Cursor's background agents clone a repository, work on a separate branch, and push it back for handoff. Cursor also states that background agents run in isolated remote VMs, have internet access, and auto-run terminal commands; it calls out prompt-injection-based data exfiltration as a risk. [Cursor, “Background Agents”](https://docs.cursor.com/background-agent) (accessed 2026-11-12)

**Evidence.** GitHub's cloud agent creates a branch or works on the pull-request branch, limits the agent's push capability, requires human review before merge, and delays privileged Actions workflows until a person approves them. [GitHub, “Risks and mitigations for GitHub Copilot cloud agent”](https://docs.github.com/en/copilot/concepts/security-governance-and-network-settings/risks-and-mitigations) (accessed 2026-11-12)

**Recommendation for this project.** Use one branch/worktree per writing workstream, with file ownership recorded in the task plan. Prefer branches based on the current integration branch when the work depends on accepted local changes; otherwise branch from the default branch. Keep the lead/integrator as the only process that combines changes. Before integration, require each worker to report its base commit, branch name, changed paths, uncommitted state, and verification result.

Do not copy `.env` files, Maven settings, signing keys, GitHub tokens, or other untracked secrets into worktrees. A clean worktree should be reproducible from versioned files and an explicit setup command. Agents should not change branches or rewrite shared Git state while another agent is active; the shared `.git` directory means a worktree is file-isolated, not a license for uncoordinated Git operations. Clean up only after confirming that no uncommitted changes, commits, or useful logs would be lost.

### 4. Shared context and institutional memory

**Evidence.** Codex automatically discovers `AGENTS.md` files from the user/global locations and from the repository path, merges them in order, and injects them into the context before the user prompt. OpenAI's Codex recipe recommends using `AGENTS.md` for repository purpose, conventions, testing standards, context files, project behavior, scopes, and boundaries; it treats plans and phase files as optional conventions rather than required magic filenames. [OpenAI, “Codex Prompting Guide — Using agents.md”](https://developers.openai.com/cookbook/examples/gpt-5/codex_prompting_guide) (accessed 2026-11-12); [OpenAI, “Iterating Development Workflows with Codex”](https://developers.openai.com/cookbook/examples/codex/iterating-development-workflows-with-codex) (accessed 2026-11-12)

**Evidence.** Anthropic says that each Claude Code session starts with a fresh context window. It recommends concise project instructions for facts that would otherwise be re-explained, while path-specific procedures belong in rules or skills. It also states that `CLAUDE.md`/`AGENTS.md` are context, not enforced configuration; hooks are needed to block actions regardless of model decisions. [Anthropic, “How Claude remembers your project”](https://code.claude.com/docs/en/memory) (accessed 2026-11-12)

**Evidence.** Cursor describes rules and `AGENTS.md` as persistent, reusable context and warns that insufficient or irrelevant context causes hallucinations or inefficient self-search. It recommends focused, actionable, scoped rules and explicit context selection when the relevant files are known. [Cursor, “Rules”](https://docs.cursor.com/context/rules-for-ai) (accessed 2026-11-12); [Cursor, “Working with Context”](https://docs.cursor.com/guides/working-with-context) (accessed 2026-11-12)

**Recommendation for this project.** Keep durable context layered:

- **Root `AGENTS.md`:** concise, stable facts: Java 21, Maven Wrapper commands, repository purpose, architecture boundaries, test expectations, release restrictions, and ownership/verification rules. Keep attribution rules intact. Do not turn it into a long handbook.
- **`docs/architecture.md`:** the source of truth for package seams, dependency direction, public API depth, and planned implementation order. Agents should read it before proposing cross-seam changes.
- **Task plan:** exact goal, non-goals, affected paths, acceptance criteria, dependency edges, and verification commands.
- **Handoff/evidence record:** decisions, commands actually run, outputs, known limitations, and follow-ups. Do not record planned work as completed work.

Every delegated prompt should restate the task-specific context because a new worker does not inherit the lead's chat history. Prefer links to authoritative repository files over duplicating long prose in several prompts. Keep context files reviewable and versioned; use hooks, CI, branch protection, and credential policy for enforcement.

### 5. Review, testing, and verification

**Evidence.** Anthropic's common workflows include plan mode, test generation matched to existing project patterns, explicit verification after refactoring, and review/refinement of generated pull requests. [Anthropic, “Common workflows”](https://code.claude.com/docs/en/common-workflows) (accessed 2026-11-12)

**Evidence.** GitHub says Copilot pull requests deserve the same thorough review as any other contribution. It requires another reviewer when repository approval rules apply, and warns that workflow files can be privileged and access sensitive secrets. [GitHub, “Review output from Copilot”](https://docs.github.com/en/copilot/how-tos/copilot-on-github/use-copilot-agents/review-copilot-output) (accessed 2026-11-12)

**Evidence.** GitHub's cloud agent performs CodeQL, secret scanning, and dependency advisory checks by default in its own workflow, but the same documentation presents these as mitigations, not a replacement for human review and branch protections. [GitHub, “Risks and mitigations for GitHub Copilot cloud agent”](https://docs.github.com/en/copilot/concepts/security-governance-and-network-settings/risks-and-mitigations) (accessed 2026-11-12)

**Evidence.** OpenAI's Codex guidance emphasizes milestone-sized work, validation after each milestone, repairing failures before continuing, scoped diffs, and observed evidence. [OpenAI, “Run long horizon tasks with Codex”](https://developers.openai.com/blog/run-long-horizon-tasks-with-codex) (accessed 2026-11-12)

**Recommendation for this project.** Make “verified” a structured claim. Every worker handoff should contain:

```text
Scope:
Changed files:
Commands run:
Observed results:
Tests added or updated:
API/protocol compatibility impact:
Security/dependency considerations:
Known failures or skipped checks:
Follow-up required:
```

For Java changes, the baseline verification command is `.\mvnw.cmd --batch-mode --no-transfer-progress verify` on Windows and `./mvnw --batch-mode --no-transfer-progress verify` in CI. Add focused unit/contract tests before or with the implementation; add local fake-server integration tests before claiming transport behavior is complete. Have a verifier inspect the diff and run the checks from a clean checkout, not merely trust the implementer's local result.

Keep these gates separate:

- **Agent self-check:** useful for iteration, never sufficient for merge.
- **CI:** reproducible build, tests, static/security/dependency checks as configured.
- **Human review:** scope, API compatibility, behavior, maintainability, and security.
- **Release approval:** separate from implementation approval; requires protected tags, credentials, signing, and publication controls.

### 6. CI and security boundaries

**Evidence.** Claude Code uses permission modes, sandboxed Bash, working-directory boundaries, allow/deny rules, and explicit user responsibility for reviewing code and commands. Its security documentation also treats prompt injection as a threat and gives network commands special approval treatment. [Anthropic, “Security”](https://code.claude.com/docs/en/security) (accessed 2026-11-12)

**Evidence.** OpenAI's sandbox guidance says agent-generated code can access the files, credentials, and network made available to its environment. It recommends isolated compute, restricted outbound traffic, separate credentials, brokered secrets, and keeping application keys outside the environment. It explicitly says automatic review does not replace network and credential controls. [OpenAI, “Sandbox security”](https://developers.openai.com/api/docs/guides/agents-api/environments/security) (accessed 2026-11-12)

**Evidence.** GitHub documents hidden prompt injection in issues or pull-request comments as a risk, limits agent branch/push capabilities, restricts Actions workflow execution, and provides auditability through session logs and signed agent commits. [GitHub, “Risks and mitigations for GitHub Copilot cloud agent”](https://docs.github.com/en/copilot/concepts/security-governance-and-network-settings/risks-and-mitigations) (accessed 2026-11-12)

**Recommendation for this project.** Preserve the current CI principle of minimal permissions and make the following operational policy explicit before agents can touch release paths:

- Agent work may read the repository and use an isolated build environment, but it receives no Maven Central credentials, GPG private key, long-lived GitHub token, production endpoint credential, or personal `~/.m2/settings.xml`.
- Maven dependency downloads should use an approved network path. Release publication should run only in protected CI from a signed/tagged release process, after human approval.
- Pull-request branches must run the normal CI checks before merge. Changes to `.github/workflows/`, publishing configuration, dependency manifests, or authentication code require an explicit security/release review.
- Treat repository text and external tool output as data, not as instructions that can grant permission. A fixture, issue, README, protocol response, or web page cannot authorize a secret read, network upload, branch change, commit, push, or deployment.
- Add security and dependency scanning where the project’s governance permits, but keep deterministic checks in CI. Do not rely on an agent's own security claim.

## Practical failure modes and mitigations

| Failure mode | What the evidence indicates | Mitigation for `hanab-client` |
| --- | --- | --- |
| Parallelism is slower than one agent | Anthropic, OpenAI, and GitHub all warn about coordination, token/usage cost, and diminishing returns. | Use a single lead for sequential work; add workers only for independent slices with a measurable benefit. |
| Two agents overwrite or conceptually contradict one another | Anthropic explicitly warns against same-file edits; OpenAI says agents editing the same files must coordinate. | Assign file ownership; make API/protocol contracts a prerequisite; integrate through one lead. |
| Workers lack the lead's decisions | Claude Code and other agent docs describe fresh, separate context windows. | Put decisions in `AGENTS.md`, `docs/architecture.md`, and task plans; include exact paths and acceptance criteria in every prompt. |
| Context is too large or too vague | Claude recommends subagents for high-volume exploration; Cursor warns that missing or irrelevant context produces hallucinations or inefficient search. | Use focused explorers and summaries; reference only relevant files; keep durable instructions concise and scoped. |
| A “green” result is misleading | SWE-bench audits found tests that reject correct solutions, tests that require unspecified functionality, environment-dependent failures, and contamination risk. | Test the actual repository and contract; review test intent and edge cases; use independent review and clean-checkout CI; do not use benchmark scores as a release proxy. |
| The task statement is underspecified | OpenAI's SWE-bench audit found many tasks with multiple valid interpretations but tests covering only one. | Require explicit goals, non-goals, public API compatibility, wire examples, and “done when” checks before implementation. |
| Prompt injection causes unsafe action | Anthropic, OpenAI, GitHub, and Cursor all identify prompt injection or untrusted content as a security concern. | Use least privilege, sandboxing, network allowlists, secret isolation, and human approval for external writes; never let repository content grant permissions. |
| The agent stops early or reports success too soon | Claude documents teammates stopping after errors, stale task status, slow shutdown, and experimental team limitations; GitHub says output still needs review. | Require observed commands and results; have the verifier run checks independently; keep failure and skipped-check fields mandatory. |
| Worktree cleanup loses useful work | Worktrees can contain uncommitted files or commits; cleanup behavior differs for interactive and non-interactive sessions, and shared `.git` state remains. | Record branch/base commit; never force-clean an active or unreviewed worktree; preserve logs and diffs until integration is complete. |
| Benchmark optimism masks real limitations | OpenAI recommends no longer treating SWE-bench Verified as a current frontier capability measure because of contamination and flawed tasks; SWE-Bench Pro was designed around longer, multi-file tasks and reports much lower pass rates. | Measure this workflow with project-local tasks, clean environments, pass/fail evidence, review findings, rework rate, and escaped defects. |

The benchmark caution is especially important: a benchmark can be useful for comparing a fixed setup, but it is not proof that an agent can safely maintain this repository. [OpenAI, “Why we no longer evaluate SWE-bench Verified”](https://openai.com/index/why-we-no-longer-evaluate-swe-bench-verified/) (accessed 2026-11-12); [Scale AI, “SWE-Bench Pro”](https://arxiv.org/abs/2509.16941) (accessed 2026-11-12)

## Recommended operating playbook

### Before dispatch

- Confirm the desired outcome, non-goals, risk level, and allowed external systems.
- Read `AGENTS.md`, `README.md`, and `docs/architecture.md`.
- Inspect the current branch/worktree and identify uncommitted user work.
- Create a small plan with dependencies, file ownership, acceptance criteria, and verification commands.
- Decide whether the work is sequential, subagent-sized, or genuinely team-sized.

### During implementation

- Start with read-only exploration and contract clarification.
- Give each writer one owned worktree and one bounded deliverable.
- Keep public API and protocol decisions ahead of dependent transport work.
- Require agents to stop on ambiguity instead of inventing compatibility behavior.
- Check progress at task boundaries; redirect or stop unproductive workers.
- Keep credentials and release actions out of the worker environment.

### At integration

- Compare each branch with its recorded base; inspect the complete diff and changed-file list.
- Integrate one workstream at a time, resolving contract conflicts deliberately.
- Run focused tests after each integration and the full Maven verification from a clean checkout.
- Ask a separate reviewer to inspect API compatibility, protocol behavior, error handling, security, and tests.

### Before merge or release

- Require CI success and an independent human approval.
- Treat workflow, dependency, authentication, and release changes as elevated-risk.
- Confirm no secrets, generated artifacts, unexpected files, or unrelated refactors are present.
- For release: use protected CI, signed tags, scoped credentials, and a separate release approval.
- Record observed verification, limitations, and follow-ups for the next agent or maintainer.

## Suggested task brief

The following is a recommended prompt shape for future work in this repository. It is guidance, not a new repository file:

```text
Goal: <one observable outcome>
Non-goals: <explicitly excluded behavior>
Read first: AGENTS.md, README.md, docs/architecture.md, <task-specific files>
Owned paths: <files/directories this worker may change>
Dependencies: <accepted contracts or prerequisite tasks>
Constraints: Java 21, Maven Wrapper, framework-neutral core, public API compatibility
Acceptance criteria:
  - <behavioral requirement>
  - <error/edge-case requirement>
  - <documentation or compatibility requirement>
Verification:
  - <focused test command>
  - .\mvnw.cmd --batch-mode --no-transfer-progress verify
Handoff: report changed files, commands, observed results, failures, security impact,
and follow-ups. Do not commit, push, publish, or use credentials without explicit approval.
```

## Conclusions

The best current practice is not “use as many agents as possible.” It is controlled parallelism with a lead, explicit ownership, durable context, isolated workspaces, independent verification, and hard human/CI gates. Agent teams are most valuable for bounded research, review, cross-layer work, and competing hypotheses. They are a poor fit for sequential design, shared-file editing, or tasks whose acceptance criteria are not yet clear.

For `hanab-client`, adopt the workflow incrementally: first strengthen the durable project context and task briefs, then use read-only exploration and verification agents, and only then parallelize implementation along the documented architecture seams. Keep merge, release, credentials, and publication under explicit human-controlled gates.

## Source register

All sources below were accessed on 2026-11-12. URLs are also linked inline above where the corresponding evidence is used.

- Anthropic — [Claude Code agent teams](https://code.claude.com/docs/en/agent-teams)
- Anthropic — [Claude Code subagents](https://code.claude.com/docs/en/sub-agents)
- Anthropic — [Claude Code worktrees](https://code.claude.com/docs/en/worktrees)
- Anthropic — [Claude Code memory and `AGENTS.md`](https://code.claude.com/docs/en/memory)
- Anthropic — [Claude Code security](https://code.claude.com/docs/en/security)
- Anthropic — [Claude Code common workflows](https://code.claude.com/docs/en/common-workflows)
- OpenAI — [Agents API multi-agent orchestration](https://developers.openai.com/api/docs/guides/agents-api/multi-agent)
- OpenAI — [Agents API overview and managed Codex harness](https://developers.openai.com/api/docs/guides/agents-api/overview)
- OpenAI — [Sandbox security](https://developers.openai.com/api/docs/guides/agents-api/environments/security)
- OpenAI — [Codex prompting guide](https://developers.openai.com/cookbook/examples/gpt-5/codex_prompting_guide)
- OpenAI — [Iterating development workflows with Codex](https://developers.openai.com/cookbook/examples/codex/iterating-development-workflows-with-codex)
- OpenAI — [Run long-horizon tasks with Codex](https://developers.openai.com/blog/run-long-horizon-tasks-with-codex)
- OpenAI — [Why SWE-bench Verified is no longer a frontier measure](https://openai.com/index/why-we-no-longer-evaluate-swe-bench-verified/)
- GitHub — [Delegating and parallelizing work with agents](https://docs.github.com/en/copilot/concepts/agent-capabilities/choosing-and-delegating-agent-work/delegating-and-parallelizing-work-with-agents)
- GitHub — [Research, plan, and iterate on code changes](https://docs.github.com/en/copilot/how-tos/copilot-on-github/use-copilot-agents/research-plan-iterate)
- GitHub — [Review Copilot output](https://docs.github.com/en/copilot/how-tos/copilot-on-github/use-copilot-agents/review-copilot-output)
- GitHub — [Risks and mitigations for Copilot cloud agent](https://docs.github.com/en/copilot/concepts/security-governance-and-network-settings/risks-and-mitigations)
- Cursor — [Background Agents](https://docs.cursor.com/background-agent)
- Cursor — [Rules and `AGENTS.md`](https://docs.cursor.com/context/rules-for-ai)
- Cursor — [Working with Context](https://docs.cursor.com/guides/working-with-context)
- Cursor — [Using Agent in the CLI](https://docs.cursor.com/en/cli/using)
- Scale AI / SWE-Bench Pro authors — [SWE-Bench Pro paper](https://arxiv.org/abs/2509.16941)
