(ns spectre.scrypt
  "scrypt for Spectre via OpenSSL 3 `kdf SCRYPT`.
   Override the path with SPECTRE_OPENSSL."
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clojure.string :as str]))

(defn- hex ^String [^bytes b]
  (apply str (map #(format "%02x" (bit-and % 0xff)) b)))

(defn- openssl3?
  "True when bin exists and is real OpenSSL 3+"
  [bin]
  (when-let [bin (fs/which bin)]
    (let [{:keys [exit out]} (try (p/sh bin "version")
                                  (catch Exception _ {:exit 1}))]
      (and (zero? exit) (re-find #"^OpenSSL [3-9]" out)))))

(def ^:private openssl
  (delay
    (or (System/getenv "SPECTRE_OPENSSL")
        (->> ["/opt/homebrew/opt/openssl@3/bin/openssl" ;; brew, Apple Silicon
              "/usr/local/opt/openssl@3/bin/openssl"
              "C:/Program Files/OpenSSL-Win64/bin/openssl.exe"
              "C:/Program Files/OpenSSL-Win64-ARM/bin/openssl.exe"
              "openssl"]
             (filter openssl3?)
             first)
        (throw (ex-info (str/join "\n"
                                  ["OpenSSL 3 not found."
                                   "macOS: brew install openssl@3."
                                   "Windows: winget install openssl"
                                   "Or set SPECTRE_OPENSSL."])
                        {:babashka/exit 1})))))

(defn scrypt
  "Derive dk-len raw bytes from passwd and salt"
  ^bytes [^bytes passwd ^bytes salt n r p dk-len]
  (let [{:keys [out exit err]}
        (p/sh {:out :bytes}
              @openssl "kdf" "-keylen" (str dk-len) "-binary"
              "-kdfopt" (str "hexpass:" (hex passwd))
              "-kdfopt" (str "hexsalt:" (hex salt))
              "-kdfopt" (str "n:" n) "-kdfopt" (str "r:" r) "-kdfopt" (str "p:" p)
              "SCRYPT")]
    (when-not (zero? exit)
      (throw (ex-info (str "openssl kdf failed: " err) {:exit exit})))
    out))

(comment
  @openssl ;; this should be a path to OpenSSL 3

  (def bytes (scrypt (.getBytes "password" "UTF-8") (.getBytes "salt" "UTF-8") 32768 8 2 64))

  (def expected (String. (.encode (java.util.Base64/getEncoder) bytes)))

  (assert (= "Hjq3C1jz1aJtpnIjb/tUJULap/wf6G82FkbpqFyHQZQhmv0II9mI56Y8A7mCrFoIiR2NBRlX2WElvbyvbRcZbA=="
             expected)))
