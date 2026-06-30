# steno — Agentic Coding Guide

- For OpenSpec propose/apply/verify/archive workflows, use the local `openspec-git-discipline` agent skill to enforce proposal commits before apply and merge-before-archive discipline.

## Project Overview

Purpose: steno is a personal project to digitalize stenographic (shorthand) writings in the Duployer system. It converts scanned page images of handwritten shorthand into plain text. The pipeline is:
1. Extractor -- splits a page image into individual word images
2. Image Processor -- cleans and skeletonizes each word image (1-pixel thick lines)
3. Converter -- converts each word image into a sequence of numeric codes
4. Translator -- translates numeric sequences into letter/character strings

Language: The project is written in Basilisp (.lpy files), which is a Clojure dialect that runs on top of the Python VM. It uses Python libraries (OpenCV, numpy, scikit-image, matplotlib) via interop. The entry point is a thin Python shim (src/steno/__init__.py) that bootstraps into Basilisp.

- Source files: src/steno/*.lpy (8 source files + 1 __init__.py)
- Test files: test/steno/*.lpy (1 test file)
- Config: pyproject.toml declares basilisp>=0.5.0 as a dependency
- Build tool: hatchling, run via uv
- CLI entry: bb app <action> <args> which calls uv run steno

## Architecture
```
src/steno/              # Source code (.lpy + one __init__.py)
  core.lpy              # CLI entry point, action dispatch
  translator.lpy        # Multimethod definitions, shared logic
  translators/          # Translator implementations (freq, diff)
  corpus.lpy            # Corpus read/write, directory walking
  converter.lpy         # Image → numeric sequence conversion
  extractor.lpy         # Page image → glyph matrix extraction
  imageprocessor.lpy    # Image cleaning / skeletonization
  utils.lpy             # Shared utilities (edn, matrix, files)
  spec.lpy              # Entity type spec documentation
  show.lpy              # Emacs-based visual display functions
test/steno/             # Tests
resources/              # Config, corpus data, references
```


##  Build / Lint / Test Commands

### Environment
```sh
# Enter dev shell (Nix)
direnv allow

# Or manually:
uv venv && uv sync
source .venv/bin/activate
```

### Task runner (babashka — `bb`)
```sh
bb app <action>          # Run the steno CLI (e.g. `bb app translate`)
bb test                  # Run all basilisp tests
bb kondo [file]          # Lint with clj-kondo (src by default)
bb format [path]         # Format code with cljfmt (src test resources by default)
bb style [path]          # Check formatting without fixing
bb nrepl                 # Start basilisp nREPL server
bb edn <file>            # Format an .edn file
```

### Running a single test
```sh
# Run a specific test namespace
basilisp test --include-path test -n steno.translator-test

# Run a specific test var
basilisp test --include-path test -v steno.translator-test/my-test

# Using bb (all tests only — no single-test filter via bb)
bb test
```

### Repl-driven development
```clojure
;; Open comment blocks at end of each file serve as REPL scratchpads
(comment
  (def config (-> "resources/config.yml" ...))
  ;; Evaluate forms interactively, results follow with ;; =>
  )
```

## 2. Code Style Guidelines

### Language
- **Basilisp** (Clojure dialect on Python VM) for all source files (`.lpy`)
- Python shim only in `src/steno/__init__.py`

### Namespace & Imports
```clojure
(ns steno.translators.freq
  "Docstring for the namespace"
  (:require
   [steno.corpus :as corpus]
   [steno.translator :as tra]
   [steno.utils :as utl])
  (:import
   [statistics :as st]))
```
- Alias project libs with short names: `tra`, `utl`, `cnv`, `ext`, `img`, `corpus`, `sut`
- Python modules in `:import` with `:as` aliases or bare (e.g. `cv2`)
- `:refer` sparingly — prefer qualified `alias/sym`

### Naming Conventions
- **Files and vars**: `kebab-case` (e.g. `translate-numseq`, `action-make-corpus`)
- **Predicates**: trailing `?` (e.g. `edn-file?`, `dir?`, `type-0?`)
- **Side-effecting functions**: trailing `!` (e.g. `save-glyphs!`, `walk-corpus!`, `walk-matrix!`)
- **Private functions**: `defn-` (e.g. `defn- match-freq`)
- **Protocol/entity keys**: kebab-case keywords (e.g. `:lineseq`, `:min-refseq`)
- **Constants**: lowercase kebab-case `def` (e.g. `unknown-letter`, `normalize-bin-size`)
- **Test namespaces**: `steno.<module>-test`

### Formatting
- Use cljfmt with `.cljfmt.edn` config (matches regex `\.lpy$`)
- Run `bb format` before committing
- Indent with spaces (2-space blocks, align function args)
- Thread macros `->` and `->>` preferred over nesting

### Function Style
```clojure
(defn translate-lsign
  "Docstring goes here as a string before the param vector."
  [{:keys [logger] :as ctx} translators lsign]
  (let [res (reduce ...)]
    (.debug logger "letters: %s" (:letters res))
    (assoc res :text text)))
```
- Docstrings are **required** for public functions
- Destructure maps with `{:keys [...] :as ...}` pattern
- Use `let` bindings, avoid deep nesting
- Use `if-let` / `when-let` / `if-some` for nil-handling

### Error Handling
- Use `try/catch` for Python interop (see `converter.lpy:get-cell-value`)
- Use `or` with default values (e.g. `(or text* "@")`)
- Check edge conditions with `when`, `if`, `case`
- Log with `(.info logger "...")` / `(.debug logger "...")`
- No exception-throwing in Clojure layer — use nil-punning and logging

### Multimethods (plugin system)
```clojure
(defmulti translate (fn [name _ctx _lsign] name))
(defmethod translate "freq" [_ ctx lsign] ...)
```
- Dispatch on first string argument (translator name)
- New translators auto-register via `defmethod` in their own namespace
- Prefix ignored args with `_`

### Python Interop
```clojure
;; Method call: (.method obj args)
(.info logger "text: %s" text)

;; Attribute access: (.- obj attr)
(.- args config)

;; Keyword args: ** :key val
(arg/ArgumentParser ** :prog "steno")

;; Numpy/cv2: direct interop
(aget mat row column)
```
- Use `**` for Python `**kwargs`
- Use `aget`/`aset` for numpy array access
- Use `python/float`, `python/slice`, `python/tuple` for Python type coercion

### Test Style
```clojure
(ns steno.translator-test
  (:require
   [basilisp.test :refer [deftest is are testing]]))
```
- Use `deftest`, `is`, `are`, `testing` from `basilisp.test`
- Name tests with kebab-case ending in `-test`
- Use `are` for data-driven table tests

### REPL / Dev Conventions
- Each source file ends with a `(comment ...)` block for interactive dev
- Results annotated with `;; =>` after the form
- Use `add-tap` / `tap>` for debugging (see `utils.lpy`, `converter.lpy`)
- Use `(show/...)` functions from `steno.show` for visual debugging in Emacs
