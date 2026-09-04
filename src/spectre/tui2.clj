(ns spectre.tui2
  "The same site browser as `spectre.tui`, built on charm.clj.

  Deliberately a second, self-contained implementation: `spectre.tui` drives
  JLine directly (read a key, redraw the screen), this one is the Elm
  architecture - state, update, view - with charm's text-input and list
  components doing the editing and scrolling."
  (:require
   [charm.components.help :as help]
   [charm.components.list :as item-list]
   [charm.components.text-input :as text-input]
   [charm.message :as msg]
   [charm.program :as program]
   [charm.style.core :as style]
   [clojure.string :as str]
   [spectre.core :as spectre]
   [spectre.db :as db]))

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
   "↑/↓" "move"
   "enter" "edit"
   "esc" "quit"))

(def ^:private edit-help
  (help/from-pairs
   "↑/↓" "field"
   "←/→" "change"
   "enter" "save"
   "esc" "back"))

;; Search prompt, hit count, blank line, help line
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
  "The initial state for a db value."
  [db]
  (let [sites (vec (sort (keys (:sites db))))
        state {:db db
               :sites sites
               :mode :search
               :term-height 24
               :input (text-input/text-input :prompt "Search: "
                                             :placeholder "type to filter")
               :list (item-list/item-list []
                                          :height (list-height 24)
                                          :cursor-style cursor-style)
               :search-help (help/help search-help :width 60)
               :edit-help (help/help edit-help :width 60)}]
    (refresh state)))

(defn init []
  [(state (db/load-db)) nil])

;;;; update

(defn- navigation? [m]
  (some #(msg/key-match? m %) [:up :down :page-up :page-down]))

(defn- open-selected
  "Open the selected site for editing, its stored settings as the draft."
  [state]
  state) ;; TODO

(defn- update-search [state m]
  (cond
    (msg/key-match? m :escape)
    [state program/quit-cmd]

    (msg/key-match? m :enter)
    [(open-selected state) nil]

    ;; only the arrow keys go to the list: its j/k/g bindings would swallow
    ;; the letters we want to search with
    (navigation? m)
    (let [[lst cmd] (item-list/list-update (:list state) m)]
      [(assoc state :list lst) cmd])

    :else
    (let [[input cmd] (text-input/text-input-update (:input state) m)]
      [(refresh (assoc state :input input)) cmd])))

(defn- cycle-value
  "Next or previous value for a field, wrapping around."
  [values v step]
  v) ;; TODO

(defn- adjust
  "Step a field of the draft. A field with :values cycles through them, the
   counter counts, and never below 1."
  [draft {:keys [key values]} step]
  draft) ;; TODO

(defn- update-edit [{:keys [site draft field] :as state} m]
  (cond
    (msg/key-match? m :escape) [(assoc state :mode :search) nil]
    (msg/key-match? m :up) [(assoc state :field (max 0 (dec field))) nil]
    (msg/key-match? m :down) [(assoc state :field (min (dec (count fields)) (inc field))) nil]
    (msg/key-match? m :left) [(assoc state :draft (adjust draft (fields field) -1)) nil]
    (msg/key-match? m :right) [(assoc state :draft (adjust draft (fields field) 1)) nil]
    (msg/key-match? m :enter) [(-> state
                                   (assoc :db (db/merge-site! (:db state) site draft)
                                          :mode :search)
                                   (refresh))
                               nil]
    :else [state nil]))

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

(defn view [state]
  (if (= :edit (:mode state))
    (edit-view state)
    (search-view state)))

;;;; entry point

(defn browse
  "Browse and edit the sites in db.edn."
  [& _]
  (program/run {:init init
                :update update-fn
                :view view
                :alt-screen true}))
