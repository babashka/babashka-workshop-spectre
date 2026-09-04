(ns spectre.tui
  "Fullscreen site browser: type to search, enter to edit site settings.

  Built on the same JLine terminal as the prompts in `spectre.term`."
  (:require
   [clojure.string :as str]
   [spectre.core :as spectre]
   [spectre.db :as db]
   [spectre.term :as term])
  (:import
   [org.jline.terminal Attributes Size Terminal]
   [org.jline.utils AttributedString AttributedStyle Display InfoCmp$Capability NonBlockingReader]))

(set! *warn-on-reflection* true)

(def ^:private defaults {:counter 1 :template :long :variant :password})

(def ^:private templates (vec (keys spectre/templates)))
(def ^:private variants (vec (keys spectre/scope)))

(def ^:private fields
  [{:key :counter :label "counter"}
   {:key :template :label "template" :values templates}
   {:key :variant :label "variant" :values variants}])

;;;; keys

(defn- decode
  "Turn key code `c` into a keyword for special keys, a character otherwise.
  `peek` reads the next code with a short timeout (negative when nothing
  follows) and is used to decode the arrow key escape sequences."
  [c peek]
  (case (int c)
    -1 :eof
    3 :eof                              ; ctrl-c
    4 :eof                              ; ctrl-d
    (13 10) :enter
    (127 8) :backspace
    21 :clear                           ; ctrl-u
    14 :down                            ; ctrl-n
    16 :up                              ; ctrl-p
    27 (if (contains? #{(int \[) (int \O)} (peek))
         (case (int (peek))
           65 :up
           66 :down
           67 :right
           68 :left
           :unknown)
         :escape)
    (if (>= (int c) 32) (char c) :unknown)))

(defn- read-key [^NonBlockingReader in]
  (decode (.read in) #(.read in 50)))

;;;; search

(defn- matches
  "Sites containing `query`, case insensitively. Prefix matches come first."
  [sites query]
  (if (str/blank? query)
    sites
    (let [q (str/lower-case query)]
      (->> sites
           (keep (fn [site]
                   (when-let [i (str/index-of (str/lower-case site) q)]
                     [i site])))
           (sort)
           (mapv second)))))

(defn- clamp [x lo hi]
  (-> x (max lo) (min hi)))

;;;; rendering

(def ^:private plain AttributedStyle/DEFAULT)
(def ^:private dim (.faint AttributedStyle/DEFAULT))
(def ^:private selected (.inverse AttributedStyle/DEFAULT))

(defn- line
  ([s] (line s plain))
  ([s ^AttributedStyle style] (AttributedString. (str s) style)))

(defn- settings-summary [{:keys [counter template variant]}]
  (format "%-3s %-8s %s" counter (name template) (name variant)))

(defn- search-lines
  "The search screen: query, hit count, the visible slice of the list."
  [{:keys [query hits cursor db]} rows]
  (let [;; query line, blank, hits, footer
        room (max 1 (- rows 4))
        top (clamp (- cursor (quot room 2)) 0 (max 0 (- (count hits) room)))
        window (->> hits (drop top) (take room))]
    (concat
     [(line (str "Search: " query))
      (line (if (seq hits)
              (format "%d/%d sites" (inc cursor) (count hits))
              (if (seq (:sites db)) "no match" "no sites in db.edn yet"))
            dim)
      (line "")]
     (map-indexed
      (fn [i site]
        (let [idx (+ top i)
              settings (merge defaults (db/site-settings db site))]
          (line (format " %-30s %s" site (settings-summary settings))
                (if (= idx cursor) selected plain))))
      window)
     (repeat (- room (count window)) (line ""))
     [(line "↑/↓ move · enter edit · esc quit" dim)])))

(defn- edit-lines
  "The edit screen for the selected site."
  [{:keys [site draft field]} rows]
  (concat
   [(line site)
    (line "")]
   (map-indexed
    (fn [i {:keys [key label]}]
      (let [v (get draft key)]
        (line (format " %-10s %s" label (if (keyword? v) (name v) v))
              (if (= i field) selected plain))))
    fields)
   (repeat (max 0 (- rows (+ 4 (count fields)))) (line ""))
   [(line "")
    (line "↑/↓ field · ←/→ change · enter save · esc back" dim)]))

(defn- render! [^Terminal terminal ^Display display state]
  (let [size (.getSize terminal)
        ;; a terminal that does not report its size (a pty without one, some
        ;; CI shells) reports 0: fall back to something usable
        rows (if (pos? (.getRows size)) (.getRows size) 24)
        cols (if (pos? (.getColumns size)) (.getColumns size) 80)
        size (Size. cols rows)]
    (.resize display rows cols)
    (let [lines (vec (take rows (if (= :edit (:mode state))
                                  (edit-lines state rows)
                                  (search-lines state rows))))
          cursor (if (= :edit (:mode state))
                   (.cursorPos size 0 0)
                   (.cursorPos size 0 (+ 8 (count (:query state)))))]
      (.update display lines cursor))))

;;;; state transitions

(defn- refresh
  "Recompute the hit list and keep the cursor inside it. The sorted site names
  are kept in the state: sorting thousands of them on every key press adds up."
  [{:keys [db sites query cursor] :as state}]
  (let [sites (or sites (vec (sort (keys (:sites db)))))
        hits (matches sites query)]
    (assoc state
           :sites sites
           :hits hits
           :cursor (clamp cursor 0 (max 0 (dec (count hits)))))))

(defn- cycle-value
  "Next or previous value for a field, wrapping around."
  [values v step]
  (let [i (or (some (fn [[i x]] (when (= x v) i)) (map-indexed vector values)) 0)]
    (nth values (mod (+ i step) (count values)))))

(defn- adjust [draft {:keys [key values]} step]
  (if values
    (update draft key #(cycle-value values % step))
    (update draft key #(max 1 (+ (or % 1) step)))))

(defn- search-key [{:keys [query cursor hits] :as state} k]
  (case k
    :eof (assoc state :done true)
    :escape (assoc state :done true)
    :up (assoc state :cursor (max 0 (dec cursor)))
    :down (assoc state :cursor (min (max 0 (dec (count hits))) (inc cursor)))
    :backspace (refresh (assoc state :query (subs query 0 (max 0 (dec (count query))))))
    :clear (refresh (assoc state :query ""))
    :enter (if-let [site (nth hits cursor nil)]
             (assoc state
                    :mode :edit
                    :site site
                    :field 0
                    :draft (merge defaults (db/site-settings (:db state) site)))
             state)
    (:left :right :unknown) state
    (refresh (update state :query str k))))

(defn- edit-key [{:keys [site draft field] :as state} k]
  (case k
    :eof (assoc state :done true)
    :escape (assoc state :mode :search)
    :up (assoc state :field (max 0 (dec field)))
    :down (assoc state :field (min (dec (count fields)) (inc field)))
    :left (assoc state :draft (adjust draft (fields field) -1))
    :right (assoc state :draft (adjust draft (fields field) 1))
    :enter (-> state
               (assoc :db (db/merge-site! (:db state) site draft) :mode :search)
               (refresh))
    state))

;;;; main loop

(defn- loop! [^Terminal terminal ^Display display]
  (let [in (.reader terminal)]
    (loop [state (refresh {:db (db/load-db) :query "" :cursor 0 :mode :search})]
      (render! terminal display state)
      (let [k (read-key in)
            state' (if (= :edit (:mode state))
                     (edit-key state k)
                     (search-key state k))]
        (when-not (:done state')
          (recur state'))))))

(defn browse
  "Browse and edit the sites in db.edn."
  [& _]
  (when-not (term/tty?)
    (binding [*out* *err*] (println "Not a terminal."))
    (System/exit 1))
  (let [terminal (term/terminal)
        attrs ^Attributes (.enterRawMode terminal)
        display (Display. terminal true)]
    (try
      (.puts terminal InfoCmp$Capability/enter_ca_mode (object-array 0))
      (loop! terminal display)
      (finally
        (.puts terminal InfoCmp$Capability/exit_ca_mode (object-array 0))
        (.flush terminal)
        (.setAttributes terminal attrs)
        (.close terminal)))))
