(ns upcloud.t_upload
  (:require [clojure.java.io :as io])
  (:use [midje.sweet])
  (:use [upcloud.upload])
  (:import [java.io ByteArrayInputStream]))

(def a-lot-of-bytes (. (apply str (take 1024 (iterate identity "a"))) getBytes))

(facts "about uploading content"
       
       (fact "should write uploaded file in chunks"
             (let [fake-input (ByteArrayInputStream. a-lot-of-bytes)
                   chunks (ref (seq nil))
                   writer-fn (fn [bytes] (dosync (alter chunks concat (seq bytes))))
                   upload! (make-upload writer-fn)]
                 (upload! fake-input)
                 @chunks) => (seq a-lot-of-bytes)))


(facts "about making a write function"
       (let [temp-dir (System/getProperty "java.io.tmpdir")
             temp-file "test.dat"
             temp-path (str temp-dir temp-file)]
        (fact "should write to directory specified"
              (io/delete-file temp-path true)
              (let [first-bytes (. "These are the first bytes..." getBytes)
                    more-bytes (. "and these are even more bytes!" getBytes)
                    writer-fn (make-writer-fn temp-dir temp-file)]
                (writer-fn first-bytes)
                (writer-fn more-bytes)
                (slurp temp-path) => "These are the first bytes...and these are even more bytes!"))))
