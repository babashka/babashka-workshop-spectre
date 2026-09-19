(ns spectre.seed
  (:require [spectre.db :as db]))

(def sites
  {"google.com" {:counter 1 :template :maximum :variant :password}
   "mail.google.com" {:counter 2 :template :long :variant :login}
   "example.org" {:counter 1 :template :pin :variant :password}})

(defn seed!
  "Adds missing example sites and returns the database. Leaves existing entries unchanged."
  [opts]
  (let [current (db/load-db opts)
        seeded (update current :sites #(merge sites %))]
    (when (not= current seeded)
      (db/save-db! seeded opts))
    seeded))
