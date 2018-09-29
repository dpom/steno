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


(def word-dims [24 24])


;; Specs

(s/def ::x (s/and nat-int? #(< % 5000)))
(s/def ::y (s/and nat-int? #(< % 5000)))
(s/def ::point (s/tuple ::x ::y))
(s/def ::points (s/coll-of ::point :distinct true :into (sorted-set)))
(s/def ::word ::points)

(def directions [[0 1] [-1 1] [1 0] [1 1]]) 

(s/def ::direction (set directions))

;; Functions

(defn get-neighbor
  [blacks [x y] [dx dy]]
  (let [point [(+ x dx) (+ y dy)]]
    (if (contains? blacks point)
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

(defn get-words
  [blacks]
  (loop [bks blacks
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


(defn get-words-as-file!
  [blacks filename]
  (set! *print-length* -1)
  (spit filename "" :append false)
  (loop [bks blacks]
    (if (empty? bks)
      filename
      (let [word (get-word bks)]
        (spit filename (prn-str word) :append true)
        (recur (st/difference bks word))))))

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













