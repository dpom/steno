# Skeleton Gap Repair — Design

**Date:** 2026-08-28
**Status:** Approved (design)
**Area:** `src/steno/imageprocessor.lpy`

## Problem

The new image-processing method (`process-image1` / `process-image2` + score-based
selection in `process-image-best`) sometimes over-thins a single Duployer stroke so
that it is **disconnected into multiple fragments**.

The converter's tracer (`matrix-to-lineseq` → `get-liniar-sequence`) follows one
*connected* 1px path. When it reaches a gap it stops, so a fragmented sign is emitted
as several short, incomplete `lsign`s instead of one continuous one. The extraction
"stops at the first gap."

Root cause is in the image layer (broken skeleton), so the fix lives there; the
converter stays untouched.

## Goal

Reconnect broken skeleton fragments so the skeleton is a continuous 1px stroke again,
without merging genuinely separate strokes and without regressing on clean glyphs.

## Approach — Endpoint Bridging (A2) + score guard

A new `repair-skeleton` step bridges dangling endpoints of a broken stroke.

### Data flow

```
process-image (public)
  └─ process-image-best  → picks best of process-image1 / process-image2
  └─ repair-skeleton     → bridges gaps
  └─ guard: keep repaired iff skeleton-score(repaired) <= skeleton-score(best)
```

`process-image-best` remains pure (unchanged) for tests.

### Mechanism (8-connected)

1. **Endpoints:** foreground pixel with exactly one 8-neighbor (degree-1).
2. For each endpoint `E`:
   - search a square window of half-width `gap-radius` for the nearest *other*
     foreground pixel `F`, excluding `E`'s single immediate stroke-neighbor;
   - if `F` found and `E`≠`F`, draw a 1px bridge with `cv2/line` (thickness 1,
     color 255). No hand-rolled Bresenham needed.
3. **Iterate** up to `max-iterations` (or until the connected-component count stops
   changing) so gaps slightly wider than `gap-radius` still close.

### Why it is safe

- A2 only links a dangling endpoint to the nearest nearby pixel. Two genuinely
  separate *closed* strokes have no facing endpoint, so they are not merged.
- The score guard guarantees no regression: `skeleton-score` penalises fragment
  count (`n`) and thickness. Bridging reduces `n`; if a bridge ever worsened the
  score, the original skeleton is returned. Repair can therefore never make a glyph
  worse.

## New module-level parameters (in `imageprocessor.lpy`)

- `gap-radius` — default `3` (px). Max bridge distance from an endpoint.
- `max-iterations` — default `2`. Cap on bridging passes per glyph.

## Interface

```clojure
(defn repair-skeleton
  "Bridge small gaps in a 1px skeleton by connecting nearby dangling endpoints.
   Returns the (possibly) repaired 0/255 uint8 skeleton."
  [image])

(defn process-image
  "Process IMAGE into a clean 1px stenogram, auto-selecting the better
   skeletonization strategy (ADR-0001) and repairing over-thinning gaps."
  [image]
  (let [best (process-image-best image)
        repaired (repair-skeleton best)]
    (if (<= (skeleton-score repaired) (skeleton-score best))
      repaired
      best)))
```

## Testing

- REPL/comment checks appended to `imageprocessor.lpy` (project convention):
  take a known fragmented glyph, show before/after `repair-skeleton`, assert
  connected-component count drops and the converter yields a single continuous
  `lsign`.
- `compare-glyph-batch` (in `converter.lpy`) re-run on a corpus subset to confirm
  lsign counts move toward the curated counts on previously-fragmented glyphs.

## Out of scope

- Switching thinning algorithms (Lee), adjusting `process-image1/2` preprocessing,
  or making the converter gap-tolerant. These were considered (A1/A3) and rejected
  in favour of A2.
