(ns spectre.cli-test
  (:require
   [babashka.cli :as cli]
   [babashka.fs :as fs]
   [clojure.test :refer [deftest is testing]]
   [spectre.cli :as spectre-cli]
   [spectre.db :as db]))

(defn- parse [args]
  (cli/parse-opts args (:org.babashka/cli (meta #'spectre-cli/generate))))

(deftest spec-test
  (testing "the site is positional"
    (is (= {:site "example.com"} (parse ["example.com"]))))
  (testing "every flag has an alias and coerces to the right type"
    (is (= {:site "example.com" :name "John Doe" :counter 3
            :template :maximum :variant :login :print true}
           (parse ["example.com" "-u" "John Doe" "-c" "3" "-t" "maximum" "-v" "login" "-p"]))))
  (testing "a template outside the set is rejected"
    (is (thrown? Exception (parse ["example.com" "-t" "nope"])))))

(def ^:private site-opts #'spectre.cli/site-opts)

(deftest site-opts-test
  (fs/with-temp-dir [dir {}]
    (let [opts {:path (str (fs/file dir "db.edn"))}
          stored #(db/site-settings (db/load-db opts) "example.com")]
      (binding [*err* (java.io.StringWriter.)]
        (testing "a new site starts from the defaults, and they are saved"
          (is (= spectre-cli/defaults (site-opts "example.com" {} opts)))
          (is (= spectre-cli/defaults (stored))))
        (testing "a flag overrides the stored setting and is saved"
          (is (= :maximum (:template (site-opts "example.com" {:template :maximum} opts))))
          (is (= :maximum (:template (stored)))))
        (testing "without a flag the stored setting stands"
          (is (= :maximum (:template (site-opts "example.com" {} opts)))))))))
