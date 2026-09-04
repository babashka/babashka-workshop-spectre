(ns spectre.clipboard-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [spectre.clipboard :as clipboard]))

(deftest copy-test
  (testing "the value is passed over stdin"
    (let [f (fs/create-temp-file {:suffix ".clip"})
          bb (or (System/getenv "BABASHKA_BINARY") "bb")
          fake [bb "-e" (str "(spit " (pr-str (str f)) " (slurp *in*))")]]
      (try
        (is (some? (clipboard/copy! "SECRET" fake)))
        (is (= "SECRET" (slurp (str f))))
        (finally (fs/delete-if-exists f))))))

(deftest no-tool-test
  (testing "returns nil so callers can fall back to printing"
    (is (nil? (clipboard/copy! "SECRET" nil)))))

;; The real clipboard is not available everywhere: Linux runners need a display
;; (xvfb-run) and Windows needs an interactive session. This overwrites and
;; restores the clipboard, so it only runs when asked for.

(defn- paste-cmd []
  (some (fn [[exe :as cmd]] (when (fs/which exe) cmd))
        [["pbpaste"]
         ["wl-paste" "--no-newline"]
         ["xclip" "-selection" "clipboard" "-o"]
         ["xsel" "-ob"]
         ["powershell" "-NoProfile" "-Command" "Get-Clipboard -Raw"]]))

(defn- pasted
  "Clipboard contents, without the trailing newline paste commands add."
  [cmd]
  (str/replace (:out (apply p/sh cmd)) #"\r?\n\z" ""))

(deftest ^:clipboard real-clipboard-test
  (when (System/getenv "SPECTRE_CLIPBOARD_TEST")
    (let [copy (clipboard/tool)
          paste (paste-cmd)]
      (is (some? copy) "a clipboard tool is available")
      (is (some? paste) "a paste command is available to check with")
      (when (and copy paste)
        (let [previous (pasted paste)]
          (try
            (testing "the value reaches the system clipboard unchanged"
              (clipboard/copy! "spectre-test-value")
              (is (= "spectre-test-value" (pasted paste))))
            (finally
              (clipboard/copy! previous))))))))
