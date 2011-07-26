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
                   writer-fn (fn
                               ([bytes] (dosync (alter chunks concat (seq bytes))))
                               ([] nil))
                   upload! (make-upload-fn upload-id writer-fn)]
               (upload! fake-input)
               @chunks) => (seq a-lot-of-bytes)))

(facts "about the write function"
       (let [temp-dir (System/getProperty "java.io.tmpdir")
             upload-id "test.dat"
             temp-path (str temp-dir upload-id)]

         (fact "should write to directory specified"
               (let [notifier-fn (fn [& _])
                     file-size 10]

                 (io/delete-file temp-path true)

                 (let [first-bytes (. "These are the first bytes..." getBytes)
                       more-bytes (. "and these are even more bytes!" getBytes)
                       writer-fn (make-writer-fn temp-dir upload-id notifier-fn file-size)]
                   (writer-fn first-bytes)
                   (writer-fn more-bytes)
                   (slurp temp-path) => "These are the first bytes...and these are even more bytes!")))

         (fact "should notify about progress every time it writes a chunk"
               (let [first-chunk (byte-array 1000)
                     second-chunk (byte-array 20)
                     third-chunk (byte-array 4)
                     file-size 1024
                     headers-size 100
                     file-plus-headers-size (+ file-size headers-size)
                     notifications (ref [])
                     notifier-fn (fn [upload-id current total] (dosync (alter notifications conj [upload-id current total])))]
                 
                 (io/delete-file temp-path true)
                 
                 (let [writer-fn (make-writer-fn temp-dir upload-id notifier-fn file-plus-headers-size)]
                   (writer-fn first-chunk)
                   (writer-fn second-chunk)
                   (writer-fn third-chunk)

                   @notifications => [[upload-id 1000 1124]
                                      [upload-id 1020 1124]
                                      [upload-id 1024 1124]])))

         (fact "should notify about completion once no data is passed to it"
               (let [file-plus-headers-size 1025
                     notifications (ref [])
                     notifier-fn (fn [upload-id current total] (dosync (alter notifications conj [upload-id current total])))]
               
                 (io/delete-file temp-path true)
               
                 (let [writer-fn (make-writer-fn temp-dir upload-id notifier-fn file-plus-headers-size)]
                   (writer-fn)

                   @notifications => [[upload-id 1025 1025]])))))

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
