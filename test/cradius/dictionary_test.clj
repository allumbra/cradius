(ns cradius.dictionary-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :as p]
            [cradius.dictionary :refer :all]))

(load-dictionaries)

(deftest load-a-dictionary
  (testing "load-dictionary"
    (let [dict (load-dictionary (dictionary-file "dictionary.rfc2865"))]
      (p/pprint dict)
      (is (= ":29" (attr-key "" 29)))
      (is (= {:code "29", :name "Termination-Action", :type "integer", :vendor ""} (attribute 29)))
      (is (= {:name "Login-IP-Host", :code "14", :type "ipaddr", :vendor ""} (attribute "Login-IP-Host")))
      (is (= "Callback-NAS-Prompt" (value "Service-Type" "9"))))))

(println "values")
(prn (get-in @dictionaries [:values ":NAS-Port-Type" "32"]))

  ; (testing "vendor dictionary"
  ;   (let [dict (load-dictionary (dictionary-file "dictionary.aruba"))]
  ;     (is (= (get-in dict [:vsa "Aruba" :attributes "Aruba-AP-IP-Address" :code]) "34")))))

      
; ; (p/pprint @dictionaries)
; ; (spit "dictionaries.edn" (p/write @dictionaries :pretty true :stream nil))
; (p/pprint (attribute "30"))
; (println "attribute get" (get-in @dictionaries [:attributes (attr-key "311" "23")]))

