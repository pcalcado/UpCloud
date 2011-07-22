(ns upcloud.web
  (:use ring.adapter.jetty)
  (:use ring.middleware.multipart-params)
  (:use ring.middleware.file)
  (:use ring.middleware.file-info)
  (:use upcloud.upload)
  (:use ring.middleware.multipart-params.temp-file))

(defn return-200 [& _] {:status 200})
(defn return-404 [& _] {:status 404})

(defn upload-id-for [req]
  (let [name (:query-string req)]
    (if (re-matches #"\d+\.?\w*" name)
      name
      (throw (IllegalArgumentException. (str "Trying to use [" name "] as temp file name!"))))))

(defn approximate-file-size [req] (:content-length req))

(defn temp-directory [] "./temp/")

(defn handler-form [req]
  (let [upload-id-prefix (System/currentTimeMillis)  ]
   {:status 200
    :headers {"Content-Type" "text/html"}
    :body (str "<html>
               <head>
                <title>Welcome to UpCloud!</title>
                <link rel=\"stylesheet\" type=\"text/css\" href=\"static/upcloud.css\">
                <script src=\"static/jquery-1.6.2.min.js\" type=\"text/javascript\" language=\"javascript\"></script>
                <script src=\"static/upcloud.js\" type=\"text/javascript\" language=\"javascript\"></script>
              </head>
              <body>
                <h1>Upload your awesomeness!</h1>
                <div id=\"uploadPane\">
                  <form id=\"uploadForm\" target=\"upload-frame\" method=\"post\" enctype=\"multipart/form-data\" action=\"#\">
                    <input type=\"hidden\" id=\"uploadIdPrefixField\" name=\"uploadId\" value=\"" upload-id-prefix "\">
                    <input type=\"file\" name=\"uploaded\" id=\"uploadedFileBox\">
                  </form>
                </div>
                <iframe id=\"uploadFrame\" name=\"upload-frame\"></iframe>
                <form>
                  <textarea name=\"description\"></textarea>
                  <input type=\"submit\">
                </form>
                <script  type=\"text/javascript\" language=\"javascript\">$ (document).ready(Ui.loadApp); </script>
              </body>
         </html>\n")}))

(defn handler-status [req]
  (if-let [progress (progress-for (upload-id-for req))]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (str "{\"progress\":" progress "}")}
    {:status 404
     :body "No upload in progress"}))

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
    #"/temp/.*" ((wrap-file-info (wrap-file return-200  ".")) req)
    #"/static/.*" ((wrap-file-info (wrap-file return-200 "./src/")) req)
     (handler-form req)))
    
(defn app [req] (handler-for req))
  
(defn start! [port]
  (doto (Thread. #(run-jetty #'app {:port port})) .start))

(defn -main [] (start! (Integer/parseInt (System/getenv "PORT")))) 
