# steno

`steno` is a personal project dedicated to digitizing Duployer shorthand (stenography) notes. It aims to transform scanned images of handwritten shorthand into digital text, ultimately integrating them into an `org-roam` system.

## Technology Stack

- **Language:** [Basilisp](https://github.com/basilisp-lang/basilisp) (a Clojure dialect targeting the Python VM).
- **Core Libraries:**
  - [OpenCV](https://opencv.org/) (`cv2`) for image processing.
  - [scikit-image](https://scikit-image.org/) (`skim`, `skiu`) for morphology and skeletonization.
  - [NumPy](https://numpy.org/) (`np`) for matrix manipulations.
  - [matplotlib](https://matplotlib.org/) for visualization.
- **Environment & Build:**
  - `nix` & `direnv` for reproducible development environments.
  - `uv` for Python package management.
  - [Babashka](https://babashka.org/) (`bb`) for task orchestration.
- **Testing:** `pytest` (integrated with Basilisp).

## Architecture

The application follows a pipeline architecture:
1. **Extractor** (`steno.extractor`): Splits page images into individual word images (glyphs).
2. **Image Processor** (`steno.imageprocessor`): Cleans and skeletonizes glyph images (1-pixel thick lines).
3. **Converter** (`steno.converter`): Converts processed word images into numerical sequences (`numseq`).
4. **Translator** (`steno.translator`): Converts numerical sequences into character strings using various strategies (e.g., `diff`, `freq`).

### File Organization
- `src/steno/core.lpy`: CLI entry point, action dispatch.
- `src/steno/translator.lpy`: Multimethod definitions, shared logic.
- `src/steno/translators/`: Translator implementations (freq, diff).
- `src/steno/corpus.lpy`: Corpus read/write, directory walking.
- `src/steno/converter.lpy`: Image → numeric sequence conversion.
- `src/steno/extractor.lpy`: Page image → glyph matrix extraction.
- `src/steno/imageprocessor.lpy`: Image cleaning / skeletonization.
- `src/steno/utils.lpy`: Shared utilities (edn, matrix, files).
- `src/steno/spec.lpy`: Entity type spec documentation.
- `src/steno/show.lpy`: visual display functions (useful for REPL).

## Core Concepts & Data Structures

- **Glyph/wsign (Word Sign):** An individual stenographic word unit.
- **lsign (Letter Sign):** A component of a word sign, representing a letter or sound.
- **numseq:** A sequence of numbers (often derived from skeletonized pixel patterns) used for matching.
- **Matrix (`mtx`):** A 2D collection of glyphs extracted from a page.
- **Corpus:** A collection of labeled `edn` files used for building translation references.

## Development Workflow

### Common Commands
- **Run Application:** `bb app <action> <args>`
- **Run Tests:** `bb test`
- **Linting:** `bb kondo [file]`
- **Check Style:** `bb style [path]`
- **Format Code:** `bb format [path]`
- **Format EDN:** `bb edn <file.edn>`
- **nREPL:** `bb nrepl` (Starts a Basilisp nREPL server)

### Code Style & Conventions

#### Namespace & Imports
- **Language:** Use Basilisp (`.lpy`) for all logic.
- **Namespaces:** Match the directory structure.
- **Aliases:** Prefer short aliases for project libs: `tra` (translator), `utl` (utils), `cnv` (converter), `ext` (extractor), `img` (imageprocessor), `corpus`.
- **Python Imports:** Use `:import` for Python libraries. Alias with `:as` or use bare (e.g., `cv2`).

#### Naming Conventions
- **Kebab-case:** Used for files, vars, and functions (e.g., `translate-numseq`).
- **Predicates:** Trailing `?` (e.g., `type-0?`).
- **Side-effects:** Trailing `!` (e.g., `walk-matrix!`).
- **Private Functions:** Use `defn-`.
- **Constants:** Lowercase kebab-case `def` (e.g., `unknown-letter`).

#### Implementation Patterns
- **Docstrings:** Required for all public functions.
- **Destructuring:** Prefer `{:keys [a b] :as m}` pattern.
- **Python Interop:**
  - `(.method obj args)` for method calls.
  - `(.-attribute obj)` for attribute access.
  - `(python/tuple [...])`, `(python/slice ...)` for Python-specific types.
  - Use `**` for passing keyword arguments to Python functions (e.g., `(cv2/blur img [5 5] ** :borderType cv2/BORDER_DEFAULT)`).
  - Use `aget`/`aset` for NumPy array access.
- **Error Handling:** Use `try/catch` for Python interop. Prefer nil-punning and logging over throwing exceptions in the Clojure layer.
- **Multimethods:** Used for the translator plugin system. Dispatch on the translator name (string).

#### REPL-Driven Development
- Every source file should end with a `(comment ...)` block for interactive development and scratchpad usage.

## OpenSpec Workflow

The project uses a spec-driven development workflow via `openspec`.
- Specifications are located in `openspec/specs/`.
- Proposed changes are in `openspec/changes/`.
- Custom AI commands (`opsx`) are available for exploring, proposing, applying, and archiving changes.
- Workflow: Propose -> Explore -> Apply -> Archive.

## Testing
- Tests are located in `test/steno/`.
- Use `basilisp.test` (`deftest`, `is`, `are`, `testing`).
- Test namespaces follow `steno.<module>-test`.
- Data-driven table tests with `are` are preferred where applicable.
