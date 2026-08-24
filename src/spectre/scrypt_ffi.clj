(ns spectre.scrypt-ffi
  (:require
   [babashka.ffi :as ffi]))

(def ^:private sodium
  (delay
    (ffi/load-system-library "sodium")))

(ffi/defcfn sodium-init "sodium_init" [] :int)

(ffi/defcfn crypto-pwhash-scryptsalsa208sha256-ll
  "crypto_pwhash_scryptsalsa208sha256_ll"
  [:pointer :size_t :pointer :size_t :ulong :uint32 :uint32 :pointer :size_t] :int)

(defonce ^:private _init
  (do @sodium
      (assert (zero? (sodium-init)) "sodium_init failed")))

(defn- bytes->ptr
  [b]
  (let [len (alength b)
        p (ffi/alloc (max len 1))]
    (dotimes [i len]
      (ffi/write p :int8 i (aget b i)))
    p))

(defn- ptr->bytes
  [p len]
  (let [out (byte-array len)]
    (dotimes [i len]
      (aset-byte out i (unchecked-byte (ffi/read p :int8 i))))
    out))

(defn scrypt
  "Derive dk-len raw bytes from passwd and salt using scrypt(N, r, p)."
  [^bytes passwd ^bytes salt n r p dk-len]
  (let [passwd-ptr (bytes->ptr passwd)
        salt-ptr (bytes->ptr salt)
        out-ptr (ffi/alloc dk-len)]
    (try
      (let [rc (crypto-pwhash-scryptsalsa208sha256-ll
                passwd-ptr
                (alength passwd)
                salt-ptr
                (alength salt)
                n r p
                out-ptr
                dk-len)]
        (when-not (zero? rc)
          (throw (ex-info "scrypt failed" {:rc rc})))
        (ptr->bytes out-ptr dk-len))
      (finally
        (ffi/free passwd-ptr)
        (ffi/free salt-ptr)
        (ffi/free out-ptr)))))

(comment
  (def bytes (scrypt (.getBytes "password" "UTF-8") (.getBytes "salt" "UTF-8") 32768 8 2 64))
  (def expected (String. (.encode (java.util.Base64/getEncoder) bytes)))
  (assert (= "Hjq3C1jz1aJtpnIjb/tUJULap/wf6G82FkbpqFyHQZQhmv0II9mI56Y8A7mCrFoIiR2NBRlX2WElvbyvbRcZbA=="
             expected)))
