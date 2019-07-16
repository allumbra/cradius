(ns cradius.crypt
  (require  
    [clojure.string :as s]
    [octet.core :as o]
    [byte-streams :as bs]
    [byte-transforms :as bt]
    [cradius.dictionary :as d]
    [cradius.util :as u]
    [clojure.core.rrb-vector :as fv]
    [clojure.pprint :as p]))

(defn md5hash [secret suffix] ; takes a string and  byte array
  (let [buf (o/allocate (+ (count secret) (count suffix)))]
        ; spec (o/spec (o/string (count secret)) (o/repeat (count suffix) o/byte))]
    (o/write! buf secret (o/string (count secret)))
    (o/write! buf suffix (o/repeat (count suffix) o/byte) {:offset (count secret)}) ; suffix (authenticator)
    (bt/hash buf :md5)))
  
(defn len16 [len]  ; round to the nearst 16
  (-> len (/ 16) (Math/ceil) (* 16) (int)))
(defn pad16 [s] ; pad with zeros
  (let [cnt (count s)
        diff (- (len16 cnt) cnt)]
    (str s (apply str (repeat diff (char 0))))))

(defn xor-segment [segment hash-arr] ; both should be same length
  (let [  
          chars (into [] segment)
          hashed  (mapv (fn [i]
                          (bit-xor  (int (nth chars i)) 
                                    (nth hash-arr i)))
                    (range (count hash-arr)))]
      hashed)) 

(defn xcrypt-password [password secret authenticator is-decrypt]
  (let [buf-len (if (string? password) 
                    (len16 (count password))
                    (o/get-capacity password))
        pw-buff (o/allocate buf-len)
        xor-buffer (o/allocate buf-len)]
    (if (string? password)
        (o/write! pw-buff (pad16 password) (o/string buf-len)) ; encrypt string pw
        (o/write! pw-buff (bs/to-byte-array password) (o/repeat buf-len o/byte))) ; decrypt encr password
    (loop [ offset 0
            hash-suffix authenticator]
        (if (>= offset buf-len) 
            ; if offset >= bufflen return value
            (if (string? password) 
              xor-buffer  ; return buffer
              (re-find #"^.+?(?=\x00|$)" (-> xor-buffer bs/to-string s/trim))) ; return string
            ; recurse
            (let [hsh (md5hash secret hash-suffix) ; else bufflen < offset
                  seg (o/read pw-buff (o/repeat 16 o/byte) {:offset offset})
                  xor-seg (xor-segment seg hsh)]
              (o/write! xor-buffer xor-seg (o/repeat 16 o/byte) {:offset offset})              
              (recur (+ 16 offset) (if is-decrypt seg xor-seg)))))))

(defn decrypt-password [pw-arr secret authenticator]
  (let [pw-buff (if (vector? pw-arr) 
                  (u/buff-from-byte-array pw-arr)
                  pw-arr)]
    (prn "decrypt: ")
    (prn pw-arr)
    (prn pw-buff)
    (xcrypt-password pw-buff secret authenticator true)))

(defn encrypt-password [pw secret authenticator]
    (xcrypt-password pw secret authenticator false))
