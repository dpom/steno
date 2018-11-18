(ns steno.handler.example-test
  (:require [clojure.test :refer :all]
            [integrant.core :as ig]
            [ring.mock.request :as mock]
            [steno.handler.example :as example]))

(deftest smoke-test
  (testing "example page exists"
    (let [handler  (ig/init-key :steno.handler/example {})
          response (handler (mock/request :get "/example"))]
      (is (= :ataraxy.response/ok (first response)) "response ok"))))
