(ns upcloud.web
  (:use (ring.adapter jetty)
        (ring.middleware multipart-params file file-info)
        (upcloud upload)))

(defn return-200 [& _] {:status 200})
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
    {:status 404
     :body "No upload in progress"}))

(defn handler-upload [req]
  (let [writer-fn (make-writer-fn (temp-directory) (upload-id-for req))
        notifier-fn notify-progress-for
        upload! (make-upload-fn (upload-id-for req) writer-fn notifier-fn (approximate-file-size req))
        store-fn (fn [multipart-map] (upload! (:stream multipart-map)))]
    ((wrap-multipart-params return-200 {:store store-fn}) req)))

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
                <div>Your track is here: <a href=\"" link-to-file "\">" link-to-file  "</a></div>
               <div>Description: " description  "</div>
              </body>
         </html>\n")}))

(defn- handler-for [req]
  (condp re-matches (req :uri)
    #"/upload" (handler-upload req)
    #"/status" (handler-status req)
    #"/description" (handler-description req)
    #"/temp/.*" ((wrap-file-info (wrap-file return-200  ".")) req)
    #"/static/.*" ((wrap-file-info (wrap-file return-200 "./src/")) req)
    (handler-form req)))

(defn app [req] (handler-for req))

(defn start! [port]
  (doto (Thread. #(run-jetty #'app {:port port})) .start))

(defn -main [] (start! (Integer/parseInt (System/getenv "PORT")))) 
