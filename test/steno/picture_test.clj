(ns steno.picture-test
  (:require [steno.picture :as p]
            [steno.test.utils :refer [defspec-test]]))



(defspec-test test-xy2idx `p/xy2idx)
(defspec-test test-idx2xy `p/idx2xy)

(defspec-test test-get-black-pixels `get-black-pixels)
