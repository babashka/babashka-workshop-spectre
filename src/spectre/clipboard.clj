(ns spectre.clipboard
  "Copy to the system clipboard by shelling out."
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]))

(def ^:private tools
  [["pbcopy"]
   ["wl-copy"]
   ["xclip" "-selection" "clipboard"]
   ["xsel" "-ib"]
   ["clip"]])

(defn tool
  "First available clipboard command, or nil."
  []) ;; TODO

(defn copy!
  "Copy s to the clipboard with cmd, by default the first available tool.
   Returns the command used, or nil when there is none. The value goes over
   stdin, never argv."
  ([s] (copy! s (tool)))
  ([s cmd])) ;; TODO

(comment (copy! "foo"))
