(ns upcloud.t_upload
  (use [midje.sweet])
  (use [upcloud.upload])
  (import [java.io ByteArrayInputStream]))

(def a-lot-of-bytes (. (apply str (take 1024 (iterate identity "a"))) getBytes))

(facts "about uploading content"
       
       (fact "should write uploaded file in chunks"
             (let [fake-input (ByteArrayInputStream. a-lot-of-bytes)
                   chunks (ref (seq nil))]
               (binding [*writer-fn* (fn [bytes] (dosync (alter chunks concat (seq bytes))))]
                 (upload! fake-input (alength a-lot-of-bytes)))
               @chunks => (seq a-lot-of-bytes))))



