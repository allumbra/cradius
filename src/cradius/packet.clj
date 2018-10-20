(ns cradius.packet
  (require  
    [clojure.string :as s]
    [octet.core :as o]
    [byte-streams :as bs]
    [byte-transforms :as bt]
    [cradius.dictionary :as d]
    [cradius.util :as u]
    [clojure.core.rrb-vector :as fv]
    [clojure.pprint :as p]))

(defrecord Attribute [type length value])

(def attribute-header 
  (o/spec 
    :type o/byte
    :length o/byte))

(def vsa-header
    (o/spec
        :vendor-id o/int32
        :vendor-type o/byte     ; vsa "type" not data type.  Need to look up data type in dictionary
        :vendor-length o/byte)) ; if this length is the the length of the following value, we can remove this

(def header (o/spec :code o/byte
                    :identifier o/byte
                    :length o/int16
                    :authenticator (o/repeat 16 o/byte)))

(defn read-attributes [buffer length]
  (let [offset 20]
    (loop [pos offset
           result []]
        (let [r (last result)
              v (:value r)]
          (if (>= pos length)
            result
            (let[ att-header (o/read buffer attribute-header {:offset pos}) 
                  attribute-value (o/read buffer (o/repeat (- (:length att-header) 2) o/byte) {:offset (+ 2 pos)})
                  size (:length att-header)]
              (recur (+ pos size) (conj result {:type (:type att-header)
                                                :length size
                                                :value attribute-value}))))))))

(defn load-radius-packet [radius-buffer]
  (let [header (o/read radius-buffer header)]
    { :header header
      :attributes (read-attributes radius-buffer (:length header))})) 

(defn buff-from-byte-array [arr]
    (let [buff (o/allocate (count arr))]
      (o/write! buff arr (o/repeat (count arr) o/byte))
      buff))

(defn convert-type [value type] ; value is byte array
    (let [buff (buff-from-byte-array value)]
      (case type
        ("string" "text") (o/read buff (o/string (count value)))
        "ipaddr" (s/join "." value)
        "date" (* 1000 (o/read buff o/int32))
        ("integer" "time") (o/read buff o/int32)
        value))) 

(defn vsa [attr item]
    (let [buff (o/allocate (:length item)) ; npe
          _ (o/write! buff (:value item) (o/repeat (:length item) o/byte)) ; mutates buff :(
          vsa-head (o/read 
                      buff
                      vsa-header)
          val-byte-vec (o/read buff (o/repeat (:vendor-length vsa-head) o/byte) {:offset 6})
          attr-spec (d/attribute (:vendor-id vsa-head) (:vendor-type vsa-head))
          value (convert-type val-byte-vec (:type attr-spec))]
        { :vendors #{(:vendor attr-spec)}
          :attributes
            {(:name attr-spec) value}}))
          
(defn to-human-readable [packet]
    (let [new-attributes
                        (reduce (fn [result item]
                                  (let [attr (d/attribute (:type item)) 
                                        attr-map (if (= 26 (:type item))
                                                    (vsa attr item)
                                                    {:attributes   ; normal attribute
                                                      {(:name attr) (convert-type (:value item) (:type attr))}})]
                                    (u/deep-merge result attr-map))) ; might be slow
                          {} (:attributes packet))
          attrs-and-vsas {}] 
      (assoc packet :attributes new-attributes)))

(defn parse-radius [radius-buffer]
  (-> radius-buffer
      (load-radius-packet)
      (to-human-readable)))

    
; (defn md5hash [& args] ; all args are
;   (bt/hash (apply concat args) :md5))
(defn md5hash [secret suffix] ; takes a string and  byte array
  (let [buf (o/allocate (+ (count secret) (count suffix)))]
    (o/write! buf secret (o/string (count secret)))
    (o/write! buf suffix (o/repeat (count suffix) o/byte))
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


;; need to change the password, authenticator to be a byte array - to allow for decrypting with the same function
(defn encrypt-password [password secret authenticator is-decrypt]
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
            (if (string? password)
              xor-buffer
              (s/trim (bs/to-string xor-buffer)))
            (let [hsh (md5hash secret hash-suffix)
                  seg (o/read pw-buff (o/repeat 16 o/byte) {:offset offset})
                  xor-seg (xor-segment seg hsh)]
              (o/write! xor-buffer xor-seg (o/repeat 16 o/byte) {:offset offset})              
              (recur (+ 16 offset) (if is-decrypt seg xor-seg)))))))

; (defn decrypt-password [password secret authenticator]
;   (let [pw-buff (o/allocate)]))
  

    ; TODO
    ; user secret
    ; optimize octet specs

; encoding json -> radius
; start/stop UDP server
; add udp server to it's own non/core file (do not initialize on start up)
; turn into includable lib

