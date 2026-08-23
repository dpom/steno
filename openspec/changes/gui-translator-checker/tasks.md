## 1. Pure checker core (TDD)

- [ ] 1.1 Create `src/steno/checker.lpy` namespace skeleton (docstring, requires: utils, imageprocessor, converter, translator, corpus) and add `test/steno/checker-test.lpy` boilerplate using basilisp.test
- [ ] 1.2 Glyph loading helper: `cv2.imread(path, gray)` → `img/process-image` → `cnv/image-to-wsign`; write failing test first with a fixture glyph image, then implement
- [ ] 1.3 Row-model builder: one map per lsign `{:pos :matrix :results {"freq" [["t" 0.91] ...] ...} :best "t" :save? true}` built via `tra/translate-lsign`; test asserts per-translator letters/probabilities and best-match prefill
- [ ] 1.4 Result formatter rendering probability chains (`t(0.91) l(0.75)`) per row; table-driven `are` tests
- [ ] 1.5 Corpus save-data helper: strip translator annotations to exactly `{:ltype :lineseq :fileimage :row :column :pos}`, compute filename `<image>-00-00-<pos>.edn` and destination `<corpus-dir>/<letter>/` (ADR-0002 contract); tests for keys, name, path
- [ ] 1.6 Letters-list helper enumerating existing corpus subfolders; test against a temp fixture tree
- [ ] 1.7 Skip filter: unchecked rows or rows with an empty letter selection produce no save; unit test
- [ ] 1.8 `basilisp test --include-path test -n steno.checker-test` green

## 2. CLI action registration

- [ ] 2.1 Register `check` action in `core.lpy` (`:require steno.checker`, dispatch entry) with flags mirroring existing actions: `-i/--input`, `-o/--output` corpus dir, `-c/--config`, `-t/--translators`
- [ ] 2.2 Build translation context once at startup via `tra/prepare-translations config translators` inside the check action
- [ ] 2.3 Smoke test headless path: `bb app check -i test/resources/<glyph>` starts without errors up to window open

## 3. tkinter UI layer (thin, private defs)

- [ ] 3.1 Window scaffold following the mockup layout (`specs/translator-checker/GUI-screen.png`): top panel = large central frame labelled "gliph image"; middle panel = "lsigns" table; bottom diamond control cluster with SAVE on top, PREV/NEXT left/right, QUIT at bottom; File→Open menu retained; callbacks as named `defn`s
- [ ] 3.2 Image rendering: `cv2.imencode(".png")` → base64 → `tk.PhotoImage(data=...)`, with temp-PNG fallback; used for glyph view and row thumbnails
- [ ] 3.3 "lsigns" table with exactly 6 columns per row: lsign thumbnail, freq chain, diff chain, knn chain (formatted per-translator probability labels), select combobox pre-filled with `:best` (values from corpus letters-list plus free text), save checkbox bound to `:save?`
- [ ] 3.4 UI state atom `{files idx rows}`: sorted directory expansion (png jpg jpeg tif tiff), Prev/Next index moves, File→Open replaces queue synchronously
- [ ] 3.5 SAVE: persist every checked, non-empty row via the 1.5 helper with `utl/save-edn` (parents created on demand), log destination paths, overwrite same-position file as replacement; QUIT closes the app
- [ ] 3.6 Emacs bridge: auto-send glyph on open, per-row button sends lsign matrix via `utl/lineseq-to-matrix` reusing the show mechanism; wrap in try/catch + short subprocess timeout

## 4. Verification and wrap-up

- [ ] 4.1 `bb kondo` and `bb style` clean on touched files
- [ ] 4.2 Full `bb test` green
- [ ] 4.3 Manual GUI run over a directory batch: correct a proposal, skip a row, type a not-yet-present letter (folder created on save), re-save same position (replacement confirmed)
- [ ] 4.4 Run `openspec validate gui-translator-checker --type change --strict` before archive
