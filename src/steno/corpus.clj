(ns steno.corpus
  (:require
   [steno.extractor :as extr]
   [steno.imageprocessor :as img]
   [steno.converter :as conv]
   [steno.utils :as utl]
   [basilisp.io :as bio]
   [basilisp.core :refer [format]]))

(defn extract-lsign-from-wsing!
  [imagefile outdir row column wsign]
  (let [fname (utl/get-filename imagefile)
        ws (:wsign wsign)]
    (doseq [i (range (count ws))]
      (let [lsign (merge (nth ws i)
                         {:fileimage imagefile
                          :row row
                          :column column
                          :pos i})
            outname (format "%s-%02d-%02d-%01d.edn" fname row column i)
            outfile (bio/path outdir outname)]
        (utl/save-edn outfile lsign)))))

(defn walk-matrix!
  "Traverse a matrix and execute the function for each cell. The function has 3 arguments: row index, column index, and cell content."
  [matrix function!]
  (doseq [y (range (count matrix))]
    (let [row (nth matrix y)]
      (doseq [x (range (count row))]
        (let [cell (nth row x)]
          (function! y x cell))))))

(defn extract-lsign-from-image!
  "Extract all lsigns from a FILEIMAGE and save them as edn files in OUTPUTDIR."
  [fileimage outdir]
  (let [mtx (extr/extract-glyphs fileimage)
        convert-image (fn [row col glyph]
                        {:row row
                         :col col
                         :wsign (-> (:roi glyph)
                                    img/process-image
                                    conv/image-to-wsign)})
        wsigns (utl/map-glyph-matrix mtx convert-image)
        f! (fn [row column cell]
             (extract-lsign-from-wsing! fileimage outdir row column cell))]
    (walk-matrix! wsigns f!)))

(comment

  (def imagefile "test/resources/template.png")

  (extract-lsign-from-image! imagefile "tmp/corpus")

  ;;
  )
