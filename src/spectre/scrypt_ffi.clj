(ns spectre.scrypt-ffi
  (:require
   [babashka.ffi :as ffi]))

(defonce ^:private sodium
  (ffi/load-library {:linux ["libsodium.so.26" "libsodium.so"]
                     :mac ["/opt/homebrew/opt/libsodium/lib/libsodium.26.dylib"]
                     :windows ["libsodium-26.dll"]}))

(defonce ^:private _init
  (let [sodium-init (ffi/cfn sodium "sodium_init" [] :int)]
    (assert (zero? (sodium-init)) "sodium_init failed")))

(ffi/defcfn scrypt
  "crypto_pwhash_scryptsalsa208sha256_ll"
  [:pointer :size_t :pointer :size_t :ulong :uint32 :uint32 :pointer :size_t] :int
  native-fn
  [passwd salt n r p dk-len]
  (with-open [arena (ffi/confined-arena)]
    (let [*passwd (ffi/alloc arena (max (alength passwd) 1))
          *salt (ffi/alloc arena (max (alength salt) 1))
          *out (ffi/alloc arena dk-len)]
      (ffi/write-bytes *passwd passwd)
      (ffi/write-bytes *salt salt)
      (let [rc (native-fn
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
