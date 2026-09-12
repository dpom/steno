## 1. Pure helpers (headless-testable)

- [x] 1.1 `next-available?` / `prev-available?`: given `{files idx}`, horizon predicate for Next past the last and Previous before the first glyph; comment-block forms at queue ends
- [x] 1.2 `loadable-file?`: wrapper over `load-glyph` returning a wsign only when the image reads and yields at least one sign (empty wsign counts as unreadable); comment-block forms
- [x] 1.3 `load-position-data`: given `{files idx}`, ctx and translators, convert the glyph (or skip forward past unreadable entries, one per position) and build rows; comment-block forms including all-unreadable tail
- [x] 1.4 `busy-guard?`: predicate over the ui state snapshot controlling action drops; comment-block forms for busy/not-busy

## 2. Async load plumbing

- [x] 2.1 Split `load-position!` into worker computation (`load-glyph` + `build-rows`, pure, no Tk) and a main-thread `commit-load!` posted via `root.after(0, …)`; both idle paths (no current file / exhausted queue) skip the worker and go straight to commit
- [x] 2.2 Result map carries the snapshot `{:files idx}` it started from; `commit-load!` discards the result when `:busy?` cleared or files/idx changed (stale-commit guard)
- [x] 2.3 Add `:busy? false` to the `make-ui` state atom and set/clear it around worker dispatch; unreadable-skip path uses 1.2 to advance `idx` before committing

## 3. Input blocking

- [x] 3.1 Busy guard (`:busy?`) early-returns in `next-glyph!`, `prev-glyph!`, `delete-glyph!`, `save-checked!`, `open-image!`, `open-dir!`; menu items disabled via `entryconfigure(… "disabled")` while busy, re-enabled after commit
- [x] 3.2 `quit-app!` joins the in-flight worker (short timeout, daemon fallback) so Quit never leaves a dangling thread

## 4. Status bar

- [x] 4.1 Status bar panel (indeterminate `ttk/Progressbar` + `tk.Label`) packed last in `run-checker!`; `start`/`stop` around processing; label shows `Processing i/n …` then `Ready`
- [x] 4.2 Boundary modals via `mb/showinfo`: Next past last → "No more glyphs to review", Previous before first → "This is the first glyph"
- [x] 4.3 Skip note in the status label ("Skipped unreadable glyph <name>"), replaced by the next navigation's `Processing …` text

## 5. Verification and wrap-up

- [x] 5.1 Run `bb kondo` and `bb style` on `checker.lpy`; fix findings
- [x] 5.2 Manual GUI smoke run: directory batch, double-click Next/Previous during a slow glyph, boundary modals at both ends, unreadable-skip note, Save/Delete/Open dropped while busy, Quit with worker in flight
- [x] 5.3 Run `openspec validate checker-busy-feedback --type change --strict` before archive