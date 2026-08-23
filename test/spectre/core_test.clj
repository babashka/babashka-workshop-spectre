(ns spectre.core-test
  (:refer-clojure :exclude [derive])
  (:require
   [clojure.test :refer [deftest is testing]]
   [spectre.core :as spectre]))

(deftest derive
  (testing "a passoword derivation"
    (is (= "HuqoBoquSeyn1'"
           (spectre/password "John Doe"
                             "hunter2"
                             "example.com"
                             {:variant :password :template :long})))))

;; TODO: More tests
