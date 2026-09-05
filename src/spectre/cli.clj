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
  [_]) ;; TODO

(def spec
  {:site {:desc "Site to derive a password for"
          :require true
          :positional true
          ;; TODO: offer the known sites for completion
          }
   :print {:desc "Print to stdout instead of copying to the clipboard" :coerce :boolean :alias :p}
   ;; TODO: :name, :counter, :template and :variant
   })

(defn- warn [& xs]
  (binding [*out* *err*]
    (apply println xs)))

(defn- site-opts
  "Site settings: the db entry, or app defaults for a new site. Flags
   override. When the effective settings differ from the db, warn and save."
  ([site explicit] (site-opts site explicit {}))
  ([site explicit db-opts])) ;; TODO

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
        master (or (System/getenv "SPECTRE_MASTER") (term/password "Master password: "))
        ;; the same name and master password always draw the same figure, so a
        ;; typo in the master password is visible before you use the result
        _ (warn "Identicon:" (identicon/identicon-of full-name master))
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
