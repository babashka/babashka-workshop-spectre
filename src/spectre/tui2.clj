(ns spectre.tui2
  "Browse and edit site settings in a terminal UI."
  (:require
   [charm.components.help :as help]
   [charm.components.list :as item-list]
   [charm.components.text-input :as text-input]
   [charm.message :as msg]
   [charm.program :as program]
   [charm.style.core :as style]
   [clojure.string :as str]
   [spectre.core :as spectre]
   [spectre.db :as db]
   [spectre.identicon :as identicon]))

(def ^:private defaults {:counter 1 :template :long :variant :password})

(def ^:private fields
  [{:key :counter :label "counter"}
   {:key :template :label "template" :values (vec (keys spectre/templates))}
   {:key :variant :label "variant" :values (vec (keys spectre/scope))}])

(def ^:private label-style (style/style :fg 240))
(def ^:private title-style (style/style :fg style/magenta :bold true))
(def ^:private cursor-style (style/style :fg style/cyan :bold true))

(def ^:private search-help
  (help/from-pairs
   "type" "filter"
   "up/down" "move"
   "enter" "edit"
   "tab" "identity"
   "esc" "quit"))

(def ^:private identity-help
  (help/from-pairs
   "↑/↓" "field"
   "esc" "back"))

(def ^:private edit-help
  (help/from-pairs
   "up/down" "field"
   "left/right" "change"
   "enter" "save"
   "esc" "back"))

(def ^:private chrome-height 4)

(defn- list-height [term-height]
  (max 3 (- term-height chrome-height)))

;;;; sites

(defn- settings-summary [{:keys [counter template variant]}]
  (format "%-3s %-8s %s" counter (name template) (name variant)))

(defn- entry
  "A site as a charm list item. :title is what the list renders."
  [db site]
  {:site site
   :title (format "%-30s %s" site (settings-summary (merge defaults (db/site-settings db site))))})

(defn- matches
  "Sites containing `query`, case insensitively. Prefix matches come first."
  [sites query]
  sites) ;; TODO

(defn- entries [{:keys [db sites]} query]
  (mapv #(entry db %) (matches sites query)))

(defn- refresh
  "Feed the current query's hits to the list component."
  [state]
  (update state :list item-list/set-items (entries state (text-input/value (:input state)))))

;;;; init

(defn state
  "The initial state for a db value. `env` is a map like (System/getenv)."
  ([db] (state db (System/getenv)))
  ([db env]
   (let [sites (vec (sort (keys (:sites db))))
         state {:db db
                :sites sites
                ;; the identity screen, prefilled from the environment when set
                :name-input (text-input/text-input :prompt "Name:   "
                                                   :placeholder "your full name"
                                                   :value (or (get env "SPECTRE_NAME") ""))
                :master-input (text-input/text-input :prompt "Master: "
                                                     :placeholder "your master password"
                                                     :echo-mode :password
                                                     :value (or (get env "SPECTRE_MASTER") "")
                                                     :focused false)
                :identity-field 0
                :mode :search
                :term-height 24
                :input (text-input/text-input :prompt "Search: "
                                              :placeholder "type to filter")
                :list (item-list/item-list []
                                           :height (list-height 24)
                                           :cursor-style cursor-style)
                :search-help (help/help search-help :width 60)
                :identity-help (help/help identity-help :width 60)
                :edit-help (help/help edit-help :width 60)}]
     (refresh state))))

(defn- figure
  "The identicon for the name and master password on the identity screen, nil
   while either is empty."
  [state]
  nil) ;; TODO

(defn init []
  [(state (db/load-db)) nil])

;;;; update

(defn- navigation? [m]
  (some #(msg/key-match? m %) [:up :down :page-up :page-down]))

(defn- open-selected
  "Open the selected site and load its settings into the draft."
  [state]
  state) ;; TODO

(defn- update-search [state m]
  (cond
    (msg/key-match? m :escape)
    [state program/quit-cmd]

    (msg/key-match? m :enter)
    [(open-selected state) nil]

    (msg/key-match? m :tab)
    [(assoc state :mode :identity) nil]

    ;; only the arrow keys go to the list: its j/k/g bindings would swallow
    ;; the letters we want to search with
    (navigation? m)
    (let [[lst cmd] (item-list/list-update (:list state) m)]
      [(assoc state :list lst) cmd])

    :else
    (let [[input cmd] (text-input/text-input-update (:input state) m)]
      [(refresh (assoc state :input input)) cmd])))

(defn- cycle-value
  "The next or previous value for a field, wrapping around."
  [values v dir]
  v) ;; TODO

(defn- adjust
  "Step a field of the draft. A field with :values cycles through them, the
   counter counts, and never below 1."
  [draft {:keys [key values]} dir]
  draft) ;; TODO

(defn- update-edit [{:keys [site draft field] :as state} m]
  (cond
    (msg/key-match? m :escape) [(assoc state :mode :search) nil]
    (msg/key-match? m :up) [(assoc state :field (max 0 (dec field))) nil]
    (msg/key-match? m :down) [(assoc state :field (min (dec (count fields)) (inc field))) nil]
    (msg/key-match? m :left) [(assoc state :draft (adjust draft (fields field) :prev)) nil]
    (msg/key-match? m :right) [(assoc state :draft (adjust draft (fields field) :next)) nil]
    (msg/key-match? m :enter) [(-> state
                                   (assoc :db (db/merge-site! (:db state) site draft)
                                          :mode :search)
                                   (refresh))
                               nil]
    :else [state nil]))

(defn- identity-inputs [{:keys [identity-field]}]
  (if (zero? identity-field) [:name-input :master-input] [:master-input :name-input]))

(defn- update-identity [state m]
  (cond
    (or (msg/key-match? m :escape) (msg/key-match? m :enter))
    [(assoc state :mode :search) nil]

    (or (msg/key-match? m :up) (msg/key-match? m :down))
    (let [state (assoc state :identity-field (if (msg/key-match? m :up) 0 1))
          [active other] (identity-inputs state)]
      [(-> state (update active text-input/focus) (update other text-input/blur)) nil])

    :else
    (let [[active] (identity-inputs state)
          [input cmd] (text-input/text-input-update (get state active) m)]
      [(assoc state active input) cmd])))

(defn update-fn [state m]
  (cond
    (msg/key-match? m "ctrl+c")
    [state program/quit-cmd]

    (msg/window-size? m)
    [(-> state
         (assoc :term-height (:height m))
         (update :list item-list/set-height (list-height (:height m))))
     nil]

    (= :edit (:mode state)) (update-edit state m)
    (= :identity (:mode state)) (update-identity state m)
    :else (update-search state m)))

;;;; view

(defn- search-view [state]
  (let [n (item-list/item-count (:list state))]
    (str (text-input/text-input-view (:input state)) "\n"
         (style/render label-style
                       (cond
                         (pos? n) (format "%d/%d sites" (inc (item-list/selected-index (:list state))) n)
                         (seq (:sites state)) "no match"
                         :else "no sites in db.edn yet"))
         (when-let [figure (figure state)]
           (str "   " (style/render title-style figure)))
         "\n\n"
         (item-list/list-view (:list state)) "\n"
         (help/short-help-view (:search-help state)))))

(defn- edit-view [{:keys [site draft field] :as state}]
  (str (style/render title-style site) "\n\n"
       (str/join "\n"
                 (map-indexed
                  (fn [i {:keys [key label]}]
                    (let [v (get draft key)
                          line (format " %-10s %s" label (if (keyword? v) (name v) v))]
                      (if (= i field) (style/render cursor-style line) line)))
                  fields))
       "\n\n"
       (help/short-help-view (:edit-help state))))

(defn- identity-view [state]
  (str (style/render title-style "Who are you?") "\n\n"
       (text-input/text-input-view (:name-input state)) "\n"
       (text-input/text-input-view (:master-input state)) "\n\n"
       (if-let [figure (figure state)]
         (style/render title-style figure)
         (style/render label-style "the figure appears once both are filled in"))
       "\n\n"
       (help/short-help-view (:identity-help state))))

(defn view [state]
  (case (:mode state)
    :edit (edit-view state)
    :identity (identity-view state)
    (search-view state)))

;;;; entry point

(def ^:private browse-spec
  {:nrepl {:desc "Also serve an nREPL, to change the TUI while it runs" :coerce :boolean}
   :port {:desc "Port for --nrepl" :coerce :long :default 1667 :alias :p}})

(defn browse
  "Browse and edit the sites in db.edn.

  With --nrepl, redefining `view` or `update-fn` from your editor takes effect
  on the next keystroke. Evaluate, then press a key to see it. Do not print to
  *out* from there: the TUI owns the screen."
  {:org.babashka/cli {:spec browse-spec :restrict true}}
  [{:keys [nrepl port]}]
  (when nrepl
    ;; resolved at call time, so this namespace still loads on a JVM REPL,
    ;; where babashka.nrepl.server does not exist
    ((requiring-resolve 'babashka.nrepl.server/start-server!)
     {:host "127.0.0.1" :port port}))
  (program/run {;; init runs once, so there is nothing to reload there
                :init init
                ;; the vars, not their values: charm calls these on every
                ;; message, so redefining one lands on the next keystroke
                :update #'update-fn
                :view #'view
                :alt-screen true}))
