(ns spectre.core-test
  (:require
   [borkdude.deflet :as d]
   [clojure.test :refer [deftest is testing]]
   [spectre.core :as spectre]))

(deftest pass-derive
  (testing "a passoword derivation"
    (is (= "HuqoBoquSeyn1'"
           (spectre/password "John Doe"
                             "hunter2"
                             "example.com"
                             {:variant :password :template :long})))))

(deftest different-site
  (testing "a different site yields a different password"
    (is (not= (spectre/password "John Doe" "hunter2" "example.com"
                                {:variant :password :template :long})
              (spectre/password "John Doe" "hunter2" "example.org"
                                {:variant :password :template :long})))))

(deftest different-variant
  (testing "a different variant yields a different password"
    (is (not= (spectre/password "John Doe" "hunter2" "example.com"
                                {:variant :password :template :long})
              (spectre/password "John Doe" "hunter2" "example.com"
                                {:variant :login :template :long})))
    (is (not= (spectre/password "John Doe" "hunter2" "example.com"
                                {:variant :password :template :long})
              (spectre/password "John Doe" "hunter2" "example.com"
                                {:variant :answer :template :long})))))

(deftest different-template
  (testing "the :maximum template gives a 20 char password"
    (is (= 20 (count (spectre/password "John Doe" "hunter2" "example.com"
                                       {:variant :password :template :maximum})))))
  (testing "the :pin template gives a 4 digit password"
    (d/deflet
      (def pw (spectre/password "John Doe" "hunter2" "example.com"
                                {:variant :password :template :pin}))
      (is (= 4 (count pw)))
      (is (re-matches #"\d{4}" pw))))
  (testing "the :short template gives a 4 char password starting Capital-lower-case-digit"
    (d/deflet
      (def pw (spectre/password "John Doe" "hunter2" "example.com"
                                {:variant :password :template :short}))
      (is (= 4 (count pw))))))

(deftest different-counter
  (testing "bumping the counter yields a different password"
    (is (not= (spectre/password "John Doe" "hunter2" "example.com"
                                {:variant :password :template :long :counter 1})
              (spectre/password "John Doe" "hunter2" "example.com"
                                {:variant :password :template :long :counter 2})))))

(deftest deterministic
  (testing "the same arguments always give the same password"
    (is (= (spectre/password "John Doe" "hunter2" "example.com"
                             {:variant :password :template :long})
           (spectre/password "John Doe" "hunter2" "example.com"
                             {:variant :password :template :long})))))
