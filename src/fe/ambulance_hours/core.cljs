(ns ambulance-hours.core
  (:require ["date-fns" :as date-fns]
            ["d3" :as d3]
            [cljs.reader :refer [read-string]]
            [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as dom]
            [reitit.coercion.spec :as rss]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]))

(def state (r/atom {:data []}))

(defn format-date [date]
  ((.-format date-fns) date "yyyy-MM-dd HH:mm"))

(defn save-data [data]
  (.setItem js/localStorage "data" (prn-str data)))

(defn button [{:keys [class on-click secondary disabled active]} children]
  [:button.justify-center.items-center.rounded.px-3.py-2
   {:class [(cond
              active "bg-gray-700 text-white"
              disabled "bg-gray-200 opacity-50 text-gray-400"
              secondary "bg-gray-400"
              :else "bg-orange-400 text-white")
            class]
    :disabled disabled
    :on-click on-click}
   children])

(defn total-hours [data]
  (->> data
       (map :hours)
       flatten
       count))

(defn add-hours [data chiffre]
  (mapv (fn [patient] (if (= (:chiffre patient) chiffre)
                       (update patient :hours (fn [hours] (vec (cons {:date (new js/Date) :id (random-uuid)} hours))))
                      patient))
       data))

(defn patient [{:keys [idx chiffre hours]}]
  (let [hours-count (->> hours (map :hours) count)]
    [:div.flex.px-6.py-6.justify-between
     {:class (when (even? idx) "bg-gray-300")}
     [:a.px-2.justify-center.items-center.rounded-lg.flex
      {:href (when (> hours-count 0) (rfe/href ::details {:chiffre chiffre}))
       :class (if (even? idx) "bg-gray-100" "bg-gray-700")}
      [:<>
       [:span.text-2xl
        {:class (when (odd? idx) "text-white")}
        chiffre]
       (when (> hours-count 0)
         [:span.ml-1 "👁"])]]
     [:div.flex.items-center.pt-3
      [:div
       [:span.text-2xl.px-4
        hours-count]]
      [:div
       [button {:on-click (fn [] (save-data (swap! state update :data #(add-hours % chiffre))))}
        [:span.text-2xl.text-white "+"]]]]]))

(defn new-patient [{:keys [chiffre update-chiffre create-new-patient]}]
  [:div.flex.p-6.justify-between.items-center.bg-gray-700
   [:input.bg-white.text-2xl.rounded.p-2.border-2
    {:auto-focus true
     :class "w-1/2"
     :on-blur #(when (= chiffre "") (update-chiffre nil))
     :placeholder "Chiffre"
     :default-value chiffre :on-change (fn [^js e] (update-chiffre (.-target.value e)))}]
   [:button.bg-orange-500.justify-center.items-center.rounded.p-2
    {:on-click #(create-new-patient chiffre)}
    [:span.text-2xl.text-white
     "Hinzufügen"]]])

(defn add-patient-button [{:keys [add]}]
  [:button.absolute.p-6
   {:on-click add
    :style {:bottom 0 :right 0}}
   [:div.bg-orange-400.rounded-full.w-16.h-16.items-center.justify-center.relative
    [:span.text-white.text-5xl.font-bold.absolute
     {:style {:top "45%" :left "50%" :transform "translate(-50%,-50%)"}}
     "+"]]])

(defn pie-chart [{:keys [data]}]
  (let [size 150
        pie (-> (d3/pie) (.sort nil) (.value (fn [d] (:value d))))
        arc (-> (d3/arc) (.innerRadius (- (/ size 2) 10)) (.outerRadius (/ size 2)))
        color (d3/scaleOrdinal #js ["#D1D5DB" "#FF7F0E"])
        arcs (pie [{ :value (- 600 (total-hours data)) }
                   { :value (total-hours data) }])]
    [:svg
     {:viewBox (str (- (/ size 2)) " " (- (/ size 2)) " " size " " size) :height 180}
     (map
      (fn [pie-arc]
        [:g
         {:key (.-index pie-arc)}
         [:path {:d (arc pie-arc), :fill (color (.-index pie-arc))}]])
      arcs)]))

(defn total [{:keys [data]}]
  [:div.items-center
   [:div.relative.flex.justify-center.py-4
    [pie-chart {:data data}]
    [:span.absolute.text-6xl.font-extrabold.tracking-wide.text-center
     {:style {:top "50%" :left "50%" :transform "translate(-50%,-50%)"} }
     (total-hours data)]]])

(defn header [{:keys [toggle-menu]}]
  [:div.flex.bg-orange-400.w-full.py-3.px-6.items-baseline
   [:button.text-white.text-3xl.mr-1 {:on-click toggle-menu}
    "☰"]
   [:span.text-2xl.text-white "Ambulante Stunden"]])

(defn footer []
  [:div.flex.bg-orange-400.w-full.justify-center.py-2
   [:span.text-white "made with 💖"]])

(defn yearly-stats [data]
  ;; TODO find other solution for this one hour
  (->> #_(update-in (vec data) [0 :hours] conj {:date #inst "2021-05-02T18:13:18.077-00:00"})
       (vec data)
       (mapcat :hours)
       (map #(assoc % :year (js/parseInt ((.-format date-fns) (:date %) "yyyy"))))
       (group-by :year)
       (map (fn [[year hours]]
              [year
               {:hours-count (count hours)
                :quarters (->> hours
                               (map (fn [hour] (assoc hour :quarter ((.-format date-fns) (:date hour) "QQQ"))))
                               (group-by :quarter)
                               (sort-by first)
                               reverse
                               (mapv (fn [[quarter hours]] [quarter {:hours-count (count hours)}])))}]))
       (sort-by first)
       reverse))

(defn weekly-stats [data]
  (->> data
       (mapcat :hours)
       (map :date)
       (group-by (fn [date] [(date-fns/getYear date) (date-fns/getWeek date)]))
       (sort-by first (fn [[y1 w1] [y2 w2]]
                        (cond (< y1 y2) -1
                              (> y1 y2) 1
                              (< w1 w2) -1
                              :else 1)))
       reverse
        (map (fn [[[year week] hours]] {:year year :week week :hours-count (count hours)}))))

(defn prepare-last-backup [last-backup]
  (-> last-backup
      (str/replace #" " "-")
      (str/replace #":" "-")))

(defn backup []
  (let [error (r/atom nil)
        backup-loading (r/atom nil)]
    (fn []
      [:div.bg-white.flex-1.w-full.pt-6.px-6
       [:div.mb-3
        [button {:on-click (fn []
                             (let [last-backup (format-date (new js/Date))]
                               (-> (js/fetch
                                    "/backup"
                                    (clj->js {:method "POST"
                                              :headers {"content-type" "application/edn"}
                                              :body (prn-str
                                                     {:device-name "web"
                                                      :state (assoc @state :last-backup last-backup)})}))
                                   (.then (fn []
                                            (reset! error false)
                                            (save-data (swap! state assoc :last-backup last-backup))))
                                   (.catch (fn [e]
                                             (prn e)
                                             (reset! error true)))))) }
         [:span.text-white.text-lg "Backup erstellen"]]]
       [:span.text-lg.pb-12 "Letztes Backup: " (if @error "Fehlgeschlagen"
                                                   (if (:last-backup @state) (:last-backup @state) "-"))]
       [:div.mb-3
        [button {:on-click (fn []
                             (reset! backup-loading "LOADING")
                             (->(js/fetch
                                 (str "/backup?device-name=web&last-backup="
                                      (prepare-last-backup (:last-backup @state)))
                                 (clj->js {:headers {"content-type" "application/edn"}}))
                                (.then #(.text %))
                                (.then (fn [res]
                                         (->> res
                                              read-string
                                              (reset! state)
                                              save-data)
                                         (reset! backup-loading "Erfolgreich")))
                                (.catch (fn [e]
                                          (prn e)
                                          (reset! backup-loading "Fehlgeschlagen")))))}
         [:span.text-white.text-lg "Backup laden"]]]
       (when @backup-loading
         [:span.text-lg.pb-12 "Letztes Backup: " @backup-loading])])))

(defn stats []
  (let [active (r/atom :quarterly)]
    (fn []
      [:div.bg-white.flex-1.w-full.pt-6.px-6.pb-3.overflow-scroll
       {:style {:height "calc(100% - 100px)"}}
       [:div.mb-4.flex.gap-2
        [button {:active (= @active :quarterly) :on-click #(reset! active :quarterly)}
         "Quartalsweise"]
        [button {:active (= @active :weekly) :on-click #(reset! active :weekly)}
         "Wöchentlich"]]
       (if (= @active :quarterly)
         (map
          (fn [[year {:keys [hours-count quarters]}]]
            [:div {:key year}
             [:div.flex.justify-between.pr-12.mb-2
              [:span.text-xl.mr-6 year]
              [:span.text-xl.font-bold.text-right (str hours-count (if (> hours-count 1) " Stunden" " Stunde"))]]
             (map
              (fn [[quarter {:keys [hours-count]}]]
                [:div.flex.justify-between.pr-12.mb-2.ml-4 {:key quarter}
                 [:span.text-l.mr-7 quarter]
                 [:span.text-l.font-bold.text-right (str hours-count (if (> hours-count 1) " Stunden" " Stunde"))]])
              quarters)])
          (yearly-stats (:data @state)))
         [:ul
          (map
           (fn [{:keys [year week hours-count]}]
             ^{:key [year week]}
             [:li.flex.justify-between.text-lg
              [:div [:span (str year)] [:span (str " KW " week)]]
              [:div [:span.font-bold hours-count] " Stunde(n)"]])
           (weekly-stats (:data @state)))])])))

(defn home []
  (let [new-chiffre (r/atom nil)]
    (fn []
      [:div.flex.flex-col.bg-white.w-full.relative
       {:style {:height "calc(100% - 60px - 40px)"}}
       [total {:data (:data @state)}]
       [:div.overflow-scroll
        {:style {:height "calc(100% - 212px)"}}
        (when @new-chiffre
          [new-patient {:chiffre @new-chiffre
                        :update-chiffre #(reset! new-chiffre %)
                        :create-new-patient (fn [chiffre]
                                              (save-data (swap! state update :data (fn [data]
                                                                                     (vec
                                                                                      (reverse
                                                                                       (conj data {:chiffre chiffre :hours []}))))))
                                              (reset! new-chiffre nil))}])
        (map-indexed
         (fn [idx {:keys [chiffre hours]}]
           [patient {:key idx :idx idx :chiffre chiffre :hours hours}])
         (:data @state))]
       [add-patient-button {:add #(reset! new-chiffre "")}]])))

(defn details-date [{:keys [date]}]
  (let [updated-date (r/atom date)]
    (fn [{:keys [hour on-remove on-save date]}]
      [:div.flex.px-6.py-4 {:class (when (odd? hour) " bg-gray-300")}
       [:div.flex.flex-col {:class "w-2/3"}
        [:span.text-lg.mb-1.font-bold (str "Stunde " hour)]
        [:input.p-2.rounded
         {:style {:width "90%"}
          :type "datetime-local"
          :value (date-fns/format @updated-date "yyyy-MM-dd'T'HH:mm")
          :on-change (fn [^js e]
                       (let [new-date (date-fns/parse (.-target.value e) "yyyy-MM-dd'T'HH:mm" (new js/Date))]
                         (when (date-fns/isValid new-date)
                           (reset! updated-date new-date))))}]]
       [:div.flex.flex-col.text-white {:class "w-1/3"}
        [button {:on-click #(on-save @updated-date) :class "mb-2" :disabled (= date @updated-date)}
         "Speichern"]
        [button {:on-click on-remove :secondary true}
         "Löschen"]]])))

(defn get-details-data [chiffre]
  (some #(when (= (:chiffre %) chiffre) %) (:data @state)))

(defn get-chiffre-from-props [props]
  (-> props :parameters :path :chiffre))

(defn update-details-data [new-details-data chiffre]
  (-> (swap! state update :data (fn [data] (mapv #(if (= chiffre (:chiffre %)) new-details-data %) data)))
      save-data))

(defn update-chiffre [old-chiffre new-chiffre]
  (-> (swap! state update :data
             (fn [data]
               (mapv #(if (= old-chiffre (:chiffre %))
                        (assoc % :chiffre new-chiffre)
                        %)
                     data)))
      save-data)
  (.back js/history)
  (js/setTimeout
   #(set! (.-href js/location) (rfe/href ::details {:chiffre new-chiffre}))
   1))

(defn details [props]
  (let [details-data (get-details-data (get-chiffre-from-props props))
        {:keys [chiffre]} details-data
        updated-chiffre (r/atom chiffre)]
    (fn [props]
      (let [details-data (get-details-data (get-chiffre-from-props props))
            {:keys [chiffre hours]} details-data]
        [:div
         {:style {:height "calc(100% - 60px - 40px)"}}
         [:div.mt-6.px-6.mb-4.flex
          [:input.p-2.text-3xl.mr-2
           {:value @updated-chiffre :style {:width "90%"}
            :on-change (fn [^js e] (reset! updated-chiffre (.-target.value e)))}]
          [button {:disabled (= chiffre @updated-chiffre)
                   :on-click #(update-chiffre chiffre @updated-chiffre)}
           "Speichern"]]
         [:div.overflow-scroll
          {:style {:height "calc(100% - 76px)"}}
          (->> hours
               (sort-by :date >)
               (map-indexed
                (fn [idx {:keys [date id]}]
                  ^{:key id}
                  [:<>
                   [details-date {:hour (- (count hours) idx) :date date
                                  :on-remove #(update-details-data (update details-data :hours (fn [hours] (remove (fn [h] (= id (:id h))) hours)))
                                                                   chiffre)
                                  :on-save (fn [new-date]
                                             (update-details-data
                                              (update details-data :hours
                                                      (fn [hours]
                                                        (map (fn [h] (if (= id (:id h))
                                                                      (assoc h :date new-date)
                                                                      h))
                                                             hours)))
                                              chiffre))}]])))]]))))

(defonce match (r/atom nil))

(defn nav-link [{:keys [active? href on-click]} children]
  [:li {:class [(when active?
                  "bg-orange-100 text-orange-900 rounded-lg")]}
   [:a.inline-block.px-3.py-2.w-full
    {:href href :on-click on-click} children]])

(def routes
  (r/atom
   [["/" {:name ::main :view home :title "Home" :nav true}]
    ["/details/:chiffre"
     {:name ::details :view details :parameters {:path {:chiffre string?}}}]
    ["/stats" {:name ::stats :view stats :title "Statistiken" :nav true}]]))

(defn menu [{:keys [visible? toggle-menu]}]
  [:div.absolute.left-0.top-0.bg-gray-100.z-10
   {:class "w-2/3"
    :style {:margin-top 60 :height "calc(100% - 60px - 40px)"
            :left (if visible? "0%" "-100%")
            :transition "all 300ms"}}
   [:nav.px-6.py-4.text-gray-700
    (let [active-page (:name (:data @match))]
      [:ul
       (->> @routes
            (filter (fn [[_ {:keys [nav]}]] nav))
            (map
             (fn [[_ {:keys [name title]}]]
               ^{:key name}
               [nav-link {:active? (= active-page name) :href (rfe/href name) :on-click toggle-menu}
                title])))])]])

(defn root []
  (when-let [data (.getItem js/localStorage "data")]
    (reset! state (read-string data)))
  (let [menu-visible? (r/atom false)]
    (fn []
      (let [toggle-menu #(swap! menu-visible? not)]
        [:div.flex.flex-col.bg-gray-100.h-full.relative
         [header {:toggle-menu toggle-menu}]
         [menu {:visible? @menu-visible? :toggle-menu toggle-menu}]
         (when @match
           (let [view (:view (:data @match))]
             [view @match]))
         [footer]]))))

(defn register-service-worker []
  (when (.-serviceWorker js/navigator)
    (try
      (.register js/navigator.serviceWorker "/service-worker.js")
      (catch :default e
        (js/console.error "Registration failed with" e)))))

(defn init []
  (-> (js/fetch "/backup"  (clj->js {:headers {"content-type" "application/edn"}}))
      (.then (fn [r]
               (rfe/start!
                (rf/router (if (not= (.-status r) 404)
                             (swap! routes conj ["/backup" {:name ::backup :view backup :nav true :title "Backup"}])
                             @routes)
                           {:data {:coercion rss/coercion}})
                (fn [m] (reset! match m))
                {:use-framgent true})
               (dom/render [root] (.getElementById js/document "app"))
               #_(register-service-worker)))))

(defn ^:dev/after-load clear-cache-and-render!
  []
  (init))
