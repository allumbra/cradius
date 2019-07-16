(ns cradius.crypt-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :as p]
            [cradius.packet :as packet]
            [byte-streams :as bs]
            [cradius.crypt :refer :all]))

(deftest length16
  (testing "packet size"
    (is (= (len16 15) 16))
    (is (= (len16 16) 16))
    (is (= (len16 17) 32))
    (is (= (count (pad16 "passwordpasswordpassword")) 
          32))))

(def authenticator [74 69 -6 -32 -122 -39 -31 20 40 107 55 -75 -13 113 -20 108])

(deftest pw-test
  (let [test-password "passwordpasswordpasswor"
        test-secret "shhhhh"]
    (testing "enc/dec"
      (let [enc (xcrypt-password test-password test-secret authenticator false)
            dec (xcrypt-password enc test-secret authenticator true)]
        (is (= dec test-password))
        (prn dec))
      (let [enc (encrypt-password test-password test-secret authenticator)
            _ (prn enc)
            dec (decrypt-password enc test-secret authenticator)]
        (is (= dec test-password)))
      (let [enc (encrypt-password test-password test-secret authenticator)
            _ (prn enc)
            dec (decrypt-password enc test-secret authenticator)]
        (is (= dec test-password))))))

(deftest aruba-pw-test
  (let [message-authenticator [-8 -95 35 41 -57 -19 90 110 37 104 81 82 67 -17 -71 24]
        encrypted-password [-21
                            46
                            -9
                            -24
                            62
                            -63
                            -96
                            94
                            4
                            -5
                            92
                            109
                            -111
                            -32
                            -120
                            86
                            -102
                            -103
                            15
                            -94
                            -79
                            -78
                            -36
                            106
                            15
                            4
                            -123
                            -106
                            8
                            17
                            100
                            -51]
        test-secret "nearbuy"]

;; questions:
;; 1. if I pass a byte array to the above test, does it still work?
    (testing "dec"
      (let [dec1 (decrypt-password encrypted-password test-secret authenticator)
            dec2 (decrypt-password encrypted-password test-secret message-authenticator)]
        ; (is (= dec test-password))
        (prn dec1)
        (prn dec2)))))

