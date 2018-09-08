(ns steno.word-test
  (:require [steno.word :as w]
            [steno.test.utils :refer [defspec-test]]))


(defspec-test get-neighbor-test `w/get-neighbor)
(defspec-test get-one-neighbors-test `w/get-one-neighbors)
(defspec-test get-neighbors-test `w/get-neighbors)
(defspec-test get-word-test `w/get-word)
(defspec-test get-words-test `w/get-words)
;; (defspec-test test-stats-word `w/stats-word)
(defspec-test test-normalize-word `w/normalize-word)
