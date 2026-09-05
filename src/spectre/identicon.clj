(ns spectre.identicon
  (:import [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))

(def left-arm [\╔ \╚ \╰ \═])

(def right-arm [\╗ \╝ \╯ \═])

(def body [\█ \░ \▒ \▓ \☺ \☻])

(def accessory
  [\◈ \◎ \◐ \◑ \◒ \◓ \☀ \☁ \☂ \☃ \☄ \★ \☆ \☎ \☏ \⎈ \⌂ \☘
   \☢ \☣ \☕ \⌚ \⌛ \⏰ \⚡ \⛄ \⛅ \☔ \♔ \♕ \♖ \♗ \♘ \♙
   \♚ \♛ \♜ \♝ \♞ \♟ \♨ \♩ \♪ \♫ \⚐ \⚑ \⚔ \⚖ \⚙ \⚠ \⌘ \⏎
   \✄ \✆ \✈ \✉ \✌])

(defn hmac-sha256
  [key data]
  (let [mac (Mac/getInstance "HmacSHA256")]
    (.init mac (SecretKeySpec. (.getBytes key "UTF-8") "HmacSHA256"))
    (.doFinal mac (.getBytes data "UTF-8"))))

(defn identicon-of
  [full-name main-pass]
  (let [seed (hmac-sha256 main-pass full-name)
        b #(Byte/toUnsignedInt (aget seed %))]
    (str
     (nth left-arm (mod (b 0) (count left-arm)))
     (nth body (mod (b 1) (count body)))
     (nth right-arm (mod (b 2) (count right-arm)))
     (nth accessory (mod (b 3) (count accessory))))))

(defn from-env
  "The identicon for SPECTRE_NAME and SPECTRE_MASTER in `env`, a map like the
   one (System/getenv) returns. Nil when either is not set."
  [env]
  (let [full-name (get env "SPECTRE_NAME")
        main-pass (get env "SPECTRE_MASTER")]
    (when (and (seq full-name) (seq main-pass))
      (identicon-of full-name main-pass))))

(comment
  (hmac-sha256 "correct horse battery staple" "lispyclouds")

  (identicon-of "lispyclouds" "correct horse battery staple"))
