## Why

`checker.lpy` loads and converts each glyph synchronously on the Tk main loop. Conversion + translation of one glyph can take noticeable time, during which the window freezes: a second click on Next/Previous is queued and replayed once the load finishes, advancing (or reloading) twice. When navigation reaches the ends of the queue, pressing Next/Previous silently does nothing, and an unreadable glyph silently yields an empty table. The corpus-rebuild workflow is a long interactive session over hundreds of glyphs, so these stalls and silent failures are felt constantly.

## What Changes

- Move glyph loading (`load-glyph` → `build-rows`) to a worker thread so the Tk main loop stays responsive during processing.
- Add a `:busy?` state: while processing, every navigation/mutation action (Next, Previous, Delete, Save, Open image, Open directory) early-returns instead of being replayed, and the menu items are disabled.
- Add a status bar at the bottom of the window with an indeterminate progress bar and a status label (`Processing 3/12 …` / `Ready`).
- Notify the reviewer at the queue boundaries: Next on the last glyph shows "No more glyphs to review", Previous on the first shows "This is the first glyph".
- Skip unreadable glyphs (image read fails or wsign is empty) forward in the queue with a status-bar note instead of silently showing an empty table.
- Keep the Tk touch confined to the main thread (worker returns data, commit posted via `root.after`).

## Capabilities

### New Capabilities
- `checker-busy-feedback`: the checker window SHALL indicate processing activity and SHALL ignore input while a glyph is loading; navigation at queue ends SHALL notify the reviewer; unreadable glyphs SHALL be skipped forward with a note.

### Modified Capabilities
<!-- none: no main specs exist yet; this is independent of the gui-translator-checker change specs -->

## Impact

- `src/steno/checker.lpy` — `load-position!` split into worker computation + main-thread commit; `:busy?` guard in `next-glyph!`, `prev-glyph!`, `delete-glyph!`, `save-checked!`, `open-image!`, `open-dir!`, `quit-app!`; new status-bar panel in `run-checker!`; boundary/failure message helpers.
- New pure helpers for headless tests: queue next/prev availability, unreadable-skip index, busy-guard predicate.
- No new dependencies (threading via Python interop; tkinter/ttk already used).
- No changes to corpus format, translators, or conversion pipeline.