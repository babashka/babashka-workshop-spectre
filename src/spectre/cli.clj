(ns spectre.cli
  (:require
   [babashka.cli :as cli]
   [spectre.clipboard :as clipboard]
   [spectre.core :as spectre]
   [spectre.db :as db]
   [spectre.identicon :as identicon]
   [spectre.term :as term]))

(def defaults {:counter 1 :template :long :variant :password})

(defn- known-sites
  "Sites in db.edn."
  [_]
  (sort (keys (:sites (db/load-db)))))

(def spec
  {:site {:desc "Site to derive a password for"
          :require true
          :positional true
          :complete-fn known-sites}
   :print {:desc "Print to stdout instead of copying to the clipboard" :coerce :boolean :alias :p}
   :name {:desc "Your full name (or SPECTRE_NAME)" :alias :u :complete false}
   :counter {:desc "Site counter" :coerce :long :alias :c :complete false}
   :template {:desc "Password template" :coerce :keyword :alias :t
              :enum (vec (keys spectre/templates))}
   :variant {:desc "What to derive" :coerce :keyword :alias :v
             :enum (vec (keys spectre/scope))}})

(defn- warn [& xs]
  (binding [*out* *err*]
    (apply println xs)))

(defn- site-opts
  "Site settings: the db entry, or app defaults for a new site. Flags
   override. When the effective settings differ from the db, warn and save."
  ([site explicit] (site-opts site explicit {}))
  ([site explicit db-opts]
   (let [dbv (db/load-db db-opts)
         stored (db/site-settings dbv site)
         effective (merge defaults stored explicit)]
     (if stored
       (doseq [[k v] effective
               :when (and (contains? stored k) (not= v (get stored k)))]
         (warn (str "db.edn has " k " " (get stored k) " for " site
                    ", using and saving " v)))
       (warn (str "Saving " site " to db.edn: " effective)))
     (when (not= effective stored)
       (db/merge-site! dbv site effective db-opts))
     effective)))

(defn generate
  "Derive a site password.

  Settings are stored per site in db.edn. Flags override them and are
  saved back. New sites start from {:counter 1 :template :long
  :variant :password}."
  {:org.babashka/cli {:spec spec
                      :args->opts [:site]
                      :restrict true
                      :restrict-args true}}
  [{:keys [site] :as opts}]
  (let [explicit (select-keys opts [:counter :template :variant])
        effective (site-opts site explicit)
        full-name (or (:name opts) (System/getenv "SPECTRE_NAME") (term/input "Full name: "))
        ;; the same name and master password always draw the same figure, so a
        ;; typo in the master password is visible while you type it
        figure #(identicon/identicon-of full-name %)
        master (or (System/getenv "SPECTRE_MASTER") (term/password "Master password: " figure))
        _ (warn "Identicon:" (figure master))
        password (spectre/derive (spectre/master-key full-name master (:variant effective))
                                 site
                                 effective)]
    (if (or (:print opts) (not (term/tty?)))
      (println password)
      (if (clipboard/copy! password)
        (warn "Copied to clipboard.")
        (do (warn "No clipboard tool found, printing instead.")
            (println password))))))

(defn -main
  "Entry point for standalone use. `bb pw` goes through :exec-fn instead."
  [& args]
  (let [{:keys [doc] :as m} (meta #'generate)]
    (cli/dispatch (assoc (:org.babashka/cli m)
                         :exec-fn generate
                         :doc doc)
                  args
                  {:prog "pw" :help true})))
