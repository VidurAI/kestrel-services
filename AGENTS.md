# AGENTS.md — kestrel-services

<!-- Written by the team. Keep it short; agents read this on every task. Do NOT
     duplicate what pom.xml / csproj / pyproject / package.json / CODEOWNERS encode.
     Backticked paths must exist in THIS repo (lint checks). For paths in another repo
     write `repo:path`, e.g. `kestrel-ops:adr/0007-rate-limit-per-carrier.md`. -->

## Build, test, run, lint
- Three independent services; build and test each in its own directory (see the service
  `AGENTS.md` files): `booking/` (Maven), `pricing/` (dotnet), `gateway/` (uv + pytest).
- The oracle for "an edit landed" is that service's test suite. Cross-service edits must
  keep all three green.
- Local run: each service starts on its own port; the gateway reads `BOOKING_BASE_URL` and
  `PRICING_BASE_URL`.

## Conventions
- Wire format is snake_case JSON everywhere (Jackson SNAKE_CASE, System.Text.Json
  SnakeCaseLower, Pydantic aliases). Internal code follows each language's idiom.
- Money is decimal, minor units, rounded AwayFromZero (ADR-0005 in kestrel-ops).
- Errors are RFC 7807 problem details with a stable `detail` string; those strings are
  contract (the gateway matches on them).
- Ticket ids (`BOOK-`, `PRC-`, `GW-`) appear in commit messages and comments; the ticket
  notes live in kestrel-ops, not here.

## Do not touch
- `booking/src/main/resources/db/` — schema and seed are owned by booking + platform; a
  change needs a migration ticket.
- Anything under `pricing/src/Kestrel.Pricing/Tariffs/Data/` — tariff data is loaded by
  configuration; classes there may look unreferenced and still be live.
- `gateway/gateway/legacy/` — scheduled for removal under its own ticket; do not extend.

## Where things are explained
- ADRs, runbooks, postmortems and ticket notes: the kestrel-ops repo (`kestrel-ops:adr/`,
  `kestrel-ops:runbooks/`, `kestrel-ops:postmortems/`, `kestrel-ops:tickets/`). Ownership: `CODEOWNERS`.

<!-- adlc:routing:start -->
## Context routing (kit-owned — do not edit; `adlc init` rewrites this section)

Pick the tool by the shape of the question, not by habit:
- **Graph** (`/graphify query|path|explain`, or `adlc query --level project|org` for
  cross-repo questions): structural questions — who calls this, what breaks if this
  changes, path between two things, which subsystem/community owns it, *why* something
  exists (docs and ADRs are nodes too).
- **grep / file read**: literals — an error string, a config key, a feature flag, a
  column name, a log line. The graph will not beat grep here; do not try.
- **Serena (LSP)**: symbol-precise edits and references — rename, replace a symbol body,
  insert after a symbol, find references across files with type awareness.
- Before trusting the graph run `adlc stale`; exit 0 fresh, 1 usable, 2 rebuild first.
  On a feature branch run `adlc baseline --incremental` before structural queries.
- Levels: this repo's graph answers inside-repo questions; `--level project` answers
  "which consumer breaks"; `--level org` answers "which team/project is affected".
<!-- adlc:routing:end -->

```yaml
adlc:
  schema: 1
  context: [graphify, serena]
  artifact: graphify-out/graph.json
  sensitive: []
  freshness: two-tier
  kit: 0.1.5
```
