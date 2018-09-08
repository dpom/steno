(ns steno.page
  "Functions related to page manipulation."
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [steno.word :as wd]
   [mikera.image.core :as mik]
   [mikera.image.filters :as filt]))
  

(def BLACK -16777216)
(def MAX-DIM 3000)
(def MAX-IDX 6000000)

;; Specs

(s/def ::image #(instance? java.awt.image.BufferedImage %))
(s/def ::coordinate (s/and nat-int? #(< % MAX-DIM)))
(s/def ::arrayidx (s/and nat-int? #(< % MAX-IDX)))
(s/def ::width (s/and pos-int? #(< 2 % MAX-DIM)))
(s/def ::height (s/and pos-int? #(< 2 % MAX-DIM)))
(s/def ::pixels #(instance? (Class/forName "[I") %))

(s/def ::page (s/keys :req-un [::image ::width ::height ::pixels]))

;; Implementation

(defrecord Page [image width height pixels])

(defn load-page
  "Load a page from the filename file."
  [filename]
  (let [image (mik/load-image filename)]
    (map->Page {:image (mik/filter-image image (filt/quantize 2))
                :width (mik/width image)
                :height (mik/height image)
                :pixels (mik/get-pixels image)})))

(s/fdef load-page
  :args (s/cat :filename string?)
  :ret ::page)

(stest/instrument `load-page)


(defn show-page
  "Visualise a page."
  [{:keys [image pixels]} & opts]
  (mik/set-pixels image pixels)
  (apply mik/show image opts))



(defn xy2idx
  "Convert cartezian coordinates in array index."
  [page x y]
  (+ x (* y (:width page))))

(s/fdef xy2idx
  :args (s/cat :page (s/keys :req-un [::width]) :x ::coordinate :y ::coordinate)
  :ret ::arrayidx)

(stest/instrument `xy2idx)

;; (s/exercise-fn `xy2idx 1) 

;; (stest/summarize-results (stest/check `xy2idx)) 


(defn idx2xy
  "Convert array index in cartezian coordinates."
  [{:keys [width]} idx]
  [(mod idx width) (quot idx width)])

(s/fdef idx2xy
  :args (s/cat :page (s/keys :req-un [::width])
               :idx  ::arrayidx)
  :ret (s/tuple ::coordinate ::coordinate))

(stest/instrument `idx2xy)

;; (s/exercise-fn `idx2xy 1) 

;; (stest/summarize-results (stest/check `idx2xy)) 

(defn get-black-pixels
  [page]
  (let  [xform (comp
                (map-indexed (fn [idx v] [idx v]))
                (filter (fn [[_ v]] (= v BLACK)))
                (map first)
                (map (fn [idx] (idx2xy page idx))))]
    (into (sorted-set) xform (:pixels page))))

(s/fdef get-black-pixels
  :args (s/cat :page (s/keys :req-un [::pixels]))
  :ret :steno.word/points)

(stest/instrument `get-black-pixels)

;; (s/exercise-fn `get-black-pixels 1) 

;; (stest/summarize-results (stest/check `get-black-pixels)) 


(defn show-word
  "Visualise a steno word."
  ([word] (show-word word true))
  ([word standard?] (let [[w h] (if standard? wd/word-dims (wd/stats-word max word))])))
