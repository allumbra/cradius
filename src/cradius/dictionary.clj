(ns cradius.dictionary
  (require  
            [clojure.string :as s]
            [clojure.java.io :as io]
            [cradius.util :as u]))

(defn process-value [result [collection name code]]
    (-> result
        (assoc-in [:values collection name] code)
        (assoc-in [:values collection code] name))) ; reverse lookup

(defn process-attribute [result [name code type]]
  (let [value {:name name :code code :type type}]
    (-> result
      (assoc-in [:attributes name] value)
      (assoc-in [:attributes code] value)))) ; reverse lookup

(defn process-vendor-attribute [result [vendor-id name code type]]
  (let [value {:name name :code code :type type}]
    (-> result
      (assoc-in [:vsa vendor-id :attributes name] value)
      (assoc-in [:vsa vendor-id :attributes code] value)))) ; reverse lookup
                              
(defn process-vendor-value [result [vendor-id collection name code]]
    (-> result
      (assoc-in [:vsa vendor-id :values collection name] code)
      (assoc-in [:vsa vendor-id :values collection code] name))) ; reverse lookup
                                      
(defn get-vendor [lines]
  (let [lines-with-begin-vendor (filter (fn [l] (some? (re-find #"^BEGIN-VENDOR" l))) lines)]
    (if (not (empty? lines-with-begin-vendor))
      (let [
            vendor-line (filter (fn [l] (some? (re-find #"^VENDOR" l))) lines)]
        (if (empty? vendor-line)
            nil
            (s/split (first vendor-line) #"\s+")))
      nil)))
    
(defn load-dictionary
  [file-name]
  (let [  lines (-> file-name slurp s/split-lines)  ; todo - use canonical path?
          [vendor vendor-id] (get-vendor lines)]
    (reduce (fn [result line]
              (let [tokens (s/split line #"\s+")]
                  (case (first tokens)
                    "VALUE" (if vendor
                              (process-vendor-value result (into [vendor-id] (rest tokens)))
                              (process-value result (rest tokens)))
                    "ATTRIBUTE" (if vendor
                                    (process-vendor-attribute result (into [vendor-id] (rest tokens)))
                                    (process-attribute result (rest tokens)))
                    "VENDORATTR" (process-vendor-attribute result (rest tokens))
                    result)))
            {} lines)))

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



(defn attribute [k]
  (let [attr (get-in @dictionaries [:attributes k])]
    attr))
    
