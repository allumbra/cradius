(ns cradius.util
  (require  
    [clojure.string :as s]
    [octet.core :as o]
    [byte-streams :as bs]
    [byte-transforms :as bt]))
   
(defn deep-merge [v & vs]
  (letfn [(rec-merge [v1 v2]
            (if (and (map? v1) (map? v2))
              (merge-with deep-merge v1 v2)
              v2))]
    (if (some identity vs)
      (reduce #(rec-merge %1 %2) v vs)
      (last vs))))

(defn buff-from-byte-array [arr]
  (let [buff (o/allocate (count arr))]
    (o/write! buff arr (o/repeat (count arr) o/byte))
    buff))

(defn byte-array-from-buff [buff])
  
