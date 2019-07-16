(ns cradius.packet
  (require  
    [clojure.string :as s]
    [octet.core :as o]
    [byte-streams :as bs]
    [byte-transforms :as bt]
    [cradius.dictionary :as d]
    [cradius.util :as u]
    [cradius.crypt :as c]
    [clojure.core.rrb-vector :as fv]
    [clojure.pprint :as p]))

(defrecord Attribute [type length value])

(def secret (atom ""))
(defn set-secret [sec]
  (reset! secret sec))
(defn get-secret [] @secret)

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


; item ex: {:type 61, :length 6, :value [0 0 0 19]}
; ATTRIBUTE	Service-Type				6	integer
;":Service-Type"
;  {:name "Service-Type", :code "6", :type "integer", :vendor ""})

; TODO - account for vendor prefix
(defn convert-type [value attribute] ; value is byte array
    (let [type (:type attribute)
          att-name (:name attribute)
          buff (u/buff-from-byte-array value)
          result  (case type
                    ("string" "text") (o/read buff (o/string (count value)))
                    "ipaddr" (s/join "." value)
                    "date" (* 1000 (o/read buff o/int32))
                    "time" (o/read buff o/int32)
                    ; if the result is an integer - it could be a table lookup
                    "integer" (let [int-val (o/read buff o/int32)
                                    table-val (d/value att-name int-val)]
                                 (if (nil? table-val)
                                    int-val
                                    table-val))  
                    value)] 
        (if (= att-name "User-Password")
            value ; keep it as byte array for now
            result)))

(defn vsa [attr item]
    (let [buff (o/allocate (:length item)) ; npe
          _ (o/write! buff (:value item) (o/repeat (:length item) o/byte)) 
          vsa-head (o/read 
                      buff
                      vsa-header)
          val-byte-vec (o/read buff (o/repeat (:vendor-length vsa-head) o/byte) {:offset 6})
          attr-spec (d/attribute (:vendor-id vsa-head) (:vendor-type vsa-head))
          value (convert-type val-byte-vec (:type attr-spec))]
        { :vendors #{(:vendor attr-spec)}
          :attributes
            {(:name attr-spec) (convert-type value attr-spec)}}))
          
(defn convert-item-to-human-readable [item]
  (let [attr (d/attribute (:type item))] 
      (prn attr)
      (if (= 26 (:type item))
          (vsa attr item)
          {:attributes   ; normal attribute
            {(:name attr) (convert-type (:value item) attr)}})))

(defn to-human-readable [packet]
    (let [new-attributes
            (reduce (fn [result item]
                      (let [attr (d/attribute (str (:type item))) 
                            attr-map (if (= 26 (:type item)) ; 26 is vsa
                                        (vsa attr item)
                                        {(:name attr) (convert-type (:value item) attr)})]
                        (u/deep-merge result attr-map))) ; might be slow
              {} (:attributes packet))
          attrs-and-vsas {}] 
      (assoc packet :attributes new-attributes)))

(defn apply-values [obj]
  ; "For each attribute, see if there is an associated VALUE table and replace VALUE id with
  ;   string description"
  (let [atts (get obj :attributes)
        new-atts (map (fn [k v]
                        {k (d/value k v)})
                      atts)]
      (assoc obj :attributes new-atts)))

(defn parse-radius [radius-buffer]
  (let [raw   (load-radius-packet radius-buffer)
        prad  (to-human-readable raw)
        pw-arr (get-in prad [:attributes "User-Password"])
        pw-authenticator (get-in prad [:header :authenticator])
        pw (c/decrypt-password pw-arr (get-secret) pw-authenticator)]
    (prn raw)
    (assoc-in prad [:attributes  "User-Password"] pw)))

; TODO
; Values lookups - needs to be a seperate pass on the data
    ; optimize octet specs
; pull :vendors list out of attributes

; remove :attributes in :attributes
; encoding json -> radius
; start/stop UDP server
; add udp server to it's own non/core file (do not initialize on start up)
; turn into includable lib

