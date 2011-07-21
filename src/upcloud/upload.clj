(ns upcloud.upload
  (:require [clojure.java.io :as io])
  (:import [java.util Arrays])
  (:import [java.io InputStream]))

(defn make-upload-fn [upload-id writer-fn notifier-fn file-size]
  (fn [#^InputStream input-stream]
    (let [buffer-size 512
          buffer (byte-array buffer-size)]
      (with-open [input input-stream]
        (loop [counter 0]
          (let [number-of-bytes-read (.read input buffer)]
            (when (pos? number-of-bytes-read)
              (do
                (let [chunk (Arrays/copyOf buffer number-of-bytes-read)
                      position-in-array (* counter buffer-size)]
                  (writer-fn chunk)
                  (notifier-fn upload-id position-in-array file-size)
                  (recur (inc counter))))))))
      (notifier-fn upload-id file-size file-size))))

(defn make-writer-fn [target-dir target-file]
  (fn [bytes]
    (with-open [out (io/output-stream (str target-dir target-file) :append true)]
      (.write out bytes))))

