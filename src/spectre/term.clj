(ns spectre.term
  "JLine helpers: prompts, masked password input."
  (:import
   [org.jline.reader EndOfFileException Highlighter LineReader LineReaderBuilder UserInterruptException]
   [org.jline.reader.impl LineReaderImpl]
   [org.jline.terminal Terminal TerminalBuilder]
   [org.jline.utils AttributedString]))

(set! *warn-on-reflection* true)

(defn terminal ^Terminal []
  (-> (TerminalBuilder/builder)
      (.system true)
      (.build)))

(defn line-reader ^LineReader [^Terminal term]
  (-> (LineReaderBuilder/builder)
      (.terminal term)
      (.build)))

;; one system terminal per process: a second TerminalBuilder falls back to a
;; dumb terminal, and a dumb terminal echoes the password in plain text
(def ^:private term (delay (terminal)))

(def ^:private reader
  (delay (line-reader @term)))

;; a second reader for secrets: what is typed there must not end up in history
(def ^:private secret-reader
  (delay (doto ^LineReaderImpl (line-reader @term)
           (.setVariable "disable-history" true))))

(defn tty?
  "True when running attached to a terminal, false when piped or redirected."
  []
  (some? (System/console)))

(defn- read-plain [prompt]
  (print prompt)
  (flush)
  (read-line))

(defmacro ^:private or-abort
  "Run a jline read, exiting quietly on ctrl-c (130) or ctrl-d (1)."
  [& body]
  `(try
     ~@body
     (catch UserInterruptException _# (System/exit 130))
     (catch EndOfFileException _# (System/exit 1))))

(defn input
  "Prompt for a line of input."
  [prompt]
  (if (tty?)
    (or-abort (.readLine ^LineReader @reader ^String prompt))
    (read-plain prompt)))

(defn- masked
  "A highlighter that draws a * per character typed, then what `figure` makes of
   the text so far. JLine calls it on every keystroke, so the figure follows the
   typing."
  ^Highlighter [figure]
  (reify Highlighter
    (highlight [_ _ text]
      (AttributedString. (str (apply str (repeat (count text) \*))
                              (when (and figure (seq text)) (str "  " (figure text))))))
    (setErrorPattern [_ _])
    (setErrorIndex [_ _])))

(defn password
  "Prompt for a line of input, echoing * per character. With `figure`, a fn of
   the text typed so far, its result is drawn after the stars and redrawn on
   every keystroke. Falls back to plain (echoed) input when not connected to a
   terminal."
  ([prompt] (password prompt nil))
  ([prompt figure]
   (if (tty?)
     (let [^LineReaderImpl r @secret-reader]
       (.setHighlighter r (masked figure))
       (or-abort (.readLine r ^String prompt)))
     (read-plain prompt))))
