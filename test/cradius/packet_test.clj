(ns cradius.packet-test
  (:refer-clojure :exclude [read])
  (:require [clojure.test :refer :all]
            [clojure.pprint :as p]
            [cradius.packet :as packet]
            [clojure.test :refer :all]
            [octet.core :as buf]
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

(defn load-binary-file [file-name]
  (-> file-name io/resource slurp-bytes))

(def ba (bs/convert (load-binary-file "aruba_mac_auth.packet") java.nio.ByteBuffer))

; (clojurewerkz.buffy.util/hex-dump ba)
; code: UInt8, identifier: UInt8, length: UInt16BE
; auth: string 4-20
; after that - attributes
(bs/print-bytes ba)

(p/pprint (packet/parse-radius ba))

; (deftest a-test
;   (testing "read buff"
;     (is (not (= {} nil)))))

    

