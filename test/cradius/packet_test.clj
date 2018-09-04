(ns cradius.packet-test
  (:refer-clojure :exclude [read])
  (:require [clojure.test :refer :all]
            [clojure.pprint :as p]
            [cradius.packet :as packet]
            [clojure.test :refer :all]
            [octet.core :as buf]
            ; [clojurewerkz.buffy.util :refer :all]
            ; [clojurewerkz.buffy.core :refer :all]
            ; [clojurewerkz.buffy.types.protocols :refer :all]
            [byte-streams :as bs]
            [clojure.java.io :as io]))
  ; (:use [gloss.core]
  ;       [gloss.io]))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy (io/input-stream x) out)
    (.toByteArray out)))

(defn load-bindary-file [file-name]
  (-> file-name io/resource slurp-bytes))

(def ba (bs/convert (load-bindary-file "aruba_mac_auth.packet") java.nio.ByteBuffer))

; (clojurewerkz.buffy.util/hex-dump ba)
; code: UInt8, identifier: UInt8, length: UInt16BE
; auth: string 4-20
; after that - attributes
(bs/print-bytes ba)

; (println 
;   (decode radius-protocol ba))
                      
; (defcodec attribute {:type :uint8 :value (repeated :byte :prefix :uint8)})

; (defcodec radius-protocol 
;     { :code :uint8 
;       :identifier :uint8 
;       :length :uint16-be
;       :authenticator (into [] (take 16 (repeat :byte)))
;       :attributes (repeated attribute)})

; (encode radius-protocol {:code 1 :identifier 2 :length 24 
;                           :attributes 
;                             [{:type 26 :value  (into () "hi")}]})

; buffy experiment
; (def rad-header (spec :code       (byte-type)  
;                       :identifier (byte-type)        
;                       :length     (short-type)
;                       :authenticator (string-type 16)))
                      
; (decompose ba rad-header)

(def value-ba (byte-array [ (byte 0x43) 
                            (byte 0x6c)
                            (byte 0x6f)
                            (byte 0x6a)
                            (byte 0x75)]))  
(def buffer (buf/allocate 2000 {:impl :nio :type :heap}))
(buf/write! buffer {:type (byte 1) 
                    :length (byte 5)} 
                    :value value-ba} 
                  packet/attribute-spec)
(def result (buf/read buffer packet/attribute-spec))
(prn result)

(deftest a-test
  (testing "read buff"
    (is (not (= {} result)))))
