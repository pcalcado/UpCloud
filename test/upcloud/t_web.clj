(ns upcloud.t_web
  (:use (upcloud web upload)
        (midje sweet)
        (ring.middleware multipart-params)))


(facts "about the form handler"
       
       (fact "should be HTML and have status 200"
             (let [response (handler-form {}) ]
               (:status response) => 200
               (:headers response) => {"Content-Type" "text/html"}))
       
       (fact "should have a generated upload-id prefix"
             (let [expected-prefix "666"]
               (.contains (:body (handler-form {})) expected-prefix) => true
               (provided
                (generate-upload-id-prefix) => expected-prefix))))


(facts "about the file to be saved"
       (let [upload-id "101110"
             file-name "03 All Star.mp3"
             file-size 3209595
             req {:remote-addr "0:0:0:0:0:0:0:1%0",
                  :scheme :http,
                  :request-method :post,
                  :query-string upload-id,
                  :content-type "multipart/form-data; boundary=----WebKitFormBoundary2c0oHK4tobReX3Ah",
                  :uri "/upload",
                  :server-name "localhost",               
                  :headers {"user-agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_7) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.122 Safari/534.30",
                            "origin" "http://localhost:8081", "accept-charset" "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
                            "accept" "text/html, application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", "host" "localhost:8081",
                            "referer" " http://localhost:8081/", "content-type" "multipart/form-data; boundary=----WebKitFormBoundary2c0oHK4tobReX3Ah",
                            "cache-control" "max-age=0","accept-encoding" "gzip, deflate,sdch",
                            "content-length" 3209787, "accept-language" "en-US, en;q=0.8", "connection" "keep-alive"},
                  :content-length file-size,
                  :server-port 8081,
                  :character-encoding nil,
                  :body :blablabla}]

         (fact "its name is an mp3 based on upload id"
               (upload-id-for req) => upload-id)

         (fact "the name can only be a 'number.extension'"
               (upload-id-for {:query-string "666.mp3"}) => "666.mp3"
               (upload-id-for {:query-string "../../usr/bin/login"}) => (throws IllegalArgumentException))
         
         (fact "its approximate (file + heaers) size is retrieved from request"
               (approximate-file-size req) => file-size)))

(facts "about the upload handler"
       (fact "it saves the file to the expected directory"
             (let [expected-temp-dir (System/getProperty "java.io.tmpdir")
                   expected-filename "this-should-be-the-filename.mp3"
                   req  {:body (.getBytes "from some random string")}]
               (handler-upload req) => {:status 200}
               (provided
                (upload-id-for req) => expected-filename
                (temp-directory) => expected-temp-dir)))


       (fact "it removes upload from current uploads and informs bad request if there is something wrong"
             (let [expected-filename "666698.mp3"
                   broken-req {:body :something}]
               (handler-upload broken-req) => {:status 400}
               (provided
                (upload-id-for broken-req) => expected-filename
                (wrap-multipart-params irrelevant  irrelevant) => #(throw (RuntimeException. ))                
                (abandon irrelevant expected-filename) => nil))))

(facts "about the progress handler"
       (fact "should report progress for existing upload process"
             (let [upload-id "123123123"
                   req {:query-string upload-id}]
               (handler-status req) => {:status 200 :body "{\"progress\":13}" :headers {"Content-Type" "application/json"}}
               (provided
                (progress-for upload-id) => 13)))
       
       (fact "should return 404 for non existing upload process"
             (let [upload-id "54321"
                   req {:query-string upload-id}]
               (handler-status req) => {:status 404}
               (provided
                (progress-for upload-id) => nil))))

(facts "about the description handler"
       (let [file-name "1233.mp3"
             description-text "\\o/ _o/ _o_ \\o_ |o| <o/ \\o> <o> |o| \\o/"
             req {:params {"remote-file" file-name
                           "description" description-text}}
             response (handler-description req)]

         (fact "should be HTML and have status 200"
               (:status response) => 200
               (:headers response) => {"Content-Type" "text/html"})

         (fact "it had the description text and link to the file"
               (.contains (:body response) "temp/1233.mp3") => true
               (.contains (:body response) description-text) => true)))
