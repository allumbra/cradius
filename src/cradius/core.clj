(ns cradius.core
  (require  [udp-wrapper.core :as udp]
            [clojure.string :as s]
            [clojure.core.async
              :as a
              :refer [>! <! >!! <!! go chan buffer close! thread
                      alts! alts!! timeout]])) 
(def auth-in-c (chan))
(def acct-in-c (chan))
; (def auth-socket (udp/create-udp-server 1812))
; (defn auth-handler [pkt] (println pkt)) ; want to put this on a channel
; (def auth-server (udp/receive-loop auth-socket (udp/empty-packet 4096) auth-handler))

;;;;;;;;;;;;;;;;;;;;;
; dictionary processing

; (defn load-dictionary
;   [content]
;   (let [  lines ()]))



; (defn radius-processor []
;   (let [in (chan)
;         out (chan)]
;       (go (<! in)
;           ()
;           (>! out))))


; (defn send-radius [payload address port]
;   (let [packet (udp/packet (udp/get-bytes-utf8 payload) (udp/make-address address) port)]
;     (udp/send-message auth-socket  packet)))

(defn -main [& args]
  (println "start"))
  ; (send-radius  "You might receive this." "localhost" 1812)
  ; (println "message sent")
  ; (Thread/sleep 1000))
  ; (udp/close-udp-server auth-socket))

  
;; Data flow: 1. in -> parse -> normalize -> desecret -> do work (auth, accounting etc)
