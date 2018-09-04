(ns steno.word-test
  (:require [steno.word :as w]
            [clojure.spec.alpha      :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.string          :as str]
            [clojure.test            :as test]
            [expound.alpha           :as expound]))

(defn report-results [check-results]
  (let [checks-passed? (->> check-results (map :failure) (every? nil?))]
    (if checks-passed?
      (test/do-report {:type    :pass
                       :message (str "Generative tests pass for "
                                     (str/join ", " (map :sym check-results)))})
      (doseq [failed-check (filter :failure check-results)]
        (let [r       (stest/abbrev-result failed-check)
              failure (:failure r)]
          (test/do-report
           {:type     :fail
            :message  (binding [s/*explain-out* (expound/custom-printer {:theme :figwheel-theme})]
                        (expound/explain-results-str check-results))
            :expected (->> r :spec rest (apply hash-map) :ret)
            :actual   (if (instance? Throwable failure)
                        failure
                        (::stest/val failure))}))))
    checks-passed?))

(defmacro defspec-test
     ([name sym-or-syms] `(defspec-test ~name ~sym-or-syms nil))
     ([name sym-or-syms opts]
      (when test/*load-tests*
        `(defn ~(vary-meta name assoc :test
                           `(fn [] (report-results (stest/check ~sym-or-syms ~opts))))
           [] (test/test-var (var ~name))))))




(defspec-test get-neighbor-test `w/get-neighbor)
(defspec-test get-one-neighbors-test `w/get-one-neighbors)
(defspec-test get-neighbors-test `w/get-neighbors)
(defspec-test get-word-test `w/get-word)
(defspec-test get-words-test `w/get-words)
