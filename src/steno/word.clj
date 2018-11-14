(ns steno.word
  "Extract and manipulate steno words."
  (:require
      [clojure.set :as st]
      [clojure.spec.alpha :as s]
      [clojure.spec.test.alpha :as stest]
      [clojure.edn :as edn]
      [clojure.java.io :as io]
      [clojure.test :as tst]
      [com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]))

 
(def word-dims [300 200])


;; Specs

(s/def ::row (s/and nat-int? #(< % 5000)))
(s/def ::column (s/and nat-int? #(< % 5000)))
(s/def ::point (s/tuple ::row ::column))
(s/def ::points (s/coll-of ::point :distinct true :into (sorted-set)))
(s/def ::word ::points)

;; w, nw, n, ne, e, se, s, sw
(def directions [[0 -1] [-1 -1] [-1 0] [-1 1]  [0 1] [1 1] [1 0] [1 -1]]) 

(s/def ::direction (set directions))

;; Functions

(defn valid-point?
  [[x y]]
  (and (nat-int? x) (nat-int? y)))

(defn get-neighbor
  [blacks [x y] [dx dy]]
  (let [point [(+ x dx) (+ y dy)]]
    (if (and (valid-point? point) (contains? blacks point))
      point)))

(s/fdef get-neighbor
  :args (s/and (s/cat :blacks ::points
                      :point ::point
                      :dir ::direction)
               #(pos-int? (count (:blacks %))))
  :ret (s/or :rnil nil?
             :rpoint ::point))

(stest/instrument `get-neighbor)

(defn get-one-neighbors
  [blacks point]
  (->> directions
       (map #(get-neighbor blacks point %))
       (remove nil?)))


(s/fdef get-one-neighbors
  :args (s/and (s/cat :blacks ::points
                      :point ::point)
               #(pos-int? (count (:blacks %))))
  :ret (s/coll-of ::point :min-count 0))

(stest/instrument `get-one-neighbors)

;; (stest/check `get-one-neighbors)

(defn get-word
  [blacks]
  (loop [pixels #{(first blacks)}
         word #{(first blacks)}]
    ;; (printf "pixels: %s, word: %s\n" pixels word)
    (if (empty? pixels)
      word
      (let [next-pixels (set (mapcat #(get-one-neighbors blacks %) pixels))]
        (recur (st/difference next-pixels word) (st/union word next-pixels))))))


(s/fdef get-word
  :args (s/and (s/cat :blacks ::points)
               #(pos-int? (count (:blacks %))))
  :ret ::points)

(stest/instrument `get-word)

;; (s/exercise-fn `get-word 1) 

;; (stest/summarize-results (stest/check `get-word)) 


(defn compare-points
  "Compare 2 points."
  [[x1 y1] [x2 y2]]
  (let [c (compare x1 x2)]
    (if (zero? c)
      (compare y1 y2)
      c)))


(defn get-words
  [blacks]
  (loop [bks (apply sorted-set-by compare-points blacks)
         words []]
    (if (empty? bks)
        words
        (let [word (get-word bks)]
             (recur (st/difference bks word) (conj words word))))))

(s/fdef get-words
  :args (s/cat :blacks ::points)
  :ret (s/coll-of ::points :min-count 0))

(stest/instrument `get-words)

;; (s/exercise-fn `get-words 1) 

;; (stest/summarize-results (stest/check `get-words)) 




(defn stats-word
  [func word]
  (reduce (fn [[x1 y1] [x2 y2]] [(func x1 x2) (func y1 y2)]) word))

(s/fdef stats-word
  :args (s/and (s/cat :func tst/function? :word ::points)
               #(pos-int? (count (:word %))))
  :ret ::point)

(stest/instrument `stats-word)

;; (s/exercise-fn `stats-word 1) 

;; (stest/summarize-results (stest/check `stats-word)) 



(defn normalize-word
  [word]
  (let [[min-x min-y] (stats-word min word)]
    (transform [ALL] (fn [[x y]] [(- x min-x) (- y min-y)]) word)))

(s/fdef normalize-word
  :args (s/and (s/cat :word ::points)
               #(pos-int? (count (:word %))))
  :ret ::points)

(stest/instrument `normalize-word)

;; (s/exercise-fn `normalize-word 1) 

;; (stest/summarize-results (stest/check `normalize-word)) 



(defn transform-words!
  [in-file out-file func]
  (with-open [rdr (io/reader in-file)]
    (set! *print-length* -1)
    (with-open [w (io/writer out-file)]
      (doseq [line (line-seq rdr)]
        (if-let [word (edn/read-string line)]
          (.write w (prn-str (func word))))))))

(defn get-gaps
  [blacks line?]
  (let [func (if line? first second)
        blk (set (mapv func blacks))
        max-val (inc (apply max blk))
        mis (apply sorted-set (st/difference (set (range max-val)) blk))
        gaps (conj (:res (reduce (fn [{:keys [prev res]} elm]
                                     {:prev elm :res (if (> (- elm prev) 1) (conj res prev) res)})
                                 {:prev 0 :res []}
                                 mis))
                   max-val)]
    (if (zero? (first gaps))
        (vec (rest gaps))
        gaps)))

(s/fdef get-gaps
  :args (s/and (s/cat :blacks ::points :line? boolean?)
               #(pos-int? (count (:blacks %))))
  :ret (s/coll-of ::row :distinct true))

(stest/instrument `get-gaps)

;; (s/exercise-fn `get-gaps 1) 

;; (stest/summarize-results (stest/check `get-gaps)) 

(defn split-zones
  [blacks line?]
  (let [gaps (get-gaps blacks line?)
        func (if line? first second)]
    (if (seq gaps)
      (:zones (reduce (fn [{:keys [blk zones]} gap]
                        (let [zone (set (filter #(< (func %) gap) blk))]
                          {:blk (st/difference blk zone) :zones (conj zones zone)}))
                      {:blk blacks :zones []}
                      gaps))
      [blacks])))


(s/fdef split-zones
  :args (s/and (s/cat :blacks ::points :line? boolean?)
               #(pos-int? (count (:blacks %))))
  :ret (s/coll-of ::points :distinct true))

(stest/instrument `split-zones)

;; (s/exercise-fn `split-zones 1) 

;; (stest/summarize-results (stest/check `split-zones)) 

(defn get-words-as-file!
  [blacks filename]
  (with-bindings {#'*print-length* -1}
    (spit filename "" :append false)
    (let [page-words (pmap get-words (remove empty? (mapcat #(split-zones % false) (split-zones blacks true))))]
      (doseq [words page-words]
        (doseq [word words]
          (spit filename (prn-str word) :append true))))))


