## Context

The stenography pipeline produces numeric sequences (cnumseq) from glyph images. The translator layer maps these sequences to letters. Two existing translators exist:

- **freq**: Compares frequency distributions of cnum values against per-letter references using a similarity score
- **diff**: Compares raw sequences using bit-difference (Hamming distance) with sliding-window alignment

Both are heuristic/rule-based. A classification-based approach uses supervised ML to learn the mapping from corpus data, potentially handling edge cases and noisy inputs better.

## Goals / Non-Goals

**Goals:**
- Implement a translator that trains a scikit-learn classifier per letter type
- Support both type-0 (single-letter) and type-1 (multi-letter) glyphs
- Feature vector extraction from numeric sequences (frequency map + optional sequence features)
- k-NN as the primary classifier; support swapping to other sklearn classifiers
- Plug into the existing multimethod system (`tra/prepare-translation`, `tra/translate`, `tra/build-references`)
- Persist trained models as pickle files

**Non-Goals:**
- No deep learning or neural networks (out of scope for this change)
- No online/adaptive learning (models are trained once from corpus)
- No hyperparameter auto-tuning (manual defaults, configurable)

## Decisions

1. **k-NN as primary classifier** — Natural fit since existing translators already use distance/similarity concepts. k-NN requires no training phase (lazy learner), is interpretable, and works well with small corpus sizes. Fallback to SVM or RandomForest if accuracy is insufficient.

2. **Frequency vector as feature** — Reuse the frequency-based feature vector from the `freq` translator (cnum value counts). This is already proven to carry discriminative signal. Optionally extend with sequence length and raw cnum values.

3. **One-vs-rest per letter** — Train a binary classifier per letter (is-this-letter vs everything-else), matching the existing architecture where each letter has its own reference. For type-0, classify directly. For type-1, use sliding window with confidence threshold (similar to `diff` approach).

4. **Pickle serialization** — Use `joblib.dump`/`joblib.load` for model persistence. Compatible with sklearn, efficient for numpy arrays, and fits the existing reference-loading pattern.

5. **Hyperparameters via config** — Let `config.yml` specify k (neighbors), distance metric, and algorithm choice so users can tune without code changes.

## Risks / Trade-offs

- **[Dependency weight]** scikit-learn adds ~50MB to the Python environment. Mitigation: it's a standard ML library, well-maintained, and already compatible with the numpy/scipy stack used by OpenCV.
- **[Corpus size]** k-NN performance degrades with imbalanced or tiny corpora. Mitigation: start with k=3, and generate synthetic variations from existing corpus entries during training.
- **[Pickle security]** Pickle files can execute arbitrary code during loading. Mitigation: models are loaded from a trusted, version-controlled resources directory (same trust model as `.edn` files).
- **[Type-1 segmentation]** Sliding window segmentation may split letters incorrectly. Mitigation: use the same approach as `diff` (consume matched length), and add confidence threshold to reject low-quality matches.
