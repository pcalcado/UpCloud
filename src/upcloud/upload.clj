(ns upcloud.upload
  (:require [clojure.java.io :as io])
  (:import [java.util Arrays]
           [java.io InputStream]))

(defn log [& msg] (println "LOG [" (System/currentTimeMillis) "]" msg))

(def *current-uploads* (ref {}))

(defn notify-progress-for [upload-id bytes-read total-size]
  (dosync (alter *current-uploads* assoc upload-id {:read bytes-read :total total-size})))

(defn progress-for [upload-id]
  (let [upload-status (@*current-uploads* upload-id)
        so-far (:read upload-status)
        total (:total upload-status)]
    (if upload-status
      (int (/ so-far (/ total 100)))
      nil)))

(defn abandon [file-dir upload-id]
  (io/delete-file (str file-dir upload-id) true)
  (dosync (alter *current-uploads* dissoc upload-id)))

(defn make-upload-fn [upload-id writer-fn notifier-fn file-size]
  (fn [#^InputStream input-stream]
    (let [buffer-size 512
          buffer (byte-array buffer-size)]
      (with-open [input input-stream]
        (loop [total-bytes-read-so-far 0]
          (let [number-of-bytes-read (.read input buffer)
                total-bytes-read-in-this-iteration (+ total-bytes-read-so-far number-of-bytes-read)]
            (when (pos? number-of-bytes-read)
              (do
                (let [chunk (Arrays/copyOf buffer number-of-bytes-read)]
                  (writer-fn chunk))                
                (notifier-fn upload-id total-bytes-read-in-this-iteration file-size)
                (recur total-bytes-read-in-this-iteration)))))))))

(defn make-writer-fn [target-dir upload-id]
  (fn [bytes]
    (with-open [out (io/output-stream (str target-dir upload-id) :append true)]
      (.write out bytes))))
