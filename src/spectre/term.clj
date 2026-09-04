(ns spectre.term
  "JLine helpers: prompts, masked password input."
  (:import
   [org.jline.reader EndOfFileException LineReader LineReaderBuilder UserInterruptException]
   [org.jline.terminal Terminal TerminalBuilder]))

(set! *warn-on-reflection* true)

(defn terminal ^Terminal []
  (-> (TerminalBuilder/builder)
      (.system true)
      (.build)))

(defn line-reader ^LineReader [^Terminal term]
  (-> (LineReaderBuilder/builder)
      (.terminal term)
      (.build)))

(def ^:private reader
  (delay (line-reader (terminal))))

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

(defn password
  "Prompt for a line of input, echoing * per character.
   Falls back to plain (echoed) input when not connected to a terminal."
  [prompt]
  (if (tty?)
    (or-abort (.readLine ^LineReader @reader ^String prompt (Character/valueOf \*)))
    (read-plain prompt)))
