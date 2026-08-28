# Skeleton Gap Repair — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reconnect over-thinned, disconnected 1px skeleton fragments in `imageprocessor` so the converter traces one continuous stroke instead of stopping at the first gap.

**Architecture:** Add a `repair-skeleton` step that finds dangling endpoints (degree-1 pixels) of the 1px skeleton and bridges each to the nearest nearby foreground pixel with a 1px `cv2.line`. `process-image` calls it after `process-image-best` and keeps the result only if `skeleton-score` does not regress. The converter is untouched.

**Tech Stack:** Basilisp (.lpy) on the Python VM; `cv2`, `numpy`, `skimage` (already imported in `src/steno/imageprocessor.lpy`).

## Global Constraints

- New parameters: `gap-radius` default `3` (px), `max-iterations` default `2`.
- Reuse the existing `skeleton-score` (fragment count + thickness) as the "only-if-improves" guard.
- `process-image-best` stays pure (unchanged) for tests.
- The converter (`src/steno/converter.lpy`) is NOT modified.
- Public function style: docstrings required; predicates `?`, mutators `!`, private `defn-`.
- Headless check runner (pytest+basilisp is broken here — `test/` collides with stdlib `test`): `PYTHONPATH=src uv run basilisp run <file.lpy>`.

---

## Task 1: `repair-skeleton` + helpers

**Files:**
- Modify: `src/steno/imageprocessor.lpy` (add imports `python`, add params + fns)
- Create: `test/steno/imageprocessor_test.lpy` (headless check script)

**Interfaces:**
- Produces: `repair-skeleton [image]` → `uint8` 0/255 1px skeleton (repaired). Consumed by Task 2 (`process-image`).
- Consumes: nothing new (uses existing `cv2`, `numpy`, `skimage` imports).

- [ ] **Step 1: Write the failing headless check**

Create `test/steno/imageprocessor_test.lpy`:

```clojure
(require '[steno.imageprocessor :as img])
(import [cv2] [numpy :as np])

(defn- check!
  [cond msg]
  (when-not cond
    (throw (Exception. msg))))

;; A vertical stroke broken by a 2px gap (two fragments)
(def broken
  (np/array [[0 0 255 0 0]
             [0 0 255 0 0]
             [0 0   0 0 0]
             [0 0 255 0 0]
             [0 0 255 0 0]] ** :dtype np/uint8))
(check! (= 2 (first (cv2/connectedComponents broken nil 8)))
        "broken stroke must start as 2 components")
(def fixed (img/repair-skeleton broken))
(check! (= 1 (first (cv2/connectedComponents fixed nil 8)))
        "repair must reconnect to 1 component")

;; A clean continuous stroke must be left unchanged
(def clean
  (np/array [[0 0 255 0 0]
             [0 0 255 0 0]
             [0 0 255 0 0]
             [0 0 255 0 0]
             [0 0 255 0 0]] ** :dtype np/uint8))
(def clean-fixed (img/repair-skeleton clean))
(check! (np/array_equal clean clean-fixed)
        "clean stroke must be unchanged by repair")

(println "imageprocessor repair checks PASSED")
```

- [ ] **Step 2: Run check to verify it fails**

Run: `PYTHONPATH=src uv run basilisp run test/steno/imageprocessor_test.lpy`
Expected: error `unable to resolve symbol 'img/repair-skeleton'` (function not yet defined).

- [ ] **Step 3: Add imports and parameters to `imageprocessor.lpy`**

In the `ns` form, extend the `:import` block to include python:

```clojure
  (:import
   cv2
   [numpy :as np]
   [python :as py]
   [skimage.morphology :as skim]
   [skimage.util :as skiu]))
```

Add after `process-image2` (before `skeleton-score`):

```clojure
(def gap-radius 3
  "Max pixel distance from an endpoint within which a bridge may be drawn.")

(def max-iterations 2
  "Cap on bridging passes per glyph (stops early if component count stabilises).")

(defn- all-dirs-8
  "The eight 8-connected directions."
  [[0  1] [1  1] [1  0] [1 -1] [0 -1] [-1 -1] [-1  0] [-1  1]])

(defn- bin-u8
  "Foreground mask of a 0/255 skeleton as uint8 0/1."
  [img]
  (.astype (np/greater img 0) np/uint8))

(defn- neighbor-counts
  "8-connected foreground neighbour count for every pixel."
  [bin-img]
  (let [kernel (np/ones [3 3] np/uint8)
        summed (cv2/filter2D bin-img -1 kernel)]
    (np/subtract summed bin-img)))

(defn- endpoint-coords
  "Coordinates of degree-1 foreground pixels (dangling ends)."
  [img]
  (let [bin-img (bin-u8 img)
        nbr (neighbor-counts bin-img)
        cond (np/logical_and (np/equal bin-img 1) (np/equal nbr 1))
        [rs cs] (np/where cond)]
    (map (fn [r c] [r c]) rs cs)))

(defn- continuation-neighbor
  "The single foreground neighbour of an endpoint (the along-stroke direction)."
  [img [r c]]
  (->> all-dirs-8
       (some (fn [[dr dc]]
               (let [nr (+ r dr) nc (+ c dc)
                     h (first (.- img shape)) w (second (.- img shape))]
                 (when (and (>= nr 0) (< nr h) (>= nc 0) (< nc w)
                            (> (aget img nr nc) 0))
                   [nr nc]))))))

(defn- nearest-foreground
  "Nearest other foreground pixel to [r c] within `gap-radius`, excluding EXCLUDE.
   Returns [y x] or nil."
  [img [r c] exclude]
  (let [R gap-radius
        h (first (.- img shape)) w (second (.- img shape))
        y0 (max 0 (- r R)) y1 (min h (+ r R 1))
        x0 (max 0 (- c R)) x1 (min w (+ c R 1))]
    (loop [y y0 best nil best-d (inc (* 2 R R))]
      (if (>= y y1)
        best
        (recur (inc y)
               (loop [x x0 b best bd best-d]
                 (if (>= x x1)
                   b
                   (let [same? (and (= y r) (= x c))
                         excl? (contains? exclude [y x])
                         fg? (> (aget img y x) 0)
                         d (+ (* (- y r) (- y r)) (* (- x c) (- x c)))]
                     (if (and (not same?) (not excl?) fg? (< d bd))
                       (recur (inc x) [y x] d)
                       (recur (inc x) b bd))))))))))

(defn- component-count
  "Number of 8-connected components in a 0/255 image."
  [img]
  (first (cv2/connectedComponents img nil 8)))

(defn repair-skeleton
  "Bridge small gaps in a 1px skeleton by connecting nearby dangling endpoints.
   Returns the (possibly) repaired 0/255 uint8 skeleton."
  [image]
  (let [img (np/copy image)]
    (loop [it 0 prev-n (component-count img)]
      (if (>= it max-iterations)
        img
        (let [eps (endpoint-coords img)]
          (if (empty? eps)
            img
            (do
              (doseq [E eps]
                (let [N (continuation-neighbor img E)
                      F (nearest-foreground img E (if N #{N} #{}))]
                  (when F
                    (cv2/line img (py/tuple [(second E) (first E)])
                              (py/tuple [(second F) (first F)]) 255 1))))
              (let [new-n (component-count img)]
                (if (= new-n prev-n)
                  img
                  (recur (inc it) new-n))))))))))
```

- [ ] **Step 4: Run check to verify it passes**

Run: `PYTHONPATH=src uv run basilisp run test/steno/imageprocessor_test.lpy`
Expected: prints `imageprocessor repair checks PASSED`, exit 0.

- [ ] **Step 5: Commit**

```bash
git add src/steno/imageprocessor.lpy test/steno/imageprocessor_test.lpy
git commit -m "feat(imageprocessor): add repair-skeleton to bridge over-thinned gaps"
```

---

## Task 2: Wire `repair-skeleton` into `process-image` + guard

**Files:**
- Modify: `src/steno/imageprocessor.lpy` (`process-image` fn)
- Modify: `src/steno/imageprocessor.lpy` (REPL comment block at end)

**Interfaces:**
- Produces: `process-image [image]` now returns the guarded, repaired skeleton.
- Consumes: `repair-skeleton` (Task 1), existing `skeleton-score`, `process-image-best`.

- [ ] **Step 1: Update `process-image` to repair + guard**

Replace the current body:

```clojure
(defn process-image
  "Process the IMAGE into a clear 1 pixel thick stenogram by auto-selecting
   the better of the two skeletonization strategies (ADR-0001), then repairing
   over-thinning gaps so a single stroke stays continuous."
  [image]
  (let [best (process-image-best image)
        repaired (repair-skeleton best)]
    (if (<= (skeleton-score repaired) (skeleton-score best))
      repaired
      best)))
```

- [ ] **Step 2: Add a REPL/comment check (project convention) at end of file**

Append inside the existing `(comment ...)` block:

```clojure
   (require '[steno.extractor :as extr]
            '[steno.converter :as cnv]
            '[steno.show :as show]
            '[steno.utils :as utl])

   (def mtx (extr/extract-glyphs "test/resources/template.png"))

   (defn show-repair
     [row col glyph]
     (let [skel (process-image (:roi glyph))]
       {:row row :col col :wsign (cnv/image-to-wsign skel)}))

   (def mtx-r (utl/map-glyph-matrix mtx show-repair))

   ;; Inspect a previously fragmented glyph: it should now yield one
   ;; continuous lsign instead of several short ones.
   (show/show-glyph-matrix mtx-r)

 ;;
```

- [ ] **Step 3: Verify end-to-end via the committed check file**

Run: `PYTHONPATH=src uv run basilisp run test/steno/imageprocessor_test.lpy`
Expected: prints `imageprocessor repair checks PASSED` (the clean-stroke case now also
exercises `process-image`'s guard path indirectly through `repair-skeleton`, which is
the only behviour that changed).

- [ ] **Step 4: Commit**

```bash
git add src/steno/imageprocessor.lpy
git commit -m "feat(imageprocessor): repair gaps in process-image with score guard"
```

---

## Task 3: Regression check against the corpus (manual)

**Files:**
- Read: `src/steno/converter.lpy` (`compare-glyph-batch`)

**Interfaces:**
- Consumes: `compare-glyph-batch` (already exists in `converter.lpy`).

- [ ] **Step 1: Generate glyphs + run the batch comparator**

Generate glyph PNGs (extraction step) and run the existing comparator to confirm
fragmented glyphs now produce fewer `lsign`s and existing glyphs do not regress:

```bash
bb app test/resources/template.png tmp/repair-check/
PYTHONPATH=src uv run basilisp run -e '(require (quote [steno.converter :as cnv])) (cnv/compare-glyph-batch "tmp/repair-check" "resources/corpus")'
```

Expected: for previously-fragmented glyphs the `legacy=` lsign count drops toward
the `curated=` count; no glyph shows a large *increase* in count.

- [ ] **Step 2: Commit any supporting artifacts only if generated (otherwise skip)**

Only commit if you deliberately added a fixture; do not commit `tmp/`.

```bash
git status   # confirm only intended files are staged
```

---

## Self-Review Notes

- Spec coverage: A2 bridging ✓ (Task 1), score guard ✓ (Task 2), `gap-radius`/`max-iterations` params ✓, `process-image-best` kept pure ✓, converter untouched ✓.
- No placeholders: every step has concrete code or an exact command.
- Type consistency: `repair-skeleton` returns uint8 0/255 image; `process-image` returns the same type as before (consumers unchanged). `cv2.line` receives `(py/tuple [x y])` points.
- Known limitation (documented, not in scope): a bridge connects an endpoint to its *nearest* nearby foreground, excluding only the immediate continuation neighbour; in rare U-shaped fragments this could bridge across the concavity. `gap-radius` is small (3px) and the score guard bounds any regression.
