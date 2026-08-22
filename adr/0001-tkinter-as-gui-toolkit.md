# 0001 - Use tkinter as steno's GUI toolkit

## Status

Accepted

## Date

2026-08-22

## Context and Problem Statement

The gui-translator-checker change introduces steno's first interactive GUI: a form-style tool where a human reviews translator proposals per lsign and confirms the true letter before corpus growth. steno has no GUI framework in its dependencies, adds no pip dependencies by policy for this project, and runs on the developer's Linux machine inside Emacs. Which toolkit should host this and future interactive UIs?

## Considered Options

- tkinter: ships with Python (Tk 8.6 verified in the project venv), adequate for form-style validation, zero dependencies.
- PyQt/PySide: richer widgets but a heavy new dependency for a personal tool.
- matplotlib widgets: already used for display, but poor fit for form input.
- Web UI (local server + browser): server/browser overhead disproportionate to a single-window tool.
- Emacs-only UI via `emacsclient`: rejected during design grilling — the review screen needs a real GUI layout, not buffer text.

## Decision Outcome

Chosen option: "tkinter", because it is the only option adding zero dependencies while meeting the interaction needs of a validation form; Tk 8.6 base64 `PhotoImage(data=...)` rendering was verified in the project venv.

### Consequences

- Good, because any machine running steno can run the checker without extra installs
- Good, because future interactive tools can reuse the same thin-UI-over-pure-core pattern
- Bad, because image rendering depends on Tk >= 8.6 base64 data support (temp-PNG fallback documented)
- Bad, because widget capabilities and look-and-feel are limited compared to Qt
