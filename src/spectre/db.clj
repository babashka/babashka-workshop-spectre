(ns spectre.db
  "Per-site settings in db.edn."
  (:require
   [babashka.fs :as fs]
   [clojure.edn :as edn]
   [clojure.pprint :as pp]))

(def default-path
  "The SPECTRE_DB environment variable, or ~/.config/spectre/db.edn."
  (or (System/getenv "SPECTRE_DB")
      (str (fs/path (fs/xdg-config-home "spectre") "db.edn"))))

(defn load-db
  ([] (load-db {}))
  ([{:keys [path] :or {path default-path}}])) ;; TODO

(defn save-db!
  ([db] (save-db! db {}))
  ([db {:keys [path] :or {path default-path}}])) ;; TODO

(defn site-settings
  [db site]) ;; TODO

(defn merge-site!
  "Merge settings into the site entry and save. Returns the updated db."
  ([db site settings] (merge-site! db site settings {}))
  ([db site settings opts])) ;; TODO
