(ns spectre.tui-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [spectre.tui :as tui]))

(def ^:private db
  {:sites {"google.com" {:counter 1 :template :maximum :variant :password}
           "mail.google.com" {:counter 2 :template :long :variant :login}
           "example.org" {:counter 1 :template :long :variant :password}}})

(defn- state [& kvs]
  (#'tui/refresh (merge {:db db :query "" :cursor 0 :mode :search} (apply hash-map kvs))))

(deftest matches-test
  (let [sites ["example.org" "google.com" "mail.google.com"]
        matches #'tui/matches]
    (is (= sites (matches sites "")))
    (is (= ["google.com" "mail.google.com"] (matches sites "google")))
    (testing "prefix matches come first"
      (is (= ["google.com" "mail.google.com"] (matches sites "goo"))))
    (testing "case insensitive"
      (is (= ["example.org"] (matches sites "EXAMPLE"))))
    (is (= [] (matches sites "nope")))))

(deftest decode-test
  (let [decode #'tui/decode
        seq-peek (fn [& codes] (let [left (atom codes)]
                                 (fn [] (or (first (first (swap-vals! left rest))) -2))))]
    (is (= \a (decode (int \a) (seq-peek))))
    (is (= :enter (decode 13 (seq-peek))))
    (is (= :backspace (decode 127 (seq-peek))))
    (is (= :eof (decode -1 (seq-peek))))
    (testing "escape sequences"
      (is (= :up (decode 27 (seq-peek (int \[) 65))))
      (is (= :down (decode 27 (seq-peek (int \[) 66))))
      (is (= :left (decode 27 (seq-peek (int \[) 68)))))
    (testing "a lone escape is not an arrow key"
      (is (= :escape (decode 27 (seq-peek)))))))

(deftest search-test
  (let [k #'tui/search-key]
    (testing "typing filters, backspace widens again"
      (let [s (-> (state) (k \g) (k \o))]
        (is (= "go" (:query s)))
        (is (= ["google.com" "mail.google.com"] (:hits s)))
        (is (= 3 (count (:hits (k s :backspace)))))))
    (testing "the cursor stays inside the hit list"
      (is (= 2 (:cursor (-> (state) (k :down) (k :down) (k :down)))))
      (is (= 0 (:cursor (-> (state) (k :up)))))
      (is (= 1 (:cursor (-> (state :cursor 2) (k \g) (k \o) (k \o))))))
    (testing "enter opens the selected site with its stored settings"
      (let [s (-> (state) (k \m) (k :enter))]
        (is (= :edit (:mode s)))
        (is (= "mail.google.com" (:site s)))
        (is (= {:counter 2 :template :long :variant :login} (:draft s)))))
    (testing "enter on an empty hit list does nothing"
      (is (= :search (:mode (-> (state) (k \z) (k :enter))))))
    (is (:done (k (state) :escape)))))

(deftest edit-test
  (let [k #'tui/edit-key
        editing (-> (state) (#'tui/search-key \g) (#'tui/search-key :enter))]
    (testing "left and right change the selected field"
      (is (= 2 (:counter (:draft (k editing :right)))))
      (is (= :long (:template (:draft (-> editing (k :down) (k :right)))))))
    (testing "the counter does not go below 1"
      (is (= 1 (:counter (:draft (-> editing (k :left) (k :left)))))))
    (testing "values wrap around"
      (is (= :answer (:variant (:draft (-> editing (k :down) (k :down) (k :left)))))))
    (testing "escape goes back without touching the db"
      (is (= :search (:mode (-> editing (k :right) (k :escape))))))))
