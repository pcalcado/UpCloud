(ns upcloud.upload
  (import [java.util Arrays])
  (import [java.io InputStream]))

(declare *writer-fn*)

(defn upload! [#^InputStream input-stream #^int total-size]
  (let [buffer (byte-array 512)]
    (with-open [input input-stream]
      (loop []
        (let [number-of-bytes-read (.read input buffer)]
          (when (pos? number-of-bytes-read)
            (do
              (let [chunk (Arrays/copyOf buffer number-of-bytes-read)]
                (*writer-fn* chunk))
              (recur))))))))
