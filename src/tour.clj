(ns tour)

;; Evaluate the examples in your editor.

;;;; babashka.fs
;;
;; Use strings, java.io.File or java.nio.file.Path for paths.
;;
;; https://github.com/babashka/fs - API: https://github.com/babashka/fs/blob/master/API.md

(require '[babashka.fs :as fs])

(comment
  ;; path and file join path segments without creating files.
  (fs/path "src" "spectre" "cli.clj")
  (fs/file (fs/home) ".gitconfig")

  (str (fs/absolutize "bb.edn"))

  (fs/cwd)
  (fs/parent "src/spectre/cli.clj")      ;; => a Path, "src/spectre"
  (fs/file-name "src/spectre/cli.clj")   ;; => "cli.clj"
  (fs/extension "cli.clj")               ;; => "clj"
  (fs/strip-ext "cli.clj")               ;; => "cli"

  (fs/exists? "bb.edn")
  (fs/directory? "src")
  (fs/size "bb.edn")

  (fs/which "git")

  ;; list-dir returns direct children. glob matches paths by pattern.
  (fs/list-dir "src")
  (map str (fs/glob "src" "**.clj"))
  (map fs/file-name (fs/glob "." "**/*_test.clj"))

  (fs/read-all-lines "bb.edn")
  (slurp (fs/file "bb.edn"))

  ;; with-temp-dir deletes the directory on exit, including on exceptions.
  (fs/with-temp-dir [dir]
    (let [f (fs/file dir "hello.txt")]
      (spit f "hi")
      (fs/exists? f)))

  ;; create-dirs creates missing parent directories.
  (fs/with-temp-dir [dir]
    (fs/create-dirs (fs/path dir "a" "b" "c"))
    (fs/delete-tree (fs/path dir "a"))))

;;;; babashka.process
;;
;; Run programs on PATH.
;;
;; https://github.com/babashka/process - API: https://github.com/babashka/process/blob/master/API.md

(require '[babashka.process :as p])

(comment
  ;; shell inherits stdout and stderr.
  (p/shell "bb" "-e" "(println :hello)")
  (p/shell {:dir "src"} "bb" "-e" "(println (babashka.fs/cwd))")

  ;; sh captures stdout and stderr as strings.
  (:out (p/sh "bb" "-e" "(println (+ 1 2))"))
  ;; => "3\n"
  (:out (p/sh "git" "rev-parse" "--short" "HEAD"))

  ;; check throws on a non-zero exit code.
  (p/sh "git" "rev-parse" "--verify" "nope")
  ;; => {:exit 128 :err "fatal: Needed a single revision\n" ...}
  (p/check (p/sh "git" "rev-parse" "--verify" "nope"))

  ;; :continue suppresses exceptions for non-zero exit codes.
  (:exit (p/shell {:continue true} "git" "rev-parse" "--verify" "nope"))
  ;; => 128

  ;; :in supplies stdin. :out :string captures stdout.
  (-> (p/process {:in "hello" :out :string}
                 "bb" "-e" "(print (str/upper-case (slurp *in*)))")
      deref
      :out)
  ;; => "HELLO"

  ;; deref waits for the process to exit.
  (let [proc (p/process {:out :string} "bb" "-e" "(Thread/sleep 1000)")]
    (p/alive? proc)   ;; => true
    (:exit @proc))    ;; => 0, a second later

  (p/tokenize "git commit -m 'a message'")
  ;; => ["git" "commit" "-m" "a message"]

  ;; Pipe one process into another.
  (-> (p/process "bb" "-e" "(run! println [\"b\" \"a\" \"c\"])")
      (p/process {:out :string}
                 "bb" "-e" "(run! println (sort (line-seq (java.io.BufferedReader. *in*))))")
      deref
      :out)
  ;; => "a\nb\nc\n"
  )

;;;; babashka.cli
;;
;; Parse command line arguments into a map.
;;
;; https://github.com/babashka/cli - API: https://github.com/babashka/cli/blob/main/API.md

(require '[babashka.cli :as cli])

(comment
  (cli/parse-opts ["--length" "20" "--symbols"])
  ;; => {:length "20" :symbols true}

  (def spec
    {:length {:coerce :long
              :alias :l
              :default 16
              :desc "Number of characters"}
     :symbols {:coerce :boolean
               :alias :s
               :desc "Include punctuation"}})

  (cli/parse-opts ["-l" "20" "-s"] {:spec spec})
  ;; => {:length 20 :symbols true}

  (cli/parse-opts [] {:spec spec})
  ;; => {:length 16}

  (cli/parse-args ["add" "example.com" "--length" "20"] {:spec spec})
  ;; => {:args ["add" "example.com"] :opts {:length 20}}

  (println (cli/format-opts {:spec spec}))

  ;; dispatch selects a command from a tree. :cmd maps a name to a child node,
  ;; which may have a :cmd of its own. The deepest match wins.
  (def tree
    {:cmd {"add" {:doc "Add a site"
                  :spec spec
                  :args->opts [:site]
                  :fn (fn [m] [:add (:opts m)])}
           "site" {:doc "Manage sites"
                   :cmd {"rm" {:args->opts [:site] :fn (fn [m] [:rm (:opts m)])}}}}})

  (cli/dispatch tree ["add" "example.com" "--length" "20"] {:prog "spectre" :help true})
  ;; => [:add {:length 20 :site "example.com"}]

  (cli/dispatch tree ["site" "rm" "example.com"] {:prog "spectre" :help true})
  ;; => [:rm {:site "example.com"}]

  ;; :help true wires up --help and -h at every level, built from :doc and the
  ;; spec's :desc. No help command to write, no usage string to keep in sync.
  (cli/dispatch tree ["--help"] {:prog "spectre" :help true})
  (cli/dispatch tree ["add" "--help"] {:prog "spectre" :help true})

  ;; In bb.edn, :exec-fn receives parsed options. :exec-args sets defaults.
  )

;;;; babashka.ffi
;;
;; Call C functions.
;;
;; https://github.com/babashka/ffi - API: https://github.com/babashka/ffi/blob/main/API.md

(require '[babashka.ffi :as ffi])

(comment
  ;; load-library tries candidates for the current OS in order.
  (def libc
    (ffi/load-library {:mac ["libc.dylib"]
                       :linux ["libc.so.6"]
                       :windows ["msvcrt.dll"]}))

  ;; load-system-library resolves platform-specific library names.
  (ffi/load-system-library "z")

  ;; cfn takes a library, function name, argument types and return type.
  (def strlen (ffi/cfn libc "strlen" [:pointer] :long))

  ;; with-open frees the arena's memory on exit.
  (with-open [arena (ffi/confined-arena)]
    (strlen (ffi/string->ptr arena "hello")))
  ;; => 5

  ;; defcfn binds a C function to a var.
  (ffi/defcfn c-abs {:library libc} "abs" [:int] :int)
  (c-abs -42)
  ;; => 42

  (with-open [arena (ffi/confined-arena)]
    (let [buf (ffi/alloc arena 4)]
      (ffi/write-array buf :byte (byte-array [1 2 3 4]))
      (vec (ffi/read-array buf :byte 4))))
  ;; => [1 2 3 4]

  ;; Use the longer defcfn arity to convert arguments.
  (ffi/defcfn str-length {:library libc} "strlen" [:pointer] :long
    native-fn
    [s]
    (with-open [arena (ffi/confined-arena)]
      (native-fn (ffi/string->ptr arena s))))

  (str-length "hello")
  ;; => 5
  )
