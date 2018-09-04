
;; 2018-08-28

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

;; 2018-08-29


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

;; 2018-09-01


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

;; 2018-09-03


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
             :rpoint ::point))



   ;; :fn (s/or :nil #(nil? (:ret %))
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



 

