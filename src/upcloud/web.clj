(ns upcloud.web
  (use ring.adapter.jetty)
  (use ring.middleware.multipart-params)
  (use ring.middleware.file)
  (use upcloud.upload)
  (use ring.middleware.multipart-params.temp-file))

(defn return-200 [& _] {:status 200})
(defn return-200 [& _] {:status 404})

(defn upload-id-for [req]
  (str (Integer/parseInt (:query-string req))))

(defn approximate-file-size [req] (:content-length req))

(defn temp-directory [] (System/getProperty "java.io.tmpdir"))

(defn upload-from-stream [req])

(defn handler-form [req]
  (let [upload-id (System/currentTimeMillis)  ]
   {:status 200
    :headers {"Content-Type" "text/html"}
    :body (str"<html>
              <head>
                <title>Welcome to UpCloud!</title>
                <script src=\"jquery.js\"></script>
                <script src=\"upcloud.js\"></script>
              </head>
              <body>
                <h1>Upload your awesomeness!</h1>
                <form action=\"upload?" upload-id "\" method=\"post\" enctype=\"multipart/form-data\">
                  <input type=\"file\" name=\"uploaded\">
                  <input type=\"submit\">
                </form>
                <div id=\"statusPane\"></div>
                <form action=\"description?" upload-id "\">
                  <textarea name=\"description\"></textarea>
                  <input type=\"submit\">         
                </form>
              </body>
         </html>\n")}))

(defn handler-status [req]
  (if-let [progress (progress-for (upload-id-for req))]
    {:status 200 :body (str "{progress:'" progress "'}")}
    {:status 404 :body "No upload in progress"}))

(defn handler-upload [req]
  (let [writer-fn (make-writer-fn (temp-directory) (upload-id-for req))
        notifier-fn notify-progress-for
        upload! (make-upload-fn (upload-id-for req) writer-fn notifier-fn (approximate-file-size req))
        store-fn (fn [multipart-map] (upload! (:stream multipart-map)))]
    ((wrap-multipart-params return-200 {:store store-fn}) req)))

(defn- handler-for [req]
  (condp re-matches (req :uri)
    #"/upload" (handler-upload req)
    #"/status" (handler-status req)
    #"/static/.*" ((wrap-file return-200 "./src/") req)
     (handler-form req)))
    
(defn app [req] (handler-for req))
  
(defn start! [port]
  (doto (Thread. #(run-jetty #'app {:port port})) .start))

(defn -main [] (start! (Integer/parseInt (System/getenv "PORT")))) 
