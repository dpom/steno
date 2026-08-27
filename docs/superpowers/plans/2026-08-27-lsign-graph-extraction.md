# Graph-based lsign Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the fragile skeleton-tracing lsign extractor in `converter.lpy` with a topology-driven graph decomposition so an lsign is bounded by real skeleton junctions/endpoints/loops, keeping the `wsign` data contract unchanged.

**Architecture:** `image-to-wsign` builds a graph of the 1-px skeleton, breaks it at junctions (degree ≥ 3) and endpoints (degree 1), walks each residual path, and pairs chords between the same two junctions into loops. Each path/cycle becomes a `lineseq` in the same `[x y n]` format the translator already consumes. The legacy tracer is kept as `image-to-wsign-legacy` for A/B comparison.

**Tech Stack:** Basilisp (.lpy) on the Python VM; OpenCV / numpy / skimage (already used). No new dependencies.

## Global Constraints

- Public `wsign` contract unchanged: vector of `{:ltype (0|1) :lineseq [[x y n] ...]}` — reused by translator, checker GUI, and corpus.
- No new third-party dependencies (implement the graph walk manually; do not add networkx).
- Source files are Basilisp `.lpy`; follow existing `converter.lpy` style: kebab-case vars, docstring on every public fn, `defn-` for private, `:require`/`::import` at top.
- `image-to-wsign` must remain the public entry point; the old behavior is preserved under `image-to-wsign-legacy`.
- Commits are frequent and small (one per task).

---

### Task 1: Failing unit tests for graph decomposition

**Files:**
- Create: `test/steno/converter_test.lpy`

**Interfaces:**
- Consumes: `steno.converter/image-to-wsign` (public, `[image] -> wsign`)
- Produces: a runnable test script asserting lsign counts and loop `ltype` on synthetic skeletons.

- [ ] **Step 1: Write the failing test**

```clojure
(require '[steno.converter :as cnv])
(import [numpy :as np])

(def fork
  (np/array [[0 0 0 0 0]
             [0 1 0 0 0]
             [0 1 0 0 0]
             [1 1 1 0 0]
             [0 0 0 0 0]]))

(def loopm
  (np/array [[0 1 1 1 0]
             [0 1 0 1 0]
             [0 1 0 1 0]
             [0 1 1 1 0]]))

(def cross
  (np/array [[0 0 1 0 0]
             [0 0 1 0 0]
             [1 1 1 1 1]
             [0 0 1 0 0]
             [0 0 1 0 0]]))

(assert (= 3 (count (cnv/image-to-wsign fork)))
        "T-junction must yield 3 linear lsigns")
(let [w (cnv/image-to-wsign loopm)]
  (assert (= 1 (count w)) "square ring must yield 1 lsign")
  (assert (= 0 (:ltype (first w))) "ring must be ltype 0 (loop)"))
(assert (= 4 (count (cnv/image-to-wsign cross)))
        "cross must yield 4 linear lsigns, not 1")
(println "converter graph-extraction tests passed")
```

- [ ] **Step 2: Run test to verify it fails**

Run: `uv run basilisp run test/steno/converter_test.lpy`
Expected: error — `image-to-wsign` currently uses the legacy tracer which (on these synthetic matrices) returns the wrong counts / no `:ltype` mismatch caught, or the run raises before tests pass. Tests must NOT print "converter graph-extraction tests passed".

- [ ] **Step 3: Commit (test only, expected to fail)**

```bash
git add test/steno/converter_test.lpy
git commit -m "test: add graph-decomposition unit tests for lsign extraction"
```

---

### Task 2: Skeleton graph + decomposition helpers

**Files:**
- Modify: `src/steno/converter.lpy` (add private helpers before `image-to-wsign`)

**Interfaces:**
- Consumes: existing `get-cell-number-and-neighbors`, `directions` (both already defined in `converter.lpy`)
- Produces: `skeleton-graph`, `g-neighbors`, `node-degree`, `break-node?`, `walk-segment`, `walk-loop`, `pixels->lineseq`, `group-into-lsigns`, `decompose-lsigns` (all `defn-`). `decompose-lsigns` returns a vector of lineseqs (`[[x y n] ...]`), identical in shape to the legacy `matrix-to-lineseq` output.

- [ ] **Step 1: Add the helpers**

Insert after `image-to-matrix` (before `get-cell-value` is fine, but they only need `get-cell-number-and-neighbors`):

```clojure
(defn- fg-neighbors
  "8-connected foreground neighbor coordinates of CELL in the binary MAT."
  [mat cell]
  (let [[ns _] (get-cell-number-and-neighbors mat cell)]
    (vec ns)))

(defn- skeleton-graph
  "Build the skeleton graph: cell -> vector of 8-connected foreground neighbors."
  [mat]
  (let [rows (count mat)
        cols (count (first mat))]
    (into {}
          (for [i (range rows)
                j (range cols)
                :when (= 1 (get-in mat [i j]))]
            [[i j] (fg-neighbors mat [i j])]))))

(defn- g-neighbors
  [graph cell]
  (get graph cell []))

(defn- node-degree
  [graph cell]
  (count (g-neighbors graph cell)))

(defn- break-node?
  "True for a stroke endpoint (degree 1) or junction (degree >= 3)."
  [graph cell]
  (let [d (node-degree graph cell)]
    (or (<= d 1) (>= d 3))))

(defn- walk-segment
  "Walk a path from START (first interior pixel) away from PREV until hitting a
   break node, an already-visited pixel, or a dead end. Returns the pixel list
   (excluding PREV) and the terminal cell."
  [graph visited start prev]
  (loop [cur start
         prv prev
         acc []]
    (if (break-node? graph cur)
      {:pixels (conj acc cur) :end cur}
      (let [nxt (first (->> (g-neighbors graph cur)
                            (remove #(= % prv))
                            (remove #(contains? @visited %))))]
        (if nxt
          (recur nxt cur (conj acc cur))
          {:pixels (conj acc cur) :end cur})))))

(defn- walk-loop
  "Walk a closed loop (component with no break nodes) from START."
  [graph visited start]
  (loop [cur start
         prv nil
         acc []]
    (let [nxt (first (->> (g-neighbors graph cur)
                          (remove #(= % prv))
                          (remove #(contains? @visited %)))])
      (if (and nxt (not= nxt start))
        (recur nxt cur (conj acc cur))
        (conj acc cur)))))

(defn- pixels->lineseq
  "Turn a pixel path into a lineseq of [x y n] cells (n = direction number)."
  [mat pixels]
  (mapv (fn [c]
          (let [[_ n] (get-cell-number-and-neighbors mat c)]
            (conj c n)))
        pixels))

(defn- group-into-lsigns
  "Convert collected segments to lineseqs. Two chords sharing the same two
   junctions form one loop lsign; everything else is a linear lsign. Drop
   degenerate (< 2 pixel) segments."
  [mat segments]
  (let [by-pair (group-by (fn [s] (set (:ends s))) segments)]
    (mapcat (fn [[_ segs]]
              (if (= (count segs) 2)
                (let [[a b] segs
                      pixels (vec (concat (:pixels a) (reverse (:pixels b))))]
                  [(pixels->lineseq mat pixels)])
                (->> segs
                     (filter #(>= (count (:pixels %)) 2))
                     (map #(pixels->lineseq mat (:pixels %)))))
            (vals by-pair))))

(defn- decompose-lsigns
  "Decompose a binary matrix (1/0) into a vector of lineseqs, one per lsign,
   bounded by skeleton junctions/endpoints; loops are cycles between two
   junctions."
  [mat]
  (let [graph (skeleton-graph mat)
        visited (volatile! #{})
        segments (volatile! [])]
    (doseq [c (keys graph)]
      (when (break-node? graph c)
        (doseq [n (->> (g-neighbors graph c)
                       (remove #(contains? @visited %)))]
          (let [{:keys [pixels end]} (walk-segment graph visited n c)]
            (vswap! visited into pixels)
            (vswap! segments conj {:pixels (conj pixels c) :ends [c end]}))))
    (doseq [c (keys graph)]
      (when (not (contains? @visited c))
        (let [pixels (walk-loop graph visited c)]
          (vswap! visited into pixels)
          (vswap! segments conj {:pixels pixels
                                 :ends [(first pixels) (first pixels)]}))))
    (group-into-lsigns mat @segments)))
```

- [ ] **Step 2: Run the tests (still expected to fail — `image-to-wsign` not rewired)**

Run: `uv run basilisp run test/steno/converter_test.lpy`
Expected: FAIL (legacy `image-to-wsign` still in place; counts/loop type wrong).

- [ ] **Step 3: Commit (helpers only)**

```bash
git add src/steno/converter.lpy
git commit -m "feat(converter): add skeleton graph decomposition helpers"
```

---

### Task 3: Wire graph decomposition into `image-to-wsign`

**Files:**
- Modify: `src/steno/converter.lpy` (rename current `image-to-wsign` -> `image-to-wsign-legacy`; new `image-to-wsign` uses `decompose-lsigns`)

**Interfaces:**
- Consumes: `decompose-lsigns` (Task 2), existing `lineseq-to-lsign`, `normalize1-lsign`, `normalize2-lsign`, `compare-cell`, `image-to-matrix`
- Produces: public `image-to-wsign` (graph-based) and `image-to-wsign-legacy` (old tracer).

- [ ] **Step 1: Replace the `image-to-wsign` entry point**

Replace the existing `image-to-wsign` with both definitions:

```clojure
(defn image-to-wsign-legacy
  "Legacy skeleton-tracing extractor (kept for A/B comparison)."
  [image]
  (->> image
       image-to-matrix
       matrix-to-lineseq
       lineseqs-to-wsign
       (map normalize1-lsign)
       (sort-by #(first (:lineseq %)) compare-cell)
       (mapv normalize2-lsign)))

(defn image-to-wsign
  "Convert an image into a wsign (sequence of lsign) using graph decomposition
   of the 1-pixel skeleton: lsigns are bounded by real junctions/endpoints/loops."
  [image]
  (->> image
       image-to-matrix
       decompose-lsigns
       (map lineseq-to-lsign)
       (map normalize1-lsign)
       (sort-by #(first (:lineseq %)) compare-cell)
       (mapv normalize2-lsign)))
```

- [ ] **Step 2: Run the tests to verify they pass**

Run: `uv run basilisp run test/steno/converter_test.lpy`
Expected: prints `converter graph-extraction tests passed`.

- [ ] **Step 3: Smoke-check on a real glyph image**

Run a REPL form (or add to the converter `comment` block):
```clojure
(require '[steno.extractor :as extr] '[steno.imageprocessor :as img])
(def mtx (extr/extract-glyphs "test/resources/template.png"))
(def w (-> (:roi (first (first mtx))) img/process-image cnv/image-to-wsign))
(println "legacy count:" (count (cnv/image-to-wsign-legacy (img/process-image (:roi (first (first mtx)))))))
(println "graph  count:" (count w))
```
Expected: both return a `wsign`; graph count is sane (no obvious over/under-split vs legacy).

- [ ] **Step 4: Commit**

```bash
git add src/steno/converter.lpy
git commit -m "feat(converter): use graph decomposition in image-to-wsign"
```

---

### Task 4: Evaluation harness (legacy vs new vs curated corpus)

**Files:**
- Modify: `src/steno/converter.lpy` (add a `comment` block `extraction-eval`)

**Interfaces:**
- Consumes: `image-to-wsign`, `image-to-wsign-legacy`, `extr/extract-glyphs`, corpus reader from `steno.corpus`
- Produces: a REPL-driven harness that, for a batch of glyph images, prints old vs new lsign counts and a simple match score against curated corpus entries.

- [ ] **Step 1: Add an eval helper + batch block to the `comment` section**

```clojure
(comment

  (require
   '[steno.extractor :as extr]
   '[steno.imageprocessor :as img]
   '[steno.corpus :as corpus]
   '[steno.show :as show]
   '[steno.utils :as utl])

  (defn- wsign-score
    "Simple comparison of two wsigns: 1.0 if lsign counts and ltype multisets
     match, scaled down by count difference otherwise."
    [a b]
    (let [ca (count a) cb (count b)]
      (if (and (= ca cb)
               (= (frequencies (map :ltype a))
                  (frequencies (map :ltype b))))
        1.0
        (/ 1.0 (+ 1 (abs (- ca cb)))))))

  (defn compare-extractors
    "For every glyph image under DIR, print legacy vs graph lsign counts and,
     when a curated corpus entry exists for the same image, the match score."
    [dir]
    (doseq [f (extr/expand-input dir)]
      (let [img (cv2/imread f cv2/IMREAD_GRAYSCALE)
            skel (img/process-image img)
            legacy (cnv/image-to-wsign-legacy skel)
            graph (cnv/image-to-wsign skel)
            corpus-entry (corpus/find-entry (utl/get-filename f))]
        (println (format "%s  legacy=%d graph=%d"
                         (utl/get-filename f) (count legacy) (count graph)))
        (when corpus-entry
          (println (format "   score(graph vs corpus)=%.2f"
                           (wsign-score graph corpus-entry)))))))

  ;; (compare-extractors "tmp/yoga_001_1")
  ;; (show/show-lsign (first (cnv/image-to-wsign (img/process-image (cv2/imread "tmp/yoga_001_1/yoga_001_1-01-00.png" cv2/IMREAD_GRAYSCALE)))))

  ;;
  )
```

Note: adapt `corpus/find-entry` and `utl/get-filename` names to the actual helpers in `corpus.lpy` / `utils.lpy` (verify their arity before relying on them; the scorer degrades gracefully if a corpus entry is missing).

- [ ] **Step 2: Run the harness on a real batch**

Uncomment `(compare-extractors "tmp/yoga_001_1")` in the REPL and confirm output prints legacy/graph counts per glyph without errors.

- [ ] **Step 3: Commit**

```bash
git add src/steno/converter.lpy
git commit -m "feat(converter): add legacy-vs-graph extraction eval harness"
```

---

### Task 5: Manual review and cleanup decision

**Files:**
- Modify: `src/steno/converter.lpy` (only if removing `image-to-wsign-legacy` after confirming the new method is consistently better)

**Interfaces:**
- Consumes: the eval harness output from Task 4.

- [ ] **Step 1: Compare results on the curated corpus**

Run `compare-extractors` over the corpus images (e.g. `resources/corpus/*` glyph files) and confirm the graph extractor reproduces curated lsigns at least as well as the legacy tracer (higher `score` / fewer wrong splits-merges).

- [ ] **Step 2: Decide on legacy removal**

If the graph extractor is consistently better, delete `image-to-wsign-legacy` and its now-unused helpers (`matrix-to-lineseq`, `get-liniar-sequence`, `get-parallel-sequence`, `process-lineseq`, `lineseqs-to-wsign`) — otherwise keep `image-to-wsign-legacy` for continued A/B.

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "chore(converter): drop legacy tracer after graph extractor wins"
```
(only if legacy was removed; otherwise skip)
```
