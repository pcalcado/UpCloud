(ns upcloud.web
  (use ring.adapter.jetty)
  (use ring.middleware.multipart-params)
  (use upcloud.upload)
  (use ring.middleware.multipart-params.temp-file))

(defn return-200 [& _] {:status 200})

(defn filename-for [req] (:query-string req))

(defn temp-file-size [req] (:size ((:params req) "uploaded")))

(defn temp-directory [] (System/getProperty "java.io.tmpdir"))

(defn upload-from-stream [req])

(defn handler-form [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str"<html>
              <head><title>Welcome</title></head>
              <body>
                <form action=\"upload?" (System/currentTimeMillis)  ".mp3\" method=\"post\" enctype=\"multipart/form-data\">
                  <input type=\"file\" name=\"uploaded\">
                  <input type=\"submit\">
                </form>
              </body>
         </html>\n")})

(defn handler-upload [req]
  (let [writer-fn (make-writer-fn (temp-directory) (filename-for req))
        upload! (make-upload-fn writer-fn)
        store-fn (fn [multipart-map] (upload! (:stream multipart-map)))]
    ((wrap-multipart-params return-200 {:store store-fn}) req)))

(defn- handler-for [req]
  (condp = (req :uri)
    "/upload"   (handler-upload req) 
    (handler-form req)))
    
(defn app [req] (handler-for req))
  
(defn start! [port]
  (doto (Thread. #(run-jetty #'app {:port port})) .start))

(defn -main [] (start! (Integer/parseInt (System/getenv "PORT")))) 
