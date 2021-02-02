(ns ambulance-hours.server
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [compojure.core :refer [defroutes POST]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.multipart-params :as mp]))

(defonce server (atom nil))

(defn write-edn [path data]
  (let [resources-path (str "backup/" path)]
    (binding [*print-level* nil
              *print-length* nil]
      (io/make-parents resources-path)
      (pprint (vec (edn/read-string data)) (io/writer resources-path)))))

(defn store-backup
  [file device-name]
  (write-edn (str "ambulance-hours-data-"
                  (str/replace (str/lower-case device-name) #"[\s\W]" "")
                  "-"
                  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd-HH-mm") (new java.util.Date))
                  ".edn")
             (slurp (file :tempfile)))
  {:status 200})

(defroutes public-routes
  (mp/wrap-multipart-params
   (POST "/backup" {params :params}
         (store-backup (get params "data")
                       (get params "device-name")))))

(comment
  (do
    (.stop @server)
    (reset! server nil)))

(when (not @server)
  (reset! server (run-jetty public-routes {:port 8000 :join? false})))
