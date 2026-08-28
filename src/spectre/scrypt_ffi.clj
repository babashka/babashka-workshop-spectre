(ns spectre.scrypt-ffi
  (:require
   [babashka.ffi :as ffi]))

(def ^:private sodium
  (delay (ffi/load-system-library "sodium")))

(ffi/defcfn sodium-init
  {:library sodium}
  "sodium_init" [] :int)

(ffi/defcfn crypto-pwhash-scryptsalsa208sha256-ll
  {:library sodium}
  "crypto_pwhash_scryptsalsa208sha256_ll"
  [:pointer :size_t :pointer :size_t :ulong :uint32 :uint32 :pointer :size_t] :int)

(defonce ^:private _init
  (assert (zero? (sodium-init)) "sodium_init failed"))

(defn scrypt
  "Derive dk-len raw bytes from passwd and salt using scrypt(N, r, p)."
  [passwd salt n r p dk-len]
  (with-open [arena (ffi/confined-arena)]
    (let [*passwd (ffi/alloc arena (max (alength passwd) 1))
          *salt (ffi/alloc arena (max (alength salt) 1))
          *out (ffi/alloc arena dk-len)]
      (ffi/write-bytes *passwd passwd)
      (ffi/write-bytes *salt salt)
      (let [rc (crypto-pwhash-scryptsalsa208sha256-ll
                *passwd
                (alength passwd)
                *salt
                (alength salt)
                n r p
                *out
                dk-len)]
        (when-not (zero? rc)
          (throw (ex-info "scrypt failed" {:rc rc})))
        (ffi/read-bytes *out dk-len)))))

(comment
  (def bytes (scrypt (.getBytes "password" "UTF-8") (.getBytes "salt" "UTF-8") 32768 8 2 64))

  (def expected (String. (.encode (java.util.Base64/getEncoder) bytes)))

  (assert (= "Hjq3C1jz1aJtpnIjb/tUJULap/wf6G82FkbpqFyHQZQhmv0II9mI56Y8A7mCrFoIiR2NBRlX2WElvbyvbRcZbA=="
             expected)))
