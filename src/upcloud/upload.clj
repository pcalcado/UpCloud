(ns upcloud.upload
  (:require [clojure.java.io :as io])
  (:import [java.util Arrays])
  (:import [java.io InputStream]))

(declare *writer-fn*)

(defn upload! [#^InputStream input-stream]
  (let [buffer (byte-array 512)]
    (with-open [input input-stream]
      (loop []
        (let [number-of-bytes-read (.read input buffer)]
          (when (pos? number-of-bytes-read)
            (do
              (let [chunk (Arrays/copyOf buffer number-of-bytes-read)]
                (*writer-fn* chunk))
              (recur))))))))

(defn make-writer-fn [target-dir target-file]
  (fn [bytes]
    (with-open [out (io/output-stream (str target-dir target-file) :append true)]
      (.write out bytes))))
