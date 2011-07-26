(ns upcloud.t_upload
  (:require [clojure.java.io :as io])
  (:use (midje sweet)
        (upcloud upload))
  (:import [java.io ByteArrayInputStream]))

(def a-lot-of-bytes (. (apply str (take 2049 (iterate identity "a"))) getBytes))

(facts "about uploading content"
       
       (fact "should write uploaded file in chunks"
             (let [upload-id "564435.doc"
                   fake-input (ByteArrayInputStream. a-lot-of-bytes)
                   chunks (ref (seq nil))
                   writer-fn (fn [bytes] (dosync (alter chunks concat (seq bytes))))
                   notifier-fn (fn [& _])
                   file-size (alength a-lot-of-bytes)
                   upload! (make-upload-fn upload-id writer-fn notifier-fn file-size)]
               (upload! fake-input)
               @chunks) => (seq a-lot-of-bytes))
       
       (fact "should notify about progress every time it writes a chunk"
             (let [upload-id "884333.someextension"
                   fake-input (ByteArrayInputStream. a-lot-of-bytes)
                   writer-fn identity
                   notifications (ref [])
                   notifier-fn (fn [upload-id current total] (dosync (alter notifications conj [upload-id current total])))
                   file-size (alength a-lot-of-bytes)
                   upload! (make-upload-fn upload-id writer-fn notifier-fn file-size)]
               (upload! fake-input)
               @notifications => [[upload-id 512 2049]
                                  [upload-id 1024 2049]
                                  [upload-id 1536 2049]
                                  [upload-id 2048 2049]
                                  [upload-id 2049 2049]])))

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

(facts "about upload progress notification"
       
       (fact "asking for progress for existing upload-id returns the % complete"
             (let [upload-id-1 "661"
                   upload-id-2 "662"
                   upload-id-3 "663"
                   upload-id-4 "664"]
               (notify-progress-for upload-id-1 0 1024)
               (notify-progress-for upload-id-2 12 1024)
               (notify-progress-for upload-id-3 103 1024)
               (notify-progress-for upload-id-4 1024 1024)

               (progress-for upload-id-1) => 0
               (progress-for upload-id-2) => 1
               (progress-for upload-id-3) => 10
               (progress-for upload-id-4) => 100))

       (fact "asking for progress for inexisting upload-id returns nil"
             (progress-for "some-random-upload-id") => nil))

(facts "about abandoning uploads"
       (fact "should have removed existing upload from current map"
             (let [temp-dir (System/getProperty "java.io.tmpdir")
                   upload-id "1234567890.mkv"]
               (notify-progress-for upload-id 666 1000)
               (abandon temp-dir upload-id)
               (progress-for upload-id) => nil))

       (fact "should have removed the file from temp directory"
             (let [temp-dir "/tmp/"
                   upload-id "123.mkv"]
               (abandon temp-dir upload-id) => irrelevant
               (provided
                (io/delete-file "/tmp/123.mkv" true) => nil))))
