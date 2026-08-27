# Design: Graph-based lsign extraction

- **Date:** 2026-08-27
- **Status:** approved (design)
- **Scope:** Replace the skeleton-tracing lsign extractor in `converter.lpy` with a
  topology-driven graph decomposition, and add an evaluation harness so the new
  method can be compared against the curated corpus and the legacy tracer.

## Problem

Today a glyph (word image) becomes a `wsign` by skeletonizing it (1-px) and
tracing the skeleton with `matrix-to-lineseq` / `get-liniar-sequence` /
`get-parallel-sequence` / `process-lineseq`. That tracer produces the **wrong
number of lsigns**: two lsigns get merged into one, or one lsign is split into
two. The cause is the fragile tracing order and the heuristic merge/split logic
in `process-lineseq`, not the skeleton quality (skeletonization was recently
improved and auto-selected per glyph).

The user wants the most consistent, translator-friendly lsigns and has no fixed
rule set yet — so the work is experimental, validated against the curated
`resources/corpus`.

## Goal

Extract lsigns from a 1-px skeleton using the skeleton's real topology, so that
an lsign is bounded by junctions/endpoints/loops rather than by tracing order.
Keep the public `wsign` data contract unchanged so the translator, checker GUI,
and corpus are unaffected.

## Non-goals

- Changing skeletonization (`imageprocessor`) — already handled separately.
- A learned / curvature-based stroke detector (deferred; option C).
- Changing the `lineseq` cell-number encoding or the translator.

## Approach (A — skeleton graph decomposition)

### Data contract (unchanged)

`image-to-wsign` returns a `wsign`: a vector of lsign maps

```
{:ltype <0 loop | 1 linear>
 :lineseq [[x y n] ...]}
```

where `n` is the 8-direction cell number already produced by
`get-cell-number-and-neighbors`. The translator, checker, and corpus consume
this shape, so it must be preserved exactly.

### Components (all in `converter.lpy`)

1. **`skeleton-graph`** — given the binary matrix, build a graph where each
   foreground pixel is a node and each 8-connected foreground neighbor is an edge.
   Reuses the existing `get-cell-number-and-neighbors` helper (no new
   dependency; implemented manually on the grid, no networkx).

2. **`decompose-lsigns`** — classify nodes:
   - endpoint = degree 1
   - junction = degree >= 3
   - interior = degree 2
   Then break the skeleton at every junction and walk each residual path:
   - endpoint→endpoint and junction→endpoint paths are **linear lsigns**;
   - two chords sharing the same ordered pair of junctions form a **loop**
     (one lsign, `ltype` 0); a single chord between two junctions is emitted as
     a linear lsign (safe fallback);
   - odd chord counts at a junction fall back to emitting each chord as its own
     linear lsign (never throws, never fuses).
   Returns the same "vector of lineseqs" shape that legacy `matrix-to-lineseq`
   returned.

3. **`image-to-wsign`** — calls `decompose-lsigns` then `lineseq-to-lsign`
   (reused) with the existing `normalize-loop` / `normalize-liniar` /
   `normalize1-lsign` / `normalize2-lsign`. The fragile `process-lineseq`
   merge/split step is no longer used by the default path.

4. **`image-to-wsign-legacy`** — the current implementation, kept intact for
   A/B comparison.

### Data flow

```
glyph image
  -> img/process-image            (skeleton, 1-px, uint8)
  -> image-to-matrix             (binary 0/1 matrix)
  -> skeleton-graph
  -> decompose-lsigns            (vector of lineseqs)
  -> lineseq-to-lsign + normalize*   (vector of lsign)
  => wsign
```

### Error handling

- Blank / empty skeleton -> empty `wsign` (no crash).
- Isolated single foreground pixels (degree 0) -> ignored.
- Self-touching loops and multi-junction nodes -> handled by chord pairing;
  odd chord counts degrade gracefully to linear lsigns.
- No new exceptions introduced; existing `try/catch` in `get-cell-value` is kept.

## Evaluation harness

Because `resources/corpus` holds curated lsign entries (built in the checker),
the new extractor can be scored against ground truth instead of eyeballing.

- **Batch script** (REPL-driven, in `converter.lpy` comment block and/or
  `test/steno`): for a set of glyph images, emit old vs new `wsign`.
- **Corpus score** (when a curated entry exists for the image): compare
  - lsign count,
  - multiset of `ltype`s,
  - a translation + rotation normalized `lineseq` match per lsign.
- **Visual**: `show/show-lsign` side-by-side for manual inspection.

Success criterion: the new extractor reproduces the curated lsigns at least as
well as (ideally better than) the legacy tracer, with fewer wrong merge/split
cases.

## Testing

Unit tests added in `converter.lpy`'s comment block (and/or `test/steno`):
- synthetic skeleton with a clean **fork** -> expected lsign count + ltypes;
- synthetic **loop** -> one `ltype` 0 lsign;
- synthetic **merged pair** (two strokes touching at a point) -> two lsigns,
  not one.

## Migration

- Default `image-to-wsign` switches to graph decomposition.
- `image-to-wsign-legacy` preserved; removed only after the eval harness shows
  the new method is consistently better across the curated corpus.
