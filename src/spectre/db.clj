(ns spectre.db
  "Per-site settings in db.edn."
  (:require
   [babashka.fs :as fs]
   [clojure.edn :as edn]
   [clojure.pprint :as pp]))

(def path "db.edn")

(defn load-db
  []) ;; TODO

(defn save-db!
  [db]) ;; TODO

(defn site-settings
  [db site]) ;; TODO

(defn merge-site!
  "Merge settings into the site entry and save. Returns the updated db."
  [db site settings]) ;; TODO
