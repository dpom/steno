## 1. Setup

- [ ] 1.1 Add `scikit-learn` and `joblib` as Python dependencies in `pyproject.toml`
- [ ] 1.2 Create `src/steno/translators/classification.lpy` with namespace boilerplate

## 2. Feature Extraction

- [ ] 2.1 Implement `extract-features` function: convert a cnumseq to a 256-bin frequency vector normalized by sequence length

## 3. References Building

- [ ] 3.1 Implement `build-reference` for a single letter: read corpus, extract features, train k-NN classifier
- [ ] 3.2 Implement `build-diff-ref` wrapper per letter path
- [ ] 3.3 Implement `tra/build-references "classification"` multimethod: walk corpus, build references for type-0 and type-1 letters

## 4. Translation

- [ ] 4.1 Implement `check-letter`: run inference with all classifiers for a given window, return highest-confidence letter with its probability
- [ ] 4.2 Implement `translate-numseq`: loop through sequence for type-1, classify directly for type-0
- [ ] 4.3 Implement `tra/translate "classification"` multimethod: extract cnumseq, classify, add to `:letters`

## 5. Context Preparation

- [ ] 5.1 Implement `tra/prepare-translation "classification"` multimethod: load pickled references from config path

## 6. Tests

- [ ] 6.1 Add test namespace `steno.translators.classification-test` with tests for feature extraction, classification, and type-0/type-1 translation
