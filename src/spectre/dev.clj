(ns spectre.dev
  "The nREPL server behind `bb dev`."
  (:require
   [babashka.deps :as deps]
   [babashka.nrepl.server :as nrepl]
   [babashka.process :as p]
   [clojure.edn :as edn]))

(def ^:private jvm-deps
  '{nrepl/nrepl {:mvn/version "1.7.0"}
    cider/cider-nrepl {:mvn/version "0.62.2"}})

(defn- project-deps
  "This project as a deps map, babashka's built-ins spelled out as real
   dependencies so the same code runs on the JVM."
  []
  (edn/read-string (:out (p/shell {:out :string} "bb print-deps"))))

(def spec
  {:port {:desc "Port to listen on"
          :coerce :long
          :alias :p
          :default 1667}
   :jvm {:desc "Serve from a JVM Clojure, with cider-nrepl"
         :coerce :boolean}})

(defn start
  "Start an nREPL server for your editor.

  Babashka serves it itself, so a change is live the moment you evaluate it.
  --jvm serves the same code from a JVM Clojure instead, which is what you
  want for editors that lean on cider-nrepl."
  {:org.babashka/cli {:spec spec :restrict true :restrict-args true}}
  [{:keys [port jvm]}]
  (if jvm
    @(deps/clojure "-Sdeps" (pr-str (deps/merge-deps [(project-deps) {:deps jvm-deps}]))
                   "-M" "-m" "nrepl.cmdline"
                   "--port" (str port)
                   "--middleware" "[cider.nrepl/cider-middleware]")
    (nrepl/start-server! {:host "127.0.0.1" :port port})))
