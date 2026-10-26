## 1. Pure checker core

- [x] 1.1 Remove the tkinter imports, widget builders, and `run-checker!` from `src/steno/checker.lpy`; keep every pure function and its docstring
- [x] 1.2 Keep image encoding out of the pure core and available to the web adapter (`matrix -> PNG bytes`)
- [x] 1.3 Run `test/steno/checker_test.lpy` to confirm the pure core still passes

## 2. Web checker server

- [x] 2.1 Add `src/steno/web_checker.lpy` with the session state atom, mutation lock, and JSON response helpers
- [x] 2.2 Serialize the current state: queue position, glyph original and processed images, rows with thumbnails and formatted translator chains, letters, status, and busy flag
- [x] 2.3 Implement `GET /api/state` and `POST /api/open`, `/api/nav`, `/api/save`, and `/api/delete`
- [x] 2.4 Reuse `load-position-data`, `save-row!`, and `savable-row?` so skip and corpus semantics stay unchanged (ADR-0002)
- [x] 2.5 Serve `index.html`, `app.js`, and `app.css` from `src/steno/web/`
- [x] 2.6 Implement the lifecycle: loopback bind, port 8765 default, ephemeral fallback, `--no-browser`, and URL reporting

## 3. Browser UI

- [x] 3.1 Add the three-panel page: path field and Open control, "gliph image" panel, "lsigns" table, controls, and status line
- [x] 3.2 Render the rows with Lsign, start, freq, diff, knn, select (with existing-letter suggestions), and save columns
- [x] 3.3 Add Save, Previous, Next, and Delete actions with delete confirmation
- [x] 3.4 Disable controls while a request is in flight, show progress, and surface boundary, skip, save, and error messages
- [x] 3.5 Add keyboard shortcuts matching the previous app: Ctrl-S save, Ctrl-Left previous, Ctrl-Right next, Ctrl-D delete

## 4. CLI integration

- [x] 4.1 Rewire `action-check` in `src/steno/core.lpy` to start the web checker and accept an omitted input
- [x] 4.2 Add `--port` and `--no-browser` arguments and pass them to the checker lifecycle

## 5. Tests

- [ ] 5.1 Add a test helper that starts the WSGI app on port 0 in a background thread and stops it after the test
- [ ] 5.2 Cover state, open with unreadable-glyph skipping, navigation boundaries, save filtering, delete, and static asset responses
- [ ] 5.3 Run the existing pure-core tests plus the new HTTP integration tests

## 6. Verification and wrap-up

- [ ] 6.1 Run `bb format` and `bb kondo` over the changed files
- [ ] 6.2 Run `openspec validate web-checker --type change --strict`
- [ ] 6.3 Perform a manual browser review over a directory batch: correct a proposal, skip a row, create a new letter folder, re-save a position, and delete a glyph
- [ ] 6.4 Confirm `--no-browser` and a busy-port fallback both report a usable URL
