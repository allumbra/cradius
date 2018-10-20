(ns cradius.packet-test
  (:refer-clojure :exclude [read])
  (:require [clojure.test :refer :all]
            [clojure.pprint :as p]
            [cradius.packet :as packet]
            [clojure.test :refer :all]
            [octet.core :as o]
            [octet.core :as buf]
            [byte-streams :as bs]
            [byte-transforms :as bt]
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
(def secret "nearbuy")
(def ba (bs/convert (load-binary-file "aruba_mac_auth.packet") java.nio.ByteBuffer))

(bs/print-bytes ba)

(p/pprint (packet/parse-radius ba))

(deftest a-test
  (testing "packet size"
    (is (= (packet/len16 15) 16))
    (is (= (packet/len16 16) 16))
    (is (= (packet/len16 17) 32))
    (is (= (count (packet/pad16 "passwordpasswordpassword")) 
          32))))

(prn (packet/xor-segment "password" (range 8)))
(prn "password:")
(let [enc (packet/encrypt-password "passwordpasswordpassword" "shhhhh" [-8 -95 35 41 -57 -19 90 110 37 104 81 82 67 -17 -71 24] false)
      dec (packet/encrypt-password enc "shhhhh" [-8 -95 35 41 -57 -19 90 110 37 104 81 82 67 -17 -71 24] true)]
  (bs/print-bytes enc)
  (prn (bs/to-char-sequence enc))
  (prn enc)
  (prn "decode:")
  (bs/print-bytes dec)
  (prn dec))

  ; (let [enc (packet/encrypt-password "password" "shhhhh" "1234567890" false)]
;   (bs/print-bytes enc)
;   (prn (bs/to-char-sequence enc))
;   (prn (bs/to-string enc)))

; (prn (bs/to-string (packet/md5hash "foo" "bar")))
    ; (deftest a-test

    

