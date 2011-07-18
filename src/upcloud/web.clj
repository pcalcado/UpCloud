
(ns upcloud.web
  (use ring.adapter.jetty)
  (use ring.middleware.multipart-params)
  (use upcloud.upload))

(defn handler-form [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "<html>
              <head><title>Welcome</title></head>
              <body>
                <form action=\"upload\" method=\"post\" enctype=\"multipart/form-data\">
                  <input type=\"file\" name=\"uploaded\">
                  <input type=\"submit\">
                </form>
              </body>
         </html>\n"})

(defn handler-upload [req] :not-implemented)

(defn- handler-for [req]
  (condp = (req :uri)
    "/upload"   (handler-upload req) 
    handler-form))
    
(defn app [req] ((handler-for  req)))
  
(defn start! [port]
  (doto (Thread. #(run-jetty #'app {:port port})) .start))

(defn -main [] (start! (Integer/parseInt (System/getenv "PORT")))) 
