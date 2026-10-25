# 0003 - Use a local web UI for the translator checker

## Status

accepted, supersedes ADR-0001

## Date

2026-10-25

## Supersedes

ADR-0001

## Context and Problem Statement

ADR-0001 chose tkinter for steno's interactive checker because it added no dependencies. The checker has since become the main human review surface: it shows glyph images, a table of lsign proposals, and per-row corrections. The reviewer now wants browser-based access, easier layout work, and the ability to use the tool alongside other browser windows. Tkinter couples the review screen to desktop widgets and limits how the screen can evolve. Which UI technology should host the checker moving forward?

## Considered Options

- Keep tkinter: zero dependencies, already implemented, but fixed to the desktop and awkward to evolve.
- Local web UI served by the Python standard library (`wsgiref` + vanilla HTML/CSS/JS): zero dependencies, browser rendering, simple deployment.
- Local web UI on Flask/FastAPI: familiar routing and templating, but adds dependencies and server machinery for a single local user.
- Native Qt UI: richer widgets, but a heavy dependency and the same desktop coupling.

## Decision Outcome

Chosen option: "Local web UI served by the Python standard library", because it gives the reviewer a browser screen and keeps the project's zero-new-dependency constraint. The `check` action starts a loopback-only server, serves the page and a small JSON API, and keeps all domain behavior in the existing pure checker core.

### Consequences

- Good, because the review screen is a browser page that can be laid out and inspected with ordinary web tools
- Good, because no web framework or frontend build step is added
- Good, because the pure checker core and the ADR-0002 corpus contract remain unchanged
- Bad, because the checker now depends on a browser and an HTTP lifecycle in addition to the CLI
- Bad, because the local server has no authentication and must stay bound to the loopback interface
- Bad, because large image payloads travel inline in JSON until dedicated image endpoints are justified
