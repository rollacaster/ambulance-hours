(ns ambulance-hours.migrate)

(defn migrate [path]
  (update (read-string (slurp path))
          :data
          (fn [data]
            (mapv
             (fn [patient]
               (update patient :hours
                       (fn [hours]
                         (mapv
                          (fn [hour] (assoc hour :id (str (random-uuid))))
                          hours))))
             data))))
(comment
  (spit
   "backup/ambulance-hours-data-web-2022-05-25-22-14.edn"
   (prn-str (migrate "backup/ambulance-hours-data-web-2022-05-25-22-14.edn"))))
