## 1. Pure checker core

- [x] 1.1 Create `src/steno/checker.lpy` namespace skeleton (docstring, requires: utils, imageprocessor, converter, translator, corpus) with an embedded test section in a trailing `(comment ...)` block (project convention — no external test files)
- [x] 1.2 Glyph loading helper: `cv2.imread(path, gray)` → `img/process-image` → `cnv/image-to-wsign`; comment-block forms against a real glyph image (`tmp/yoga1_12/*.png`) with expected results annotated
- [x] 1.3 Row-model builder: one map per lsign `{:pos :matrix :results {"freq" [["t" 0.91] ...] ...} :best "t" :save? true}` built via `tra/translate-lsign`; comment-block forms assert per-translator letters/probabilities and best-match prefill
- [x] 1.4 Result formatter rendering probability chains (`t(0.91) l(0.75)`) per row; table-driven examples in the comment block
- [x] 1.5 Corpus save-data helper: strip translator annotations to exactly `{:ltype :lineseq :fileimage :row :column :pos}`, compute filename `<image>-00-00-<pos>.edn` and destination `<corpus-dir>/<letter>/` (ADR-0002 contract); comment-block forms for keys, name, path
- [x] 1.6 Letters-list helper enumerating existing corpus subfolders; comment-block form against `resources/corpus`
- [x] 1.7 Skip filter: unchecked rows or rows with an empty letter selection produce no save; comment-block forms

## 2. CLI action registration

- [x] 2.1 Register `check` action in `core.lpy` (`:require steno.checker`, dispatch entry) with flags mirroring existing actions: `-i/--input`, `-o/--output` corpus dir, `-c/--config`, `-t/--translators`
- [x] 2.2 Build translation context once at startup via `tra/prepare-translations config translators` inside the check action

## 3. tkinter UI layer (thin, private defs)

- [x] 3.1 Window scaffold following the mockup layout (`specs/translator-checker/GUI-screen.png`): top panel = large central frame labelled "gliph image"; middle panel = "lsigns" table; bottom diamond control cluster with SAVE on top, PREV/NEXT left/right, QUIT at bottom; File→Open menu retained; callbacks as named `defn`s
- [x] 3.2 Image rendering: `cv2.imencode(".png")` → base64 → `tk.PhotoImage(data=...)`, with temp-PNG fallback; used for glyph view and row thumbnails
- [x] 3.3 "lsigns" table with exactly 6 columns per row: lsign thumbnail, freq chain, diff chain, knn chain (formatted per-translator probability labels), select combobox pre-filled with `:best` (values from corpus letters-list plus free text), save checkbox bound to `:save?`
- [x] 3.4 UI state atom `{files idx rows}`: sorted directory expansion (png jpg jpeg tif tiff), Prev/Next index moves, File→Open replaces queue synchronously
- [x] 3.5 SAVE: persist every checked, non-empty row via the 1.5 helper with `utl/save-edn` (parents created on demand), log destination paths, overwrite same-position file as replacement; QUIT closes the app
- [x] 3.6 Emacs bridge: auto-send glyph on open, per-row button sends lsign matrix via `utl/lineseq-to-matrix` reusing the show mechanism; wrap in try/catch + short subprocess timeout

## 4. Verification and wrap-up

- [ ] 4.1 Manual GUI run over a directory batch: correct a proposal, skip a row, type a not-yet-present letter (folder created on save), re-save same position (replacement confirmed)
- [x] 4.2 Run `openspec validate gui-translator-checker --type change --strict` before archive
