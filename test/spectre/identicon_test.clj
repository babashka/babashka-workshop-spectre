(ns spectre.identicon-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [spectre.identicon :as identicon]))

(deftest identicon-of-test
  (testing "one name and master password always draw the same figure"
    (is (= "╰▒╝◓" (identicon/identicon-of "John Doe" "hunter2"))))
  (testing "another master password draws another figure"
    (is (not= (identicon/identicon-of "John Doe" "hunter2")
              (identicon/identicon-of "John Doe" "hunter3"))))
  (testing "another name draws another figure"
    (is (not= (identicon/identicon-of "John Doe" "hunter2")
              (identicon/identicon-of "Jane Roe" "hunter2"))))
  (testing "the figure is four characters"
    (is (= 4 (count (identicon/identicon-of "John Doe" "hunter2"))))))
