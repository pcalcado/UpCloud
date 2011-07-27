(ns upcloud.upload
  (:require [clojure.java.io :as io])
  (:import [java.util Arrays]
           [java.io InputStream]))

(defn log [& msg] (println "LOG [" (System/currentTimeMillis) "]" msg))

(def *current-uploads* (atom {}))

(defn notify-progress-for [upload-id bytes-read total-size]
  (swap! *current-uploads* assoc upload-id {:read bytes-read :total total-size}))

(defn progress-for [upload-id]
  (let [upload-status (@*current-uploads* upload-id)
        so-far (:read upload-status)
        total (:total upload-status)]
    (if upload-status
      (int (/ so-far (/ total 100)))
      nil)))

(defn abandon [file-dir upload-id]
  (io/delete-file (str file-dir upload-id) true)
  (swap! *current-uploads* dissoc upload-id)
  (log "Abandoned " upload-id))

(defn make-upload-fn [upload-id writer-fn]
  (fn [#^InputStream input-stream]
    (let [buffer-size 512
          buffer (byte-array buffer-size)]
      (log "Started reading " upload-id)
      (with-open [input input-stream]
        (loop []
          (let [number-of-bytes-read (.read input buffer)]
            (when (pos? number-of-bytes-read)
              (do
                (let [chunk (Arrays/copyOf buffer number-of-bytes-read)]
                  (writer-fn chunk))
                (recur))))))
      (writer-fn)
      (log "Finished reading " upload-id))))

(defn make-writer-fn [target-dir upload-id notifier-fn file-size]
  (let [writer-agent (agent 0)]
    (fn
      ([] (send-off writer-agent (fn [_]
                               (notifier-fn upload-id file-size file-size)
                               (log "Finished writing " upload-id)))
         writer-agent)
      ([bytes]
         (send-off writer-agent (fn [total-bytes-read-so-far]
                                  (with-open [out (io/output-stream (str target-dir upload-id) :append true)]
                                    (.write out bytes))
                                  (let [total-bytes-in-this-iteration (+ total-bytes-read-so-far (alength bytes))]
                                    (notifier-fn upload-id total-bytes-in-this-iteration file-size)
                                    total-bytes-in-this-iteration)))         
         writer-agent))))
