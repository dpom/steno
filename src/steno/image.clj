(ns steno.image
  "Functions related to image manipulation."
  (:import (java.awt.image BufferedImage)
           (boofcv.io.image UtilImageIO ConvertBufferedImage)
           (boofcv.struct.image GrayF32 GrayU8)
           (boofcv.gui.binary VisualizeBinaryData)
           (boofcv.alg.filter.binary BinaryImageOps GThresholdImageOps ThresholdImageOps)
           (boofcv.gui.image ShowImages)))

(defn load-image
  "Load the an image from a file."
  [filename]
  (-> filename
      UtilImageIO/loadImage
      (ConvertBufferedImage/convertFromSingle nil GrayU8)))

(defn show-image
  "Visualise an image."
  ([image] (show-image image "Image"))
  ([image title] (-> image
                     (ConvertBufferedImage/convertTo nil)
                     (ShowImages/showWindow title))))

(defn filter-image!
  "Apply a function to all image pixels.

  Args:
    image (ImageBase): an image
    func (function): a function with 3 args image, x, y

  Returns: nil, it modifies the image"
  [image func]
  (doseq [x (range (.width image))]
    (doseq [y (range (.height image))]
      (func image x y))))

(defn clean-image!
  "Clean an binary image."
  [image]
  (let [threshold (GThresholdImageOps/computeOtsu image 0.0 255.0)]
    (filter-image! image (fn [img x y] (.set img x y (if (< (.get img x y) threshold) 0 255))))))

(defn check-pixel
  [img acc [x y]]
  (if (zero? (.get img x y))
    (conj acc [x y])
    acc))

(defn get-black-pixels
  [img]
  (for [x (range (.width img)) y (range (.height img)) :when (zero? (.get img x y))] [x y]))
