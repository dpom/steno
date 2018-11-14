(ns steno.picture
  "Functions related to picture manipulation."
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [steno.word :as wd]
   [com.rpl.specter :refer [transform ALL]]
   [mikera.image.core :as mik]))

(def WHITE 0xffffffff)
(def BLACK 0xff000000)
(def MASK 0xff)
(def THRESHOLD 200) 

(def MAX-DIM 3000)
(def MAX-IDX 9000000)

;; Specs

(s/def ::image #(instance? java.awt.image.BufferedImage %))
(s/def ::coordinate (s/and nat-int? #(<= % MAX-DIM)))
(s/def ::arrayidx (s/and nat-int? #(<= % MAX-IDX)))
(s/def ::width (s/and pos-int? #(<= 2 % MAX-DIM)))
(s/def ::height (s/and pos-int? #(<= 2 % MAX-DIM)))
(s/def ::pixels #(instance? (Class/forName "[I") %))

(s/def ::picture (s/keys :req-un [::image ::width ::height ::pixels]))

;; Implementation

(defrecord Picture [image width height pixels])

(defn load-picture
  "Load a picture from the filename file."
  [filename]
  (let [image (mik/load-image filename)]
    (map->Picture {:image image
                   :width (mik/width image)
                   :height (mik/height image)
                   :pixels (mik/get-pixels image)})))

(s/fdef load-picture
        :args (s/cat :filename string?)
        :ret ::picture)

(stest/instrument `load-picture)

(defn show-picture!
  "Visualise a picture."
  [{:keys [image pixels]} & opts]
  (mik/set-pixels image pixels)
  (apply mik/show image opts))

(defn xy2idx
  "Convert cartezian coordinates in array index."
  [picture x y]
  (+ y (* x (:width picture))))

(s/fdef xy2idx
        :args (s/and (s/cat :picture (s/keys :req-un [::width ::height]) :x ::coordinate :y ::coordinate)
                     #(and (< (:y %) (-> % :picture :width)) (< (:x %) (-> % :picture :height))))
        :ret ::arrayidx)

(stest/instrument `xy2idx)

;; (s/exercise-fn `xy2idx 1) 

;; (stest/summarize-results (stest/check `xy2idx)) 


(defn idx2xy
  "Convert array index in cartezian coordinates."
  [{:keys [width]} idx]
  [ (quot idx width) (mod idx width)])

(s/fdef idx2xy
        :args (s/and (s/cat :picture (s/keys :req-un [::width ::height])
                            :idx  ::arrayidx)
                     #(< (:idx %) (* (-> % :picture :width) (-> % :picture :height))))
        :ret (s/tuple ::coordinate ::coordinate))

(stest/instrument `idx2xy)

;; (s/exercise-fn `idx2xy 1) 

;; (stest/summarize-results (stest/check `idx2xy)) 

(defn get-black-pixels
  [picture]
  (let  [xform (comp
                (map-indexed (fn [idx v] [idx v]))
                (remove (fn [[_ v]] (>= (bit-and v MASK) THRESHOLD)))
                (map first)
                (map (fn [idx] (idx2xy picture idx))))]
    (into (sorted-set) xform (:pixels picture))))

(s/fdef get-black-pixels
        :args (s/cat :picture (s/keys :req-un [::pixels]))
        :ret :steno.word/points)

(stest/instrument `get-black-pixels)

;; (s/exercise-fn `get-black-pixels 1) 

;; (stest/summarize-results (stest/check `get-black-pixels)) 

(defn get-pixel
  [picture x y]
  (aget (:pixels picture) (xy2idx picture x y)))

(defn get-pixel-hex
  [picture x y]
  (format "%08x" (get-pixel picture x y)))

(defn show-word!
  "Visualise a steno word."
  [word & {:keys [standard? zoom title] :or {standard? true zoom 5 title "Steno Word"}}]
  (let [nword (wd/normalize-word word)
        [w h] (if standard? wd/word-dims (transform [ALL] inc (wd/stats-word max nword)))
        image (mik/new-image w h)
        pixels (mik/get-pixels image)
        picture (map->Picture {:width w
                               :height h
                               :image image
                               :pixels pixels})]
    (doseq [[x y] nword]
      (aset pixels (xy2idx picture x y) BLACK))
    (show-picture! picture :zoom zoom :title title)))

(s/fdef show-word!
        :args (s/cat :word :steno.word/word :standard? (s/? boolean?)))

