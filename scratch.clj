
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
