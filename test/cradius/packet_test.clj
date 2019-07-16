(ns cradius.packet-test
  (:refer-clojure :exclude [read])
  (:require [clojure.test :refer :all]
            [clojure.pprint :as p]
            [cradius.packet :as packet]
            [clojure.test :refer :all]
            [octet.core :as o]
            [octet.core :as buf]
            [byte-streams :as bs]
            [cradius.crypt :as c]
            [cradius.dictionary :as d]
            [byte-transforms :as bt]
            [clojure.java.io :as io]))

(d/load-dictionaries)
(packet/set-secret "nearbuy")

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

(deftest convert-item
  (testing "convert-item"
    (let [item {:type 4, :length 6, :value [10 0 0 90]}]
      (is (= {:attributes {"NAS-IP-Address" "10.0.0.90"}} (packet/convert-item-to-human-readable item))))))

(let [aruba (packet/parse-radius ba)
      _ (p/pprint aruba)
      pw (get-in aruba [:attributes "User-Password"])
      _ (prn (:arglists (meta #'packet/apply-values)))
      st (packet/apply-values aruba)])
    ; (prn st))
  ;     aruba-pw (c/decrypt-password  pw
  ;                   secret
  ;                   authenticator)]
  ; (println "\n\naruba pw: " aruba-pw))


  ; (let [enc (packet/encrypt-password "password" "shhhhh" "1234567890" false)]
;   (bs/print-bytes enc)
;   (prn (bs/to-char-sequence enc))
;   (prn (bs/to-string enc)))

; (prn (bs/to-string (packet/md5hash "foo" "bar")))
    ; (deftest a-test

    

