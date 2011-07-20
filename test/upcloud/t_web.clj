(ns upcloud.t_web
  (:use [upcloud.web])
  (:use [midje.sweet]))

(facts "about the file to be saved"
       (let [upload-id "this-is-my-random-ish-upload-id"
             file-name "03 All Star.mp3"
             file-size 3209595
             req {:remote-addr "0:0:0:0:0:0:0:1%0",
                  :scheme :http,
                  :request-method :post,
                  :query-string upload-id,
                  :content-type "multipart/form-data; boundary=----WebKitFormBoundary2c0oHK4tobReX3Ah",
                  :uri "/upload",
                  :server-name "localhost",
                  :params {"uploaded" {:filename file-name, :size file-size, :content-type "audio/mp3"}},
                  :headers {"user-agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_7) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.122 Safari/534.30",
                            "origin" "http://localhost:8081", "accept-charset" "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
                            "accept" "text/html, application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", "host" "localhost:8081",
                            "referer" " http://localhost:8081/", "content-type" "multipart/form-data; boundary=----WebKitFormBoundary2c0oHK4tobReX3Ah",
                            "cache-control" "max-age=0","accept-encoding" "gzip, deflate,sdch",
                            "content-length" 3209787, "accept-language" "en-US, en;q=0.8", "connection" "keep-alive"},
                  :content-length 3209787,
                  :server-port 8081,
                  :character-encoding nil,
                  :body :blablabla}]

         (fact "its name is based on upload id"
               (filename-for req) => upload-id)

         (fact "its approximate size is retrieved from request"
               (temp-file-size req) => file-size)))


(facts "about the upload handler"
       (fact "it saves the file to the temp directory"
             (let [expected-temp-dir (System/getProperty "java.io.tmpdir")
                   expected-filename "this-should-be-the-filename.mp3"                   req  {:params {"uploaded" {:filename "file-name.mp3", :size 666, :content-type "audio/mp3"}, "upload-id" "1231"} :body (.getBytes "from some random string")}]
               (handler-upload req) => {:status 200}
               (provided
                (filename-for req) => expected-filename
                (temp-directory) => expected-temp-dir)) )
       
       (fact "it uses the file-id parameter as the filename")
       (fact "it returns a 200 when finished"))


