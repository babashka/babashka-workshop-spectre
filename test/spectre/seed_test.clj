(ns spectre.seed-test
  (:require
   [babashka.fs :as fs]
   [clojure.test :refer [deftest is testing]]
   [spectre.db :as db]
   [spectre.seed :as seed]))

(deftest seed-test
  (fs/with-temp-dir [dir {}]
    (let [opts {:path (str (fs/path dir "db.edn"))}]
      (testing "creates a database with example sites"
        (seed/seed! opts)
        (is (= {:sites seed/sites} (db/load-db opts))))
      (testing "preserves existing sites and other fields"
        (let [existing {:sites {"google.com" {:counter 9}
                                "personal.example" {:counter 4 :template :short}}
                        :custom "keep"}]
          (db/save-db! existing opts)
          (seed/seed! opts)
          (is (= (update existing :sites #(merge seed/sites %))
                 (db/load-db opts)))))
      (testing "skips writing unchanged data"
        (let [before (slurp (:path opts))]
          (with-redefs [db/save-db! (fn [& _] (throw (ex-info "Unexpected write" {})))]
            (seed/seed! opts))
          (is (= before (slurp (:path opts)))))))))
