(ns ambulance-hours.server
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [compojure.core :refer [defroutes GET POST]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]))

(defonce server (atom nil))

(defn write-edn [path data]
  (let [resources-path (str "backup/" path)]
    (binding [*print-level* nil
              *print-length* nil]
      (io/make-parents resources-path)
      (pprint data (io/writer resources-path)))))

(defn backup-file-name [device-name last-backup]
  (str "ambulance-hours-data-" device-name "-" last-backup ".edn"))

(defn store-backup
  [device-name state]
  (write-edn (backup-file-name device-name (.format (java.text.SimpleDateFormat. "yyyy-MM-dd-HH-mm") (new java.util.Date)))
             state)
  {:status 200})

(defroutes public-routes
  (POST "/backup" [device-name state]
        (prn device-name state)
        (store-backup device-name state)
        {:status 200})
  (GET "/backup" [device-name last-backup]
       {:status 200
        :headers {"Content-Type" "application/edn"}
        :body (slurp (str "backup/" (backup-file-name device-name last-backup)))}))

#_(do
  (.stop @server)
  (reset! server nil))

(when (not @server)
  (reset! server (run-jetty (wrap-edn-params
                             (wrap-defaults
                              public-routes
                              api-defaults))
                            {:port 8000 :join? false})))
