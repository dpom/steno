
;; * 2018-08-28

(import (java.awt.image BufferedImage)
        (boofcv.io.image UtilImageIO ConvertBufferedImage)
        (boofcv.struct.image GrayF32 GrayU8)
        (boofcv.gui.binary VisualizeBinaryData)
        (boofcv.alg.filter.binary BinaryImageOps GThresholdImageOps ThresholdImageOps)
        (boofcv.gui.image ShowImages))

(def image (UtilImageIO/loadImage "test/ex1.jpg"))

(def binary (ConvertBufferedImage/convertFromSingle image nil GrayU8))

(def bin-data (VisualizeBinaryData/renderBinary binary true nil))

(ShowImages/showWindow image "Image")

(ShowImages/showWindow binary "Image")

(ShowImages/showWindow bin-data "Image")

;; * 2018-08-29


(ShowImages/showWindow (ConvertBufferedImage/convertTo binary nil) "Image")

(defn load-image
  [filename]
  (-> filename
      UtilImageIO/loadImage
      (ConvertBufferedImage/convertFromSingle nil GrayU8)))

(defn show-image
  [image]
  (-> image
      (ConvertBufferedImage/convertTo nil)
      (ShowImages/showWindow "Image")))

(show-image (load-image "test/ex1.jpg"))

(def page (load-image "test/ex1.jpg"))

(ShowImages/showWindow (VisualizeBinaryData/renderBinary page false nil) "Image")

(def page-width (.width page))

(def page-height (.height page))

(.get page 5 23)

(show-image page)

(def i1 (GrayU8. 100 100))

(show-image i1)

(.set i1 50 50 255)

(defn get-row
  [image row]
  (mapv #(.get image row %) (range (.width image))))

(filterv #(< % 255) (get-row page 400))

(defn filter-image
  [image func]
  (doseq [x (range (.width image))]
    (doseq [y (range (.height image))]
      (func image x y))))

(filter-image page (fn [img x y] (.set img x y (if (< (.get img x y) 200) 0 255))))

(show-image page)

(def page (load-image "test/ex1.jpg"))

(defn load-image32
  [filename]
  (-> filename
      UtilImageIO/loadImage
      (ConvertBufferedImage/convertFromSingle nil GrayF32)))

(def page (load-image32 "test/ex1.jpg"))
(def page8 (load-image "test/ex1.jpg"))

(show-image page)

(def threshold (GThresholdImageOps/computeOtsu page 0.0 255.0))

(def binary (GrayU8. (.width page) (.height page)))

(def bin2 (ThresholdImageOps/threshold page binary (float threshold) true))

(show-image bin2)

(filter-image page (fn [img x y] (.set img x y (if (< (.get img x y) 151) 0 255))))

(require '[steno.image :as img])

(def page (img/load-image "test/ex1.jpg"))

(img/show-image page)

(img/clean-image! page)

(img/show-image page "page2")

(def page3 (GrayU8. (.width page) (.height page)))

(def page4 (BinaryImageOps/erode8 page 1 page3))

(img/show-image page3 "page3")

(img/show-image page4 "page4")

(VisualizeBinaryData/renderBinary page false nil)

;; * 2018-09-01


(require '[steno.image :as img]
         '[clojure.set :as st])

(def page (img/load-image "test/ex1.jpg"))

(img/show-image page)

(defn check-pixel
  [img acc [x y]]
  (if (zero? (.get img x y))
    (conj acc [x y])
    acc))

(defn get-black-pixels
  [img]
  (for [x (range (.width img)) y (range (.height img)) :when (zero? (.get img x y))] [x y]))

(def blaks (get-black-pixels page))

(count blaks)

(img/clean-image! page)

(def blaks2 (get-black-pixels page))

(declare get-neighbor
         get-neighbors)

(def directions [[0 1] [-1 1] [1 0] [1 1]])

(defn get-neighbors
  [blacks word pixel]
  (let [gn (partial get-neighbor blacks pixel)]
    (reduce merge word (map gn directions))))

(defn get-neighbor
  [blacks [x y] word [dx dy]]
  (let [pixel [(+ x dx) (+ y dy)]]
    (if-not (contains? blacks pixel)
      word
      (recur (disj blacks pixel) (conj word pixel))
      #{pixel}
      #{})))

(count blaks2)

(first #{:a :b :c})

(rest #{:a :b :c})

(defn get-word
  [blacks pixel]
  (get-neighbors blacks #{pixel}  pixel))

(defn get-words
  [blacks words]
  (if (empty? blacks)
    words
    (let [pixel (first blacks)
          word (get-word blacks pixel)]
      (recur (st/difference blacks word) (conj words word)))))

;; * 2018-09-03


(require '[steno.image :as img]
         '[clojure.set :as st]
         '[clojure.spec.alpha :as s]
         '[clojure.spec.gen.alpha :as gen]
         '[clojure.spec.test.alpha :as stest])

(def page (img/load-image "test/ex1.jpg"))

(img/show-image page)

(s/def ::x (s/and nat-int? #(< % 5000)))

(s/def ::y (s/and nat-int? #(< % 5000)))

(s/def ::point (s/tuple ::x ::y))

(s/def ::points (s/coll-of ::point :distinct true :into (sorted-set)))

(def blacks (into (sorted-set) (img/get-black-pixels page)))

(s/valid? ::points blacks)

(count blacks)
(.width page)
(.height page)

(def directions [[0 1] [-1 1] [1 0] [1 1]])

(s/def ::direction (set directions))

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
                   :rpoint ::point));; :fn (s/or :nil #(nil? (:ret %))
            ;; :point #(contains? (-> % :args :blacks) (:ret %))))


(gen/generate (s/gen ::points))

(stest/instrument `get-neighbor)

(stest/check `get-neighbor)

(def point (first blacks))
(def neib (second blacks))

(get-neighbor blacks point (first directions))

(get-neighbor #{[0 1] [0 2]} [0 1] [0 1])

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

(stest/check `get-one-neighbors)

(get-one-neighbors blacks point)

(defn get-neighbors
  [blacks points]
  (let [gon (partial get-one-neighbors blacks)]
    (reduce into #{} (map gon points))))

(s/fdef get-neighbors
        :args (s/and (s/cat :blacks ::points
                            :points (s/coll-of ::point))
                     #(pos-int? (count (:blacks %))))
        :ret ::points)

(stest/instrument `get-neighbors)

(s/exercise-fn `get-neighbors 1)

(stest/summarize-results (stest/check `get-neighbors))

(get-neighbors blacks [point])

(defspec-test test-get-neighbors [`get-neighbors])

(defn get-word
  [blacks neighbors]
  (loop [bs blacks
         word neighbors
         nebs neighbors]
    (if (empty? nebs)
      word
      (let [w (st/union word nebs)
            b (st/difference bs w)]
        (recur b w (get-neighbors b nebs))))))

(s/fdef get-word
        :args (s/and (s/cat :blacks ::points
                            :neibrs ::points)
                     #(pos-int? (count (:blacks %)))
                     #(st/subset? (:neibrs %) (:blacks %)))
        :ret ::points)

(stest/instrument `get-word)

(s/exercise-fn `get-word 1)

(stest/summarize-results (stest/check `get-word))

(defspec-test test-get-word [`get-word])

(get-word blacks #{point})

(defn get-words
  [blacks words]
  (if (empty? blacks)
    words
    (let [point (first blacks)
          word (get-word blacks #{point})]
      (recur (st/difference blacks word) (conj words word)))))

(s/fdef get-words
        :args (s/cat :blacks ::points
                     :words  (s/coll-of ::points :min-count 0))
        :ret (s/coll-of ::points :min-count 0))

(stest/instrument `get-words)

(s/exercise-fn `get-words 1)

(stest/summarize-results (stest/check `get-word))

(defspec-test test-get-words [`get-word])

(def words (get-words blacks #{}))

;; * 2018-09-04


(require '[steno.image :as img]
         '[steno.word :as wd]
         '[com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]
         '[clojure.java.io :as io]
         '[clojure.set :as st]
         '[clojure.edn :as edn]
         '[clojure.spec.alpha :as s]
         '[clojure.spec.gen.alpha :as gen]
         '[clojure.spec.test.alpha :as stest])

(def page (img/load-image "test/ex1.jpg"))

(img/show-image page)

(def blacks (into (sorted-set) (img/get-black-pixels page)))

(def words (wd/get-words blacks []))

(defn write-dataset-edn! [out-file raw-dataset-map]
  (with-open [w (clojure.java.io/writer out-file)]
    (binding [*out* w]
      (clojure.pprint/write raw-dataset-map))))

(write-dataset-edn! "~/pers/steno/tmp/words.edn" words)

(defn get-words-as-file!
  [blacks filename]
  (set! *print-length* -1)
  (spit filename "" :append false)
  (loop [bks blacks]
    (if (empty? bks)
      filename
      (let [point (first bks)
            word (wd/get-word bks #{point})]
        (spit filename (prn-str word) :append true)
        (recur (st/difference bks word))))))

(get-words-as-file! blacks "/home/dan/pers/steno/tmp/words.edn")

(def word #{[1676 1222] [1674 1221] [1678 1222] [1677 1222] [1675 1221]})

(defn mx-min
  [word]
  (reduce (fn [[x1 y1] [x2 y2]] [(min x1 x2) (min y1 y2)]) word))

(def w2
  #{[1496 883] [1506 896] [1507 897] [1495 887] [1496 884] [1505 895] [1502 893] [1498 885] [1510 898] [1497 889] [1492 876] [1493 878] [1495 881] [1495 886] [1510 899] [1493 877] [1500 891] [1495 883] [1503 895] [1499 886] [1494 879] [1509 898] [1492 875] [1495 885] [1507 896] [1499 891] [1500 887] [1496 886] [1503 894] [1504 894] [1498 889] [1503 892] [1502 890] [1504 891] [1497 885] [1498 887] [1498 890] [1498 888] [1503 890] [1495 882] [1501 891] [1508 897] [1497 887] [1508 898] [1495 884] [1501 888] [1501 889] [1497 884] [1495 880]})

(def min-w2 (mx-min w2))
(defn normalize-word
  [word]
  (let [[min-x min-y] (mx-min word)]
    (transform [ALL] (fn [[x y]] [(- x min-x) (- y min-y)]) word)))

(def nw2 (normalize-word w2))

(defn apply-word
  [word func]
  (reduce (fn [[x1 y1] [x2 y2]] [(func x1 x2) (func y1 y2)]) word))

(apply-word nw2 max)

(defn transform-words!
  [in-file out-file func]
  (with-open [rdr (io/reader in-file)]
    (with-open [w (io/writer out-file)]
      (doseq [line (line-seq rdr)]
        (if-let [word (edn/read-string line)]
          (.write w (prn-str (func word))))))))

;; * 2018-09-05

(require '[steno.image :as img]
         '[steno.word :as wd]
         '[com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]
         '[clojure.java.io :as io]
         '[clojure.set :as st]
         '[clojure.edn :as edn]
         '[clojure.spec.alpha :as s]
         '[clojure.spec.gen.alpha :as gen]
         '[clojure.spec.test.alpha :as stest])

(def in-file "/home/dan/pers/steno/tmp/words.edn")
(def out-file "/home/dan/pers/steno/tmp/normalized_words.edn")

(wd/transform-words! in-file out-file wd/normalize-word)

(def out-file2 "/home/dan/pers/steno/tmp/max_words.edn")

(wd/transform-words! out-file out-file2 (partial wd/stats-word max))

(def word-max
  (with-open [rdr (io/reader out-file2)]
    (wd/stats-word max (set (mapv edn/read-string (line-seq rdr))))))

[23 24]

;; * 2018-09-06

(require '[steno.image :as img]
         '[steno.word :as wd]
         '[com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]
         '[clojure.java.io :as io]
         '[clojure.set :as st]
         '[clojure.edn :as edn]
         '[clojure.spec.alpha :as s]
         '[clojure.spec.gen.alpha :as gen]
         '[clojure.spec.test.alpha :as stest])

(def w1 #{[2 2] [0 0] [3 3] [3 4] [2 4] [1 2] [0 1]})

(img/show-word w1)

;; * 2018-09-07

(require
 '[steno.word :as wd]
 '[mikera.image.core :as mik]
 '[mikera.image.filters :as filt]
 '[com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]
 '[clojure.java.io :as io]
 '[clojure.set :as st]
 '[clojure.edn :as edn]
 '[clojure.spec.alpha :as s]
 '[clojure.spec.gen.alpha :as gen]
 '[clojure.spec.test.alpha :as stest])

(def page (mik/load-image "/home/dan/pers/steno/test/ex1.jpg"))

(mik/show page :zoom 0.5 :title "Orig Image")

(def pixels (mik/get-pixels page))

(count pixels)

(mik/width page)

(mik/height page)

(defrecord Page [image width height pixels])

(defn load-page
  [filename]
  (let [image (mik/load-image filename)]
    (map->Page {:image (mik/filter-image image (filt/quantize 2))
                :width (mik/width image)
                :height (mik/height image)
                :pixels (mik/get-pixels image)})))

(def filename "/home/dan/pers/steno/test/ex1.jpg")

(def page (load-page filename))

(mik/show (mik/filter-image (:image page) (filt/invert)))

(mik/show (mik/filter-image (:image page) (filt/quantize 2)))

(defn show-page
  [{:keys [image pixels]} & opts]
  (mik/set-pixels image pixels)
  (apply mik/show image opts))

(show-page page :zoom 0.5 :title "cucu")

(defn xy2idx
  [page x y]
  (+ x (* y (:width page))))

(xy2idx page 0 3)

(defn get-pixel
  [page x y]
  (aget (:pixels page) (xy2idx page x y)))

(get-pixel page 0 0)

(defn get-pixel-hex
  [page x y]
  (format "%08x" (get-pixel page x y)))

(get-pixel-hex page 48 216)

(get-pixel-hex page 48 214)

(get-pixel-hex page 0 0)

(get-pixel-hex page 49 200)

-16777216

(take 10 (map-indexed (fn [idx val] [idx val]) (:pixels page)))

(defn idx2xy
  [{:keys [width]} idx]
  [(mod idx width) (quot idx width)])

(defn get-black-pixels
  [page]
  (let  [xform (comp
                (map-indexed (fn [idx v] [idx v]))
                (filter (fn [[idx v]] (= v -16777216)))
                (map first)
                (map (fn [idx] (idx2xy page idx))))]
    (into (sorted-set) xform (:pixels page))))

(def blacks2 (get-black-pixels page))

(class (:pixels page))
(type (:pixels page))

(instance? (Class/forName "[I") (:pixels page))
(class (:image page))

java.awt.image.BufferedImage

(count (:pixels page))

;; * 2018-09-15

(require
 '[steno.word :as wd]
 '[steno.picture :as pic]
 '[mikera.image.core :as mik]
 '[mikera.image.filters :as filt]
 '[com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]
 '[clojure.java.io :as io]
 '[clojure.set :as st]
 '[clojure.edn :as edn]
 '[clojure.spec.alpha :as s]
 '[clojure.spec.gen.alpha :as gen]
 '[clojure.spec.test.alpha :as stest])

;; (s/def ::wid (s/ge))
;; (s/def ::pic (s/with-gen (s/keys :req-un [::width])))

(def filename "/home/dan/pers/steno/test/ex1.jpg")
(def words-file "/home/dan/pers/steno/tmp/words2.edn")
(def nwords-file "/home/dan/pers/steno/tmp/normalized_words2.edn")

(def page (pic/load-picture filename))

(pic/show-picture! page :zoom 0.25 :title "page")

(def blacks (pic/get-black-pixels page))

(wd/get-words-as-file! blacks words-file)

(wd/transform-words! words-file nwords-file wd/normalize-word)

(def w2
  #{[4 3] [2 2] [0 0] [2 8] [1 0] [3 3] [0 5] [3 4] [6 5] [4 6] [1 4] [1 3] [5 5] [2 7] [3 6] [4 5] [2 0] [0 4] [2 1] [4 4] [3 7]})

(pic/show-word! w2  :zoom 10)

;; * 2018-09-16

(require
 '[steno.word :as wd]
 '[steno.picture :as pic]
 '[mikera.image.core :as mik]
 '[mikera.image.filters :as filt]
 '[com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]
 '[clojure.java.io :as io]
 '[clojure.set :as st]
 '[clojure.edn :as edn]
 '[clojure.spec.alpha :as s]
 '[clojure.spec.gen.alpha :as gen]
 '[clojure.spec.test.alpha :as stest])
 
(def filename "/home/dan/pers/steno/test/ex2.jpg")
(def words-file "/home/dan/pers/steno/tmp/words3.edn")
(def words-file2 "/home/dan/pers/steno/tmp/words4.edn")
(def nwords-file "/home/dan/pers/steno/tmp/normalized_words3.edn")


(def page (pic/load-picture filename))

(pic/show-picture! page :zoom 0.25 :title "page")

(def blacks (pic/get-black-pixels page))

(count blacks)

(wd/get-words-as-file! blacks words-file)

(wd/transform-words! words-file words-file2 #(apply sorted-set %))


(wd/transform-words! words-file nwords-file wd/normalize-word)

;; * 2018-09-29

(require
 '[steno.word :as wd]
 '[steno.picture :as pic]
 '[mikera.image.core :as mik]
 '[mikera.image.filters :as filt]
 '[com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]
 '[clojure.java.io :as io]
 '[clojure.set :as st]
 '[clojure.edn :as edn]
 '[clojure.spec.alpha :as s]
 '[clojure.spec.gen.alpha :as gen]
 '[clojure.spec.test.alpha :as stest])

(def filename "/home/dan/pers/steno/test/ex2.jpg")
(def words-file "/home/dan/pers/steno/tmp/words3.edn")
(def words-file2 "/home/dan/pers/steno/tmp/words4.edn")
(def nwords-file "/home/dan/pers/steno/tmp/normalized_words3.edn")


(def page (pic/load-picture filename))

(pic/show-picture! page :zoom 0.25 :title "page")

(def blacks (pic/get-black-pixels page))

(count blacks)

(def pixel (first blacks))

(def pixels #{(first blacks)})

(def next-pixels (set (mapcat #(wd/get-one-neighbors blacks %) pixels)))

(def next-pixels (set (mapcat #(wd/get-one-neighbors blacks %) next-pixels)))


(defn get-word
  [blacks]
  (loop [pixels #{(first blacks)}
         word #{(first blacks)}]
    ;; (printf "pixels: %s, word: %s\n" pixels word)
    (if (empty? pixels)
      word
      (let [next-pixels (set (mapcat #(wd/get-one-neighbors blacks %) pixels))]
        (recur (st/difference next-pixels word) (st/union word next-pixels))))))

(def word (wd/get-word blacks))

(def nword (wd/normalize-word word))

(pic/show-word! nword :title "word1")


(def word2 (get-word (st/difference blacks word)))

(def nword2 (wd/normalize-word word2))

(pic/show-word! nword2 :title "word2")


#{[132 220] [132 222] [133 225] [134 229] [133 222] [133 224] [133 226] [132 221] [135 229] [133 227] [134 228] [133 223]}

#{[135 241] [136 242] [134 241] [135 242]}

(pic/get-pixel-hex page 134 229)

(pic/get-pixel-hex page 134 230)
(pic/get-pixel-hex page 134 231)

(pic/get-pixel-hex page 132 110)



;; * 2018-10-01

(require
 '[steno.word :as wd]
 '[steno.picture :as pic]
 '[mikera.image.core :as mik]
 '[mikera.image.filters :as filt]
 '[com.hypirion.clj-xchart :as xc]
 '[com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]
 '[clojure.java.io :as io]
 '[clojure.set :as st]
 '[clojure.edn :as edn]
 '[clojure.spec.alpha :as s]
 '[clojure.spec.gen.alpha :as gen]
 '[clojure.spec.test.alpha :as stest])

(def filename "/home/dan/pers/steno/test/ex2.jpg")
(def words-file "/home/dan/pers/steno/tmp/words3.edn")
(def words-file2 "/home/dan/pers/steno/tmp/words4.edn")
(def nwords-file "/home/dan/pers/steno/tmp/normalized_words3.edn")


(def page (pic/load-picture filename))

(pic/show-picture! page :zoom 0.25 :title "page")

(def pixels (:pixels page))

(def res (->> pixels
              (map #(bit-and % 0xff))
              frequencies
              (into (sorted-map))))

(xc/extract-series
 {:x first
  :y second
  :style {:marker-type :none}}
 res)


(xc/view
 (xc/xy-chart
  {"Pixel histogram"
   (xc/extract-series
     {:x first
      :y second}
     res)}
  {:title "Pixel histogram"
   :render-style :area}))

(def pixels-count (count pixels))

(doseq [idx (range pixels-count)]
  (let [pix (bit-and 0xff (aget pixels idx))]
    (aset pixels idx (if (< pix 200) pic/BLACK pic/WHITE))))

(take 10 pixels

;; * 2018-10-01

 (require
  '[steno.word :as wd]
  '[steno.picture :as pic]
  '[mikera.image.core :as mik]
  '[mikera.image.filters :as filt]
  '[com.hypirion.clj-xchart :as xc]
  '[com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]
  '[clojure.java.io :as io]
  '[clojure.set :as st]
  '[clojure.edn :as edn]
  '[clojure.spec.alpha :as s]
  '[clojure.spec.gen.alpha :as gen]
  '[clojure.spec.test.alpha :as stest]))

(def filename "/home/dan/pers/steno/test/ex2.jpg")
(def words-file "/home/dan/pers/steno/tmp/words3.edn")
(def words-file2 "/home/dan/pers/steno/tmp/words4.edn")
(def nwords-file "/home/dan/pers/steno/tmp/normalized_words3.edn")


(def page (pic/load-picture filename))

(pic/show-picture! page :zoom 0.25 :title "page")


;; * 2018-10-02

(require
 '[steno.word :as wd]
 '[steno.picture :as pic]
 '[mikera.image.core :as mik]
 '[mikera.image.filters :as filt]
 '[com.hypirion.clj-xchart :as xc]
 '[com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]
 '[clojure.java.io :as io]
 '[clojure.set :as st]
 '[clojure.edn :as edn]
 '[clojure.spec.alpha :as s]
 '[clojure.spec.gen.alpha :as gen]
 '[clojure.spec.test.alpha :as stest])

(def filename "/home/dan/pers/steno/test/ex2.jpg")
(def words-file "/home/dan/pers/steno/tmp/words3.edn")
(def nwords-file "/home/dan/pers/steno/tmp/normalized_words3.edn")


(def page (pic/load-picture filename))

(pic/show-picture! page :zoom 0.25 :title "page")

(def blacks (pic/get-black-pixels page))

(count blacks)

(def word (wd/get-word blacks))

(def nword (wd/normalize-word word))

(pic/show-word! nword :standard? false :title "word")

(def dims (wd/stats-word max nword))


(pic/show-word! nword  :title "word")

(wd/get-words-as-file! blacks words-file)

(wd/transform-words! words-file words-file2 #(apply sorted-set %))


(wd/transform-words! words-file nwords-file wd/normalize-word)

(defn second-< [x y]
  (let [c (compare (second x) (second y))]
    (if (zero? c)
      (compare x y)
      c)))
      
(def blk (apply sorted-set-by second-< blacks))

(def yblk (apply sorted-set (mapv second blk)))

(apply max yblk)

(def mis (apply sorted-set (st/difference (set (range 2423)) yblk)))

(def steps
  (reduce (fn [{:keys [last res]} elm]
              {:last elm :res (if (> (- elm last) 1) (conj res last) res)})
          {:last 0 :res []}
          mis))
  
(wd/get-words-as-file! blk words-file)

(def word #{[2131 145] [2135 141] [2135 71] [2135 152] [2133 129] [2132 62] [2132 140] [2134 148] [2132 146] [2135 153] [2133 114] [2135 112] [2132 142] [2133 118] [2134 147] [2132 143] [2135 60] [2133 135] [2134 149] [2135 107] [2135 78] [2132 148] [2133 133] [2133 138] [2135 68] [2134 145] [2134 140] [2131 141] [2133 142] [2135 128] [2135 64] [2133 127] [2135 135] [2134 97] [2132 63] [2133 67] [2134 59] [2133 58] [2134 130] [2135 148] [2132 127] [2131 144] [2132 124] [2135 59] [2133 63] [2133 144] [2135 82] [2135 151] [2134 137] [2133 139] [2134 96] [2135 113] [2135 125] [2131 146] [2132 67] [2135 143] [2134 61] [2135 87] [2133 145] [2134 102] [2134 125] [2134 93] [2133 130] [2134 95] [2132 130] [2134 152] [2134 143] [2135 130] [2131 62] [2134 104] [2135 62] [2135 89] [2132 64] [2132 144] [2134 71] [2135 104] [2135 88] [2134 101] [2132 122] [2135 124] [2134 99] [2131 127] [2134 81] [2134 119] [2134 136] [2134 115] [2133 68] [2135 129] [2135 147] [2133 116] [2133 111] [2134 150] [2132 138] [2134 69] [2131 138] [2135 83] [2135 150] [2131 139] [2135 138] [2132 65] [2133 66] [2131 131] [2135 70] [2134 129] [2135 90] [2134 72] [2134 75] [2134 139] [2135 127] [2134 153] [2132 150] [2134 146] [2133 64] [2132 141] [2135 106] [2135 120] [2135 149] [2134 122] [2135 136] [2134 127] [2135 154] [2135 142] [2133 123] [2134 124] [2134 135] [2135 86] [2133 147] [2132 59] [2134 116] [2134 67] [2133 113] [2134 87] [2133 136] [2132 121] [2135 103] [2131 61] [2135 123] [2132 139] [2134 68] [2134 64] [2135 102] [2135 133] [2132 120] [2132 128] [2133 119] [2134 112] [2134 113] [2135 84] [2135 121] [2134 109] [2131 129] [2133 112] [2133 121] [2135 146] [2135 109] [2132 149] [2135 66] [2135 110] [2133 153] [2133 124] [2133 131] [2132 125] [2134 86] [2135 118] [2135 92] [2135 73] [2134 131] [2134 58] [2135 80] [2134 133] [2132 137] [2134 111] [2134 90] [2132 123] [2135 100] [2134 73] [2132 136] [2131 130] [2132 126] [2134 120] [2133 125] [2134 123] [2135 94] [2134 117] [2133 62] [2134 144] [2135 108] [2134 142] [2131 128] [2135 117] [2133 137] [2133 65] [2132 131] [2134 121] [2134 108] [2134 141] [2135 69] [2133 132] [2133 115] [2134 128] [2134 154] [2134 83] [2132 119] [2132 135] [2133 120] [2134 88] [2135 63] [2132 60] [2134 103] [2134 80] [2131 135] [2134 62] [2133 141] [2134 105] [2132 66] [2132 61] [2133 60] [2131 136] [2134 94] [2132 147] [2133 143] [2135 140] [2135 119] [2135 79] [2133 152] [2134 107] [2131 137] [2133 148] [2135 132] [2135 137] [2133 150] [2133 128] [2134 138] [2135 144] [2135 96] [2132 134] [2135 122] [2135 75] [2134 60] [2133 117] [2135 72] [2132 145] [2135 77] [2134 76] [2135 115] [2134 63] [2134 57] [2135 97] [2135 134] [2134 114] [2135 139] [2134 89] [2132 118] [2135 76] [2135 131] [2135 155] [2134 106] [2134 132] [2135 101] [2133 126] [2135 105] [2133 146] [2135 85] [2132 129] [2132 133] [2135 74] [2135 145] [2135 114] [2134 85] [2135 116] [2134 98] [2133 134] [2135 65] [2134 79] [2131 134] [2134 118] [2134 91] [2131 126] [2135 57] [2135 126] [2135 67] [2134 84] [2134 151] [2131 133] [2135 81] [2131 140] [2135 111] [2134 66] [2135 99] [2135 58] [2133 59] [2133 151] [2134 70] [2131 125] [2135 61] [2133 149] [2135 93] [2134 65] [2134 82] [2135 98] [2131 124] [2131 63] [2131 142] [2135 91] [2134 92] [2133 140] [2134 110] [2134 126] [2131 132] [2134 134] [2135 95] [2132 132] [2131 143] [2133 122] [2133 61]})

(pic/show-word! word)


;; * 2018-10-14

(require
 '[steno.word :as wd]
 '[steno.picture :as pic]
 '[mikera.image.core :as mik]
 '[mikera.image.filters :as filt]
 '[com.hypirion.clj-xchart :as xc]
 '[com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]
 '[clojure.java.io :as io]
 '[clojure.set :as st]
 '[clojure.edn :as edn]
 '[clojure.spec.alpha :as s]
 '[clojure.spec.gen.alpha :as gen]
 '[clojure.spec.test.alpha :as stest])

(def filename "/home/dan/pers/steno/test/ex2.jpg")

(def page (pic/load-picture filename))

(pic/show-picture! page :zoom 0.25 :title "page")

(def blacks (pic/get-black-pixels page))

(count blacks)


(defn get-gaps
  [blacks]
  (let [yblk (set (mapv second blacks))
        ymax (apply max yblk)
        mis (apply sorted-set (st/difference (set (range ymax)) yblk))
        gaps (:res (reduce (fn [{:keys [last res]} elm]
                               {:last elm :res (if (> (- elm last) 1) (conj res last) res)})
                           {:last 0 :res []}
                           mis))]
      (if (zero? (first gaps)) (rest gaps) gaps)))

  
(get-gaps blacks)

(defn split-lines
  [blacks]
  (:lines (reduce (fn [{:keys [blk lines]} gap]
                      (let [line (set (filter #(< (second %) gap) blk))]
                           {:blk (st/difference blk line) :lines (conj lines line)}))
                  {:blk blacks :lines []}
                  (get-gaps blacks))))

(def lines (split-lines blacks))

(mapv count lines)

(class (st/difference (sorted-set 1 2 3 4 5 6 7) (set [2 4 5])))

(class (sorted-set 1 2 3))

(class (st/difference (set [1 2 3 4 5 6 7]) (set [2 4 5])))


(reduce into [[#{[1 2] [1 3]} #{[1 4] [1 5]}] [#{[2 2] [2 3]} #{[2 4] [2 5]}] [#{[3 2] [3 3]} #{[3 4] [3 5]}]])

(defn get-words-as-file!
  [blacks filename]
  (with-bindings {#'*print-length* -1}
    (spit filename "" :append false)
    (doseq [words (pmap wd/get-words (split-lines blacks))]
        (doseq [word words]
           (spit filename (prn-str word) :append true)))))

(def words-file1 "/home/dan/pers/steno/tmp/words4.edn")


(def words-file2 "/home/dan/pers/steno/tmp/words5.edn")


(def t1 (future (time (get-words-as-file! blacks words-file1))))

(def t2 (future (time (wd/get-words-as-file! blacks words-file2))))


(def t [@t1 @t2])

@t1


(def blacks2 (nth lines 2))

(def words2 (wd/get-words-as-file! blacks2 words-file1))

(def word
    #{[2135 616] [2135 617] [2135 619] [2135 622] [2135 618] [2135 621] [2135 614] [2135 613] [2135 615] [2135 620] [2135 623]})


(pic/show-word! word :zoom 0.25 :title "word" :standard? false)

;; * 2018-11-03

(require
 '[steno.word :as wd]
 '[steno.picture :as pic]
 '[mikera.image.core :as mik]
 '[mikera.image.filters :as filt]
 '[com.hypirion.clj-xchart :as xc]
 '[com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]
 '[clojure.java.io :as io]
 '[clojure.set :as st]
 '[clojure.edn :as edn]
 '[clojure.spec.alpha :as s]
 '[clojure.spec.gen.alpha :as gen]
 '[clojure.spec.test.alpha :as stest])

(def filename "/home/dan/pers/steno/test/ex2.jpg")

(def page (pic/load-picture filename))

(pic/show-picture! page :zoom 0.25 :title "page")

(def blacks (pic/get-black-pixels page))

(count blacks)


(defn get-gaps
  [blacks line?]
  (let [func (if line? second first)
        yblk (set (mapv func blacks))
        ymax (apply max yblk)
        mis (apply sorted-set (st/difference (set (range ymax)) yblk))
        gaps (:res (reduce (fn [{:keys [last res]} elm]
                               {:last elm :res (if (> (- elm last) 1) (conj res last) res)})
                           {:last 0 :res []}
                           mis))]
    (cond 
      (nil? gaps) []
      (zero? (first gaps)) (rest gaps)
      :else gaps)))

  
(get-gaps blacks true)

(defn split-zones
  [blacks line?]
  (let [gaps (get-gaps blacks line?)
        func (if line? second first)]
    (if (seq gaps)
        (:zones (reduce (fn [{:keys [blk zones]} gap]
                            (let [zone (set (filter #(< (func %) gap) blk))]
                                 {:blk (st/difference blk zone) :zones (conj zones zone)}))
                        {:blk blacks :zones []}
                        gaps))
        blacks)))

(def lines (split-zones blacks true))


(mapv count lines)



(def cells (mapv #(split-zones % false) lines))

(seq ())

(apply max #{})
(apply max (mapv second #{[0 0] [0 1] [0 2] [0 3] [1 0] [1 3] [2 0] [2 1] [2 4] [3 1] [4 0]
                          [4 1] [5 1] [7 1]}))
(rest #{})

;; * 2018-11-04

(require
 '[steno.word :as wd]
 '[steno.picture :as pic]
 '[mikera.image.core :as mik]
 '[mikera.image.filters :as filt]
 '[clj-time.core :as t]
 '[com.hypirion.clj-xchart :as xc]
 '[com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]
 '[clojure.java.io :as io]
 '[clojure.set :as st]
 '[clojure.edn :as edn]
 '[clojure.spec.alpha :as s]
 '[clojure.spec.gen.alpha :as gen]
 '[clojure.spec.test.alpha :as stest])

(def filename "/home/dan/pers/steno/test/ex2.jpg")

(def page (pic/load-picture filename))

(pic/show-picture! page :zoom 0.25 :title "page")

(def blacks (pic/get-black-pixels page))

(count blacks)

(def cell (mapcat #(wd/split-zones % false) (wd/split-zones blacks true)))

(def zones (mapv #(wd/split-zones % false) (wd/split-zones blacks true)))

(conj nil 1)

(def cell (remove empty? (mapcat #(wd/split-zones % false) (wd/split-zones blacks true))))

(defn get-words-as-file!
  [blacks filename]
  (with-bindings {#'*print-length* -1}
    (spit filename "" :append false)
    (printf "time1: %s\n" (t/now))
    (let [page-words (pmap wd/get-words (remove empty? (mapcat #(wd/split-zones % false) (wd/split-zones blacks true))))]
         (printf "time2: %s\n" (t/now))
         (doseq [words page-words]
           (doseq [word words]
             (spit filename (prn-str word) :append true))))))

(def words-file1 "/home/dan/pers/steno/tmp/words4.edn")

(get-words-as-file! blacks words-file1)

;; * 2018-11-11

(require
 '[steno.word :as wd]
 '[steno.picture :as pic]
 '[mikera.image.core :as mik]
 '[mikera.image.filters :as filt]
 '[clj-time.core :as t]
 '[com.hypirion.clj-xchart :as xc]
 '[com.rpl.specter :refer [transform select selected? select-one submap must ALL FIRST MAP-VALS]]
 '[clojure.java.io :as io]
 '[clojure.set :as st]
 '[clojure.edn :as edn]
 '[clojure.spec.alpha :as s]
 '[clojure.spec.gen.alpha :as gen]
 '[clojure.spec.test.alpha :as stest])

(def words-file "/home/dan/pers/steno/tmp/words4.edn")
(def nwords-file "/home/dan/pers/steno/tmp/normalized_words4.edn")


(wd/transform-words! words-file words-file2 #(apply sorted-set %))


(wd/transform-words! words-file nwords-file wd/normalize-word)

(def max-file "/home/dan/pers/steno/tmp/max_words4.edn")

(wd/transform-words! nwords-file max-file (partial wd/stats-word max))

(defn read-words
  [filename]
  (with-open [rdr (io/reader filename)]
    (mapv edn/read-string (line-seq rdr))))

(def words (read-words nwords-file))

(count words)

(defn view-word
  [words idx]
  (pic/show-word! (get words idx)))


(def max-words (mapv #(wd/stats-word max %) words))

(apply max (map first max-words))
(apply max (map second max-words))


(view-word words 219)



