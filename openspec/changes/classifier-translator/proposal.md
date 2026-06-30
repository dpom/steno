## Why

The existing freq and diff translators both collapse corpus examples into summary statistics (frequency maps, median sequences), discarding multi-modal variation — when a letter has multiple distinct writing styles, neither approach captures it. A k-NN translator that compares against individual corpus examples can better handle varied writing styles and potentially improve accuracy.

## What Changes

- New translator `knn` (k-Nearest Neighbors) in `src/steno/translators/knn.lpy`
- Register `knn` in the translator plugin system (require in `core.lpy`)
- New reference format storing individual sample sequences per letter
- Configurable `k` parameter (default 3) in `resources/config.yml`
- Auto-discovered via `--translators` CLI flag like existing translators

## Capabilities

### New Capabilities
- `knn-translator`: Translate glyph images to letters using k-NN classification with distance-weighted voting, configurable k value, and Hamming distance with sliding window for variable-length cnum sequences

### Modified Capabilities
*(none — new translator only)*

## Impact

- **New file**: `src/steno/translators/knn.lpy` implementing the three multimethod entry points
- **Modified file**: `src/steno/core.lpy` — add `:require [steno.translators.knn]`
- **Modified file**: `resources/config.yml` — add `knn_references` path and optional `knn_k` (k value)
- **Dependencies**: none — uses existing `statistics` module and reuses Hamming distance logic
