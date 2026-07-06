## 1. Reference Data Format

- [x] 1.1 Create `src/steno/translators/knn.lpy` with `build-references` multimethod that collects individual sample sequences per letter from corpus entries
- [x] 1.2 Compute `min-len` and `max-len` per letter in the reference data
- [x] 1.3 Filter out `unknown-letter` entries during reference building

## 2. k-NN Translation Algorithm

- [x] 2.1 Implement `prepare-translation` multimethod to load knn references from edn file
- [x] 2.2 Implement `translate` multimethod with k-NN classification using Hamming distance and sliding window
- [x] 2.3 Implement distance-weighted voting: each neighbor's match score weights its vote, winner = highest sum among k nearest
- [x] 2.4 Handle edge case: fewer samples than k (use all available samples)

## 3. Plugin Registration and Configuration

- [x] 3.1 Add `[steno.translators.knn]` require to `src/steno/core.lpy`
- [x] 3.2 Add `knn_references: "resources/knn-references.edn"` and `knn_k: 3` to `resources/config.yml`

## 4. Tests

- [x] 4.1 Add tests for `build-references`: collects one sample per entry, multiple samples per letter, computes min/max-len, filters unknown-letter
- [x] 4.2 Add tests for `translate`: exact match returns correct letter, sliding window matching, distance-weighted voting, degrades gracefully with fewer samples than k
- [x] 4.3 Add test for configurable k value

## 5. Verification

- [x] 5.1 Verify translation with `bb app translate -t knn` on test images
- [x] 5.2 Run `bb test` to ensure all existing tests still pass
- [x] 5.3 Run `bb kondo` to lint the new source file
- [x] 5.4 Run `openspec validate classifier-translator --type change --strict`
