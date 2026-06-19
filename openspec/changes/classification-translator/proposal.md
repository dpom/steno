## Why

The current `freq` and `diff` translators use heuristic matching (frequency similarity and bit-difference distance). Adding a classification-based translator introduces machine learning algorithms (k-NN, SVM, decision trees) that can learn patterns from corpus data more robustly, potentially improving accuracy on noisy or varied glyph inputs.

## What Changes

- New translator namespace `steno.translators.classification` implementing the `prepare-translation`, `translate`, and `build-references` multimethods
- New Python dependency on `scikit-learn` for ML classifiers
- Reference files generated from corpus data using trained classifiers (pickled models) instead of hand-tuned reference maps
- Integration into the existing translator plugin system

## Capabilities

### New Capabilities
- `classification-translator`: A translator plugin that uses supervised ML classification algorithms to map numeric glyph sequences to letters

### Modified Capabilities

None.

## Impact

- **New file**: `src/steno/translators/classification.lpy`
- **New dependency**: `scikit-learn` added to `pyproject.toml`
- **Reference format**: Pickled scikit-learn model files (`.pkl`) instead of `.edn` data
- **Corpus building**: `build-references` trains a classifier per letter from corpus data
- **Translation**: `translate` runs inference with the trained models
- **Configuration**: `config.yml` needs classification-specific reference paths and optional classifier hyperparameters
