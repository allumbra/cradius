;encoder/decoder/verifier for radius packets
(ns cradius.packet
  (require  
    [clojure.string :as s]
    [octet.core :as o]))

(defrecord Attribute [type length value])

(def attribute-spec 
  (o/spec 
    :type o/byte
    :length o/byte))
    :value (o/ref-bytes :length)))

; (def header (o/spec :code o/byte
;                     :identifier o/byte
;                     :length o/int16
;                     :authenticator o/repeat 16 o/byte))
