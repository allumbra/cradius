;encoder/decoder/verifier for radius packets
(ns cradius.packet
  (require  
    [clojure.string :as s]
    [octet.core :as o]
    [byte-streams :as bs]
    [byte-transforms :as bt]
    [cradius.dictionary :as d]
    [cradius.util :as u]
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

; simply converts a byte array to the appropritate type: int, string etc
(defn convert-type [value type] ; value is byte array
    (prn type value)
    (let [buff (buff-from-byte-array value)]
      (case type
        ("string" "text") (o/read buff (o/string (count value)))
        "ipaddr" (s/join "." value)
        "date" (* 1000 (o/read buff o/int32))
        ("integer" "time") (o/read buff o/int32)
        value))) 

; should result in a map {:Vendor-Specific {:Aruba {:attribute-name value}}} that gets merged
(defn vsa [attr item]
    (prn attr)
    (prn item)
    (let [buff (o/allocate (:length item)) ; npe
          _ (o/write! buff (:value item) (o/repeat (:length item) o/byte)) ; mutates buff :(
          vsa-head (o/read 
                      buff
                      vsa-header)
          val-byte-vec (o/read buff (o/repeat (:vendor-length vsa-head) o/byte) {:offset 6})
          _ (println "vsa-head" vsa-head val-byte-vec)
          value (convert-type val-byte-vec (:vendor-type vsa-head))]
        (prn value)
        {:attributes
            {}}))
          
    ; read header
    ; read value
    ; return map to merge - or assoc-in path and value

;vendor specific is tricky
; - options:  Vendor-Specific/"Vendor"/Attribute : value - essentially analogous to how it's stored in the packet 
(defn to-human-readable [packet]
    (let [new-attributes
                        (reduce (fn [result item]
                                  (let [attr (d/attribute (str (:type item))) 
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

; parsing:
    ; parse vsa
    ; user secret
    ; optimize octet specs

; encoding json -> radius

; start/stop UDP server

; 

; turn into includable lib

