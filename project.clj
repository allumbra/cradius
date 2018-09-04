(defproject cradius "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                  [org.clojure/clojure "1.8.0"]
                  [udp-wrapper "0.1.1"]
                  [byte-streams "0.2.4"]
                  [byte-transforms "0.1.4"]
                  ^{:voom {:repo "https://github.com/allumbra/octet"}}
                  [funcool/octet "1.1.1"]
                  [org.clojure/core.async "0.4.474"]]
                  ; [clojurewerkz/buffy "1.1.0"]]
                  ; [gloss "0.2.6"]]
  :main cradius.core)
