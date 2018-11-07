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


(println "user-password")
(let [aruba (packet/parse-radius ba)
      _ (p/pprint aruba)
      pw (get-in aruba [:attributes :attributes "User-Password"])
      ; authenticator (get-in aruba [:attributes "Message-Authenticator"])
      authenticator (get-in aruba [:header :authenticator])
      aruba-pw (packet/decrypt-password  pw
                    secret
                    authenticator)]
  (println "\n\naruba pw: " aruba-pw))
                            
  
(deftest a-test
  (testing "packet size"
    (is (= (packet/len16 15) 16))
    (is (= (packet/len16 16) 16))
    (is (= (packet/len16 17) 32))
    (is (= (count (packet/pad16 "passwordpasswordpassword")) 
          32))))

(def authenticator [74 69 -6 -32 -122 -39 -31 20 40 107 55 -75 -13 113 -20 108])
(deftest pw-test
  (testing "enc/dec"
    (let [enc (packet/encrypt-password "passwordpasswordpassword" "shhhhh" authenticator false)
          dec (packet/encrypt-password enc "shhhhh" authenticator true)]
      (println "classes enc/dec" (class enc) (class dec)))))
(prn (packet/xor-segment "password" (range 8)))
(prn "password:")
(let [enc (packet/encrypt-password "passwordpasswordpassword" "shhhhh" authenticator false)
      dec (packet/encrypt-password enc "shhhhh" authenticator true)]
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

    

