## Why

The translator checker is currently a Tkinter desktop window. Rendering glyph and lsign images, laying out the review table, and handling busy feedback are all coupled to Tk widgets, which makes the review screen harder to evolve and impossible to use from another machine or alongside browser-based tools. Moving the UI to a local web page keeps the proven checker core while giving the reviewer a more flexible screen.

## What Changes

- Replace the Tkinter checker UI with a local browser-based UI served by the existing `check` CLI action. **BREAKING**: the desktop window is removed; `check` now starts a local web server and opens a browser.
- Keep the pure checker core unchanged: glyph loading, row building, translator result formatting, corpus entry construction, input expansion, skip logic, and `save-row!` continue to own the domain behavior (ADR-0002 preserved).
- Add a new `steno.web-checker` namespace that serves static assets from `src/steno/web/` and exposes a JSON API for state, opening inputs, navigation, saving, and deleting the current glyph.
- Use only the Python standard library (`wsgiref.simple_server`, `json`, `webbrowser`). No new dependencies.
- Bind the server to `127.0.0.1`, default to port 8765, auto-open the browser, and support `--port` and `--no-browser`; fall back to an ephemeral port when the requested port is busy.
- Make the `-i` input optional. With no input, the web page opens with an empty queue and a path field for choosing a glyph image or directory.
- Preserve the review layout as three stacked panels: glyph images on top, the `lsigns` table in the middle, and controls/status at the bottom. The table keeps the Lsign, start, freq, diff, knn, select, and save columns.
- Preserve busy feedback, boundary notices, unreadable-glyph skipping, and mid-session input replacement, now expressed as browser controls and status messages.
- Remove the best-effort Emacs image side-channel requirement; the browser already displays the glyph and lsign images.

## Capabilities

### New Capabilities

*(none)*

### Modified Capabilities

- `translator-checker`: the checker launches a local web session instead of a Tkinter window; input can be opened from the page; layout, server lifecycle, and image presentation become browser-based; the Emacs requirement is removed.
- `checker-busy-feedback`: busy indication, input blocking, boundary notices, and unreadable-glyph skipping are expressed through the web UI and serialized HTTP requests.

## Impact

- **Modified**: `src/steno/checker.lpy` keeps the pure core and loses the Tkinter UI layer.
- **New**: `src/steno/web_checker.lpy` (WSGI server, session state, JSON API) and `src/steno/web/` (`index.html`, `app.js`, `app.css`).
- **Modified**: `src/steno/core.lpy` wires the `check` action to the web checker and adds `--port` and `--no-browser`.
- **Tests**: keep `test/steno/checker_test.lpy`; add HTTP integration coverage for the web adapter.
- **ADRs**: a new ADR supersedes ADR-0001 (Tkinter as GUI toolkit); ADR-0002 remains in force.
- **Dependencies**: none added.
