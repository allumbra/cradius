(ns cradius.dictionary-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :as p ]
            [cradius.dictionary :refer :all]))

; (deftest load-a-dictionary
;   (testing "load-dictionary"
;     (let [dict (load-dictionary "dictionary.rfc2865")]
;       ;(pprint dict)]
;       (is (= (get-in dict [:values "Service-Type" "9"]) "Callback-NAS-Prompt"))))
;   (testing "vendor dictionary"
;     (let [dict (load-dictionary "dictionary.aruba")]
;       (is (= (get-in dict [:vsa "Aruba" :attributes "Aruba-AP-IP-Address" :code]) "34")))))

      
; (load-dictionaries)
; (p/pprint @dictionaries)
; (spit "dictionaries.edn" (p/write @dictionaries :pretty true :stream nil))
; (pprint (attribute "30"))
