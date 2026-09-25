(ns spectre.tui2-test
  (:require
   [borkdude.deflet :as d]
   [charm.components.list :as item-list]
   [charm.message :as msg]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [spectre.identicon :as identicon]
   [spectre.tui2 :as tui2]))

(def ^:private db
  {:sites {"google.com" {:counter 1 :template :maximum :variant :password}
           "mail.google.com" {:counter 2 :template :long :variant :login}
           "example.org" {:counter 1 :template :long :variant :password}}})

(defn- press [state k]
  (first (tui2/update-fn state (msg/key-press k))))

(defn- titles [state]
  (mapv :site (item-list/items (:list state))))

(defn- init-state []
  (first (tui2/update-fn (tui2/state db) (msg/window-size 80 24))))

;; TODO: passes once spectre.tui2/matches filters and ranks the sites
(deftest search-test
  (testing "the window size sets the list height"
    (is (= 20 (:height (:list (init-state))))))
  (testing "arrow keys move the list cursor"
    (is (= 1 (item-list/selected-index (:list (press (init-state) :down)))))
    (is (= 0 (item-list/selected-index (:list (press (init-state) :up))))))
  (testing "letters bound by the list go to the search field, not the cursor"
    ;; the list binds g to go-to-start: here it has to end up in the query
    (d/deflet
      (def s (-> (init-state) (press :down) (press "g")))
      (is (= ["google.com" "mail.google.com" "example.org"] (titles s)))
      (is (= 1 (item-list/selected-index (:list s))))))
  ;; TODO:
  (testing "typing filters the list, backspace widens it again"
    (d/deflet
      (def s (reduce press (init-state) ["g" "o"]))
     ;; TODO: update spectre.tui2/matches to filter and rank the sites
     ;; TODO: add a test assertion for tui2/matches
      (is (= ["google.com" "mail.google.com"] (titles s)))
      (is (= 3 (count (titles (press s :backspace))))))))

;; TODO: passes once spectre.tui2/open-selected is fixed
;; Needs spectre.db/site-settings from E3 as well, for the stored settings
(deftest edit-test
  (d/deflet
    (def editing (-> (init-state) (press "g") (press :enter)))
    (testing "enter opens the selected site with its stored settings"
      (is (= :edit (:mode editing)))
      (is (= "google.com" (:site editing)))
      (is (= {:counter 1 :template :maximum :variant :password} (:draft editing))))
    (testing "left and right change the selected field"
      (is (= 2 (:counter (:draft (press editing :right)))))
      (is (= :long (:template (:draft (-> editing (press :down) (press :right)))))))
    (testing "the counter does not go below 1"
      (is (= 1 (:counter (:draft (-> editing (press :left) (press :left)))))))
    (testing "values wrap around"
      (is (= :answer (:variant (:draft (-> editing (press :down) (press :down) (press :left)))))))
    (testing "escape goes back to the search screen"
      (is (= :search (:mode (-> editing (press :right) (press :escape))))))))

(def ^:private env {"SPECTRE_NAME" "John Doe" "SPECTRE_MASTER" "hunter2"})

(defn- type-in [state text]
  (reduce press state (map str text)))

;; TODO: passes once spectre.tui2/cycle-value steps through the values
(deftest cycle-value-test
  (d/deflet
    (def values [:a :b :c])
    (testing "the next and the previous value"
      (is (= :b (tui2/cycle-value values :a :next)))
      (is (= :a (tui2/cycle-value values :b :prev))))
    (testing "both directions wrap around"
      (is (= :a (tui2/cycle-value values :c :next)))
      (is (= :c (tui2/cycle-value values :a :prev))))
    (testing "a value that is not one of them starts at the first"
      (is (= :a (tui2/cycle-value values :x :next)))
      (is (= :a (tui2/cycle-value values nil :prev))))))

(deftest identity-test
  (testing "tab opens the identity screen, esc and enter go back"
    (is (= :identity (:mode (press (init-state) :tab))))
    (is (= :search (:mode (-> (init-state) (press :tab) (press :escape)))))
    (is (= :search (:mode (-> (init-state) (press :tab) (press :enter))))))
  (testing "name and master password come prefilled from the environment"
    (is (str/includes? (tui2/view (press (tui2/state db env) :tab)) "John Doe")))
  (testing "the master password is never shown"
    (is (not (str/includes? (tui2/view (press (tui2/state db env) :tab)) "hunter2")))))

;; TODO, optional: passes once spectre.tui2/figure gives the identicon for the
;; name and master password on the identity screen
(deftest ^:optional figure-test
  (d/deflet
    (def s (-> (tui2/state db {}) (press :tab) (type-in "JohnDoe") (press :down) (type-in "hunter2")))
    (def expected (identicon/identicon-of "JohnDoe" "hunter2"))
    (testing "the figure follows what is typed on the identity screen"
      (is (str/includes? (tui2/view s) expected)))
    (testing "and the search screen shows the same one"
      (is (str/includes? (tui2/view (press s :escape)) expected)))
    (testing "nothing while a field is still empty"
      (is (not (str/includes? (tui2/view (-> (tui2/state db {}) (press :tab) (type-in "JohnDoe"))) "╰"))))))

(comment
  (clojure.test/run-tests 'spectre.tui2-test))
