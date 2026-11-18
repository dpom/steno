# True Cell Coordinates — Design

**Date:** 2026-11-18
**Status:** Approved (design)
**Area:** `src/steno/utils.lpy`

## Problem

Boolean NumPy masks occur in the conversion pipeline, for example the `junctions`
mask returned by `steno.converter2/get-junctions`. There is no direct way to obtain
the coordinates of the cells whose value is `True` from such a mask. The codebase
currently repeats ad hoc `np/where` + coordinate-mapping code where these coordinates
are needed.

## Goal

Provide one general utility that maps a boolean mask to the coordinates of its
`True` cells, following the existing coordinate conventions in `steno.utils`.

## Interface

```clojure
(defn true-coords
  "Coordinates of the cells whose value is True in a boolean mask."
  [mask])
```

- **Input:** `mask`, a boolean NumPy array (any 2D shape).
- **Output:** lazy seq of `[row column]` pairs of Clojure integers, in row-major
  order as returned by `np/where`.
- Empty mask or all-`False` mask returns an empty seq.
- No exception throwing; follows project nil-punning/logging conventions.

## Implementation

Place `true-coords` in `src/steno/utils.lpy` immediately after `endpoint-coords`,
reusing the same pattern:

```clojure
(defn true-coords
  "Coordinates of the cells whose value is True in a boolean mask."
  [mask]
  (let [[rs cs] (np/where mask)]
    (map (fn [r c] [(int r) (int c)]) rs cs)))
```

`np/where` returns row indices and column indices as two arrays; `map` pairs them
and `int` coerces NumPy scalars to host integers, matching `endpoint-coords`.

## Alternatives considered

- `np/argwhere` returns an Nx2 array directly, but introduces a different result
  shape than existing helpers.
- `np/ndindex` iteration is more verbose with no benefit for this use case.

## Testing

Add `test/steno/utils_test.lpy` in the existing script style (`check!` helper,
final success `println`, picked up by `bb test`):

- all-`False` mask returns no coordinates
- a single `True` cell returns its `[row column]`
- multiple `True` cells, including corners, return all expected pairs in row-major
  order

## Verification

- `bb test`
- `bb kondo src/steno/utils.lpy`
- `bb style src/steno/utils.lpy test/steno/utils_test.lpy`

## Out of scope

- Changing `get-junctions` or any converter behavior.
- Special handling for non-boolean arrays or higher-dimensional arrays.
