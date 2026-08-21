## Why

The corpus feeding the freq, diff, and knn translators is currently grown fully automatically (`make-corpus`): every extracted lsign is filed under whatever letter the translators propose, so translation mistakes silently pollute the corpus and degrade the references built from it. A human-in-the-loop tool is needed to see what each translator proposes for real glyph images, correct the wrong proposals, and only then grow the corpus.

## What Changes

- New CLI action `check` (`bb app check -i <glyph.png|glyph-dir> [-o corpus-dir] [-c config] [-t <name>...]`) launching an interactive tkinter application; flags and config semantics mirror the existing actions.
- The `-i` input accepts either a single glyph image or a directory of glyph images; for a directory the app iterates through its images (sorted) with Next/Prev navigation, each glyph being reviewed and saved independently.
- For the current glyph image the app converts it to a wsign (`img/process-image` → `cnv/image-to-wsign`), translates every lsign with all configured translators, and displays:
  - the glyph image;
  - one row per lsign: lsign thumbnail, each translator's proposed letters with match probabilities, and an editable combobox pre-filled with the best-match text where the user selects or writes the correct letter.
- A Save action writes every non-empty row as `<image>-<row>-<col>-<pos>.edn` under `<corpus-dir>/<letter>/` using the exact corpus format of `action-make-corpus` (`ltype`, `lineseq`, `fileimage`, `row`, `column`, `pos` — no translator annotations); empty rows are skipped, and typing a letter with no folder yet creates it on save.
- File→Open dialog to load a new glyph image or glyph directory without restarting the app.
- Best-effort Emacs integration reusing `steno.show/display-image-stream`: the glyph is sent to Emacs on open and any lsign can be sent on demand; if `emacsclient` fails, the app keeps working silently.
- No changes to existing actions, the translator plugin system, or the corpus format (no breaking changes).

## Capabilities

### New Capabilities
- `translator-checker`: Interactive validation of translator proposals for glyph images loaded singly or as a directory batch — wsign extraction, per-lsign display of every translator's letters and probabilities, user selection/correction of the true letter, iteration over the batch, and curated saving of validated lsigns into the corpus letter subfolders.

### Modified Capabilities
*(none)*

## Impact

- **New file**: `src/steno/checker.lpy` — checker core (wsign→row data, result formatting, lsign saving) plus the tkinter UI layer.
- **Modified file**: `src/steno/core.lpy` — require `steno.checker`, register the `check` action.
- **New test**: `test/steno/checker-test.lpy` covering the headless helpers; the GUI layer stays thin and untested.
- **Dependencies**: none added — tkinter ships with Python (Tk 8.6 verified in the project venv); Emacs integration reuses `emacsclient` on a best-effort basis.
