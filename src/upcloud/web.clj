(ns upcloud.web
  (:use (ring.adapter jetty)
        (ring.middleware multipart-params file file-info params)
        (upcloud upload)))

(defn return-200 [& _] {:status 200})
(defn return-400 [& _] {:status 400})
(defn return-404 [& _] {:status 404})

(defn generate-upload-id-prefix [] (System/currentTimeMillis))

(defn upload-id-for [req]
  (let [name (:query-string req)]
    (if (re-matches #"\d+\.?\w*" name)
      name
      (throw (IllegalArgumentException. (str "Trying to use [" name "] as temp file name!"))))))

(defn approximate-file-size [req] (:content-length req))

(defn temp-directory [] "./temp/")

(defn link-to-temp-file [file] (str "temp/" file))

(defn handler-form [req]
  (let [upload-id-prefix (generate-upload-id-prefix)]
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

                <form action=\"description\" method=\"post\">
                  <input type=\"hidden\" id=\"remoteFileNameField\" name=\"remote-file\" value=\"\">
                  <textarea name=\"description\"></textarea>
                  <input id=\"submitDescriptionButton\" type=\"submit\" value=\"Save\">
                </form>

                <script  type=\"text/javascript\" language=\"javascript\">$ (document).ready(Ui.loadApp); </script>
              </body>
         </html>\n")}))

(defn handler-status [req]
  (if-let [progress (progress-for (upload-id-for req))]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (str "{\"progress\":" progress "}")}
    (return-404)))

(defn handler-upload [req]
  (let [upload-id (upload-id-for req)]
   (try
     (let [writer-fn (make-writer-fn (temp-directory) upload-id)
           notifier-fn notify-progress-for
           upload! (make-upload-fn upload-id
                                   writer-fn
                                   notifier-fn
                                   (approximate-file-size req))
           store-fn (fn [multipart-map] (upload! (:stream multipart-map)))]
       ((wrap-multipart-params return-200 {:store store-fn}) req))
     (catch Exception _
       (abandon upload-id)
       (return-400)))))


(defn handler-description [req]
  (let [params (:params req)
        description (params "description")
        link-to-file (link-to-temp-file (params "remote-file"))]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (str "<html>
               <head>
                <title>Welcome to UpCloud!</title>
              </head>
              <body>
                <h1>Yay!</h1>
                <div>Your file <a href=\"" link-to-file "\"> is here</a>!</div>
               <div>Description: " description  "</div>
              </body>
         </html>\n")}))

(defn- handler-for [uri]
  (condp re-matches uri
    #"/upload" handler-upload
    #"/status" handler-status
    #"/description" handler-description
    #"/temp/.*" (wrap-file-info (wrap-file return-200  "."))
    #"/static/.*" (wrap-file-info (wrap-file return-200 "./src/")) 
    handler-form))

(defn app [req] ((wrap-params (handler-for (:uri req))) req))

(defn start! [port]
  (doto (Thread. #(run-jetty #'app {:port port})) .start))

(defn -main [] (start! (Integer/parseInt (System/getenv "PORT")))) 
