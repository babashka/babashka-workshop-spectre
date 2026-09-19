(ns spectre.db-test
  (:require
   [babashka.fs :as fs]
   [clojure.test :refer [deftest is testing]]
   [spectre.db :as db]))

(def ^:private settings {:counter 1 :template :long :variant :password})

;; TODO: passes once spectre.db/load-db reads the EDN at :path, and checks fs/exists? first
(deftest load-db-test
  (fs/with-temp-dir [dir {}]
    (let [path (str (fs/path dir "db.edn"))]
      (testing "a db file that is not there yet has no sites"
        (is (empty? (:sites (db/load-db {:path path})))))
      (testing "load-db reads the EDN in the file"
        (spit path (pr-str {:sites {"example.com" settings}}))
        (is (= {:sites {"example.com" settings}} (db/load-db {:path path})))))))

;; TODO: passes once spectre.db/save-db! creates the parent directory and writes EDN
(deftest save-db-test
  (testing "save-db! creates the parent directory and load-db reads the db back"
    (fs/with-temp-dir [dir {}]
      (let [path (str (fs/path dir "spectre" "db.edn"))
            value {:sites {"example.com" settings}}]
        (db/save-db! value {:path path})
        (is (= value (db/load-db {:path path})))))))

;; TODO: passes once spectre.db/site-settings looks one site up in the db
(deftest site-settings-test
  (let [value {:sites {"example.com" settings}}]
    (is (= settings (db/site-settings value "example.com")))
    (testing "nil for a site that is not in the db"
      (is (nil? (db/site-settings value "example.org"))))))

;; TODO: passes once spectre.db/merge-site! merges the settings in and saves
(deftest merge-site-test
  (fs/with-temp-dir [dir {}]
    (let [path (str (fs/path dir "db.edn"))
          value (db/merge-site! {} "example.com" settings {:path path})]
      (testing "merge-site! returns the db with the new site in it"
        (is (= {:sites {"example.com" settings}} value)))
      (testing "a partial update keeps the other keys"
        (let [value (db/merge-site! value "example.com" {:counter 2} {:path path})]
          (is (= (assoc settings :counter 2) (db/site-settings value "example.com")))
          (testing "and lands on disk"
            (is (= {:sites {"example.com" (assoc settings :counter 2)}}
                   (db/load-db {:path path}))))))
      (testing "one site does not touch another"
        (let [value (db/merge-site! value "example.org" {:counter 5} {:path path})]
          (is (= settings (db/site-settings value "example.com")))
          (is (= {:counter 5} (db/site-settings value "example.org"))))))))

(comment
  (clojure.test/run-tests 'spectre.db-test))
