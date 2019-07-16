(ns cradius.dictionary
  (require  
            [clojure.string :as s]
            [clojure.java.io :as io]
            [cradius.util :as u]))

(defn process-value 
  ( [result vendor-name vendor-id [collection name code]]  ; vendor specific form
    (let [key (str vendor-name ":" collection)
          rev-key (str vendor-id ":" collection)]
      (-> result
          (assoc-in [:values key name] code)
          (assoc-in [:values rev-key code] name)))) ; reverse lookup  
  ( [result  [collection name code]]  ; standard form
    (process-value result "" "" [collection name code])))                                                                                                                                                      
                                                    
(defn process-attribute
  (   [ result vendor-id vendor-name [attribute-name attribute-code type]] ;vendor specific form
    (let [value {:name attribute-name :code attribute-code :type type :vendor vendor-name}
          key (str vendor-name ":" attribute-name)
          rev-key (str vendor-id ":" attribute-code)]
      (-> result
          (assoc-in [:attributes key] value)
          (assoc-in [:attributes rev-key] value)))) ; reverse lookup
  (   [result  [ attribute-name attribute-code type]] ; normal form
    (process-attribute result "" "" [attribute-name attribute-code type]))) 
                                      
(defn parse-vendor [lines]
  (let [lines-with-begin-vendor (filter (fn [l] (some? (re-find #"^BEGIN-VENDOR" l))) lines)]
    (if (not (empty? lines-with-begin-vendor))
      (let [
            vendor-line (filter (fn [l] (some? (re-find #"^VENDOR" l))) lines)
            result (rest (s/split (first vendor-line) #"\s+"))]
        (if (empty? vendor-line)
            nil
            result))
      nil)))

(def dictionary-resource-path "./resources/dictionaries")

(defn dictionary-file [dict]
  (->
    (.getCanonicalPath (clojure.java.io/file (str dictionary-resource-path "/" dict)))
    (clojure.java.io/file)))


(defn load-dictionary
  [file-name]
  (let [  lines (-> file-name slurp s/split-lines)  ; todo - use canonical path?
          [vendor-name vendor-id] (parse-vendor lines)
          base (if vendor-name 
                  {:vendors {vendor-name vendor-id
                              vendor-id vendor-name}}
                  {})]
    (reduce (fn [result line]
              (let [tokens (s/split line #"\s+")]
                  (case (first tokens)
                    "VALUE" (if vendor-name
                              (process-value result vendor-id vendor-name (into [] (rest tokens)))
                              (process-value result (into [] (rest tokens))))
                    "ATTRIBUTE" (if vendor-name
                                    (process-attribute result vendor-id vendor-name (into [] (rest tokens)))
                                    (process-attribute result (into [] (rest tokens))))
                    "VENDORATTR" (process-attribute result vendor-id vendor-name (into [] (rest tokens)))
                    result)))
            base lines)))

(def dictionaries (atom {}))

(defn load-dictionaries []
  (let [files (->
                (.getCanonicalPath (clojure.java.io/file "./resources/dictionaries"))
                (clojure.java.io/file)
                (file-seq)
                (rest)) ; skip 1st (directory)
        ds    (reduce (fn [result file] 
                          (println (.getPath file))
                          (u/deep-merge result (load-dictionary (.getPath file))))        
                  {} files)]
      (reset! dictionaries ds)))

(defn attr-key [vendor attribute-name]
  (str vendor ":" attribute-name))

(defn attribute 
  ([attr] (attribute "" attr))
  ([vendor attr] (get-in @dictionaries [:attributes (attr-key vendor attr)])))
    
; for attributes that have an associated table, attempt to look up
; english value name from integer referece code
(defn value 
  ([type val] (value "" type (str val)))
  ( [vendor type val] 
    (get-in @dictionaries [:values (attr-key vendor type) val])))
