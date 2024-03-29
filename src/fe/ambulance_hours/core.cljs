(ns ambulance-hours.core
  (:require ["canvas-confetti" :as confetti]
            ["d3" :as d3]
            ["date-fns" :as date-fns]
            [cljs.reader :refer [read-string]]
            [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as dom]
            [reitit.coercion.spec :as rss]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]))

(defonce state (r/atom {:data []}))
(defonce finish (r/atom false))

(defn format-date [date]
  ((.-format date-fns) date "yyyy-MM-dd HH:mm"))

(defn parse-date [date-str]
  (date-fns/parse date-str "yyyy-MM-dd HH:mm" (new js/Date)))

(defn save-data [data]
  (.setItem js/localStorage "data" (prn-str data)))

(defn button [{:keys [class on-click secondary disabled active type]} children]
  [:button.justify-center.items-center.rounded.px-3.py-2
   {:class [(cond
              active "bg-gray-700 text-white"
              disabled "bg-gray-200 opacity-50 text-gray-400"
              secondary "bg-gray-400"
              :else "bg-orange-400 text-white")
            class]
    :disabled disabled
    :on-click on-click
    :type (or type "button")}
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

(add-watch state :finish (fn [_ _ old new]
                             (let [old-hour-count (count (mapcat :hours (get-in old [:data])))
                                   new-hour-count (count (mapcat :hours (get-in new [:data])))]
                               (when (and (= old-hour-count 599) (= new-hour-count 600))
                                 (reset! finish true)))))
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
  [:button {:on-click add}
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
  [:div.flex.bg-orange-400.w-full.items-baseline
   [:button.text-white.text-3xl.pr-2.pl-6.py-3 {:on-click toggle-menu}
    "☰"]
   [:span.text-2xl.text-white.py-3
    "Ambulante Stunden"]])

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
       (group-by (fn [date] [(date-fns/getYear date)
                            (date-fns/getWeek date
                                              #js {:weekStartsOn 1
                                                   :firstWeekContainsDate 4})]))
       (sort-by first (fn [[y1 w1] [y2 w2]]
                        (cond (< y1 y2) -1
                              (> y1 y2) 1
                              (< w1 w2) -1
                              :else 1)))
       reverse
        (map (fn [[[year week] hours]] {:year year :week week :hours-count (count hours)}))))

(defn backup-csv [data]
  (str/join "\n"
            (let [longest-hours
                  (->> data
                       (map (fn [{:keys [hours]}] (count (map :date hours))))
                       (apply max))]
              (->> data
                   (map (fn [{:keys [chiffre hours]}]
                          (let [dates (->> hours
                                           (sort-by :date <)
                                           (map (comp format-date :date)))
                                filled-dates (map-indexed
                                              (fn [idx e]
                                                (if (< idx (count dates)) (nth dates idx) e))
                                              (repeat longest-hours ""))]
                            (conj filled-dates chiffre))))
                   (apply map vector)
                   (map-indexed (fn [idx row] (if (= idx 0)
                                               (cons "Chiffre" row)
                                               (cons (str "Stunde " idx) row))))
                   (map (fn [row] (str/join "," row)))))))

(defn download-csv [csv-content file-name]
  (let [encoded-uri (js/encodeURI (str "data:text/csv;charset=utf-8," csv-content))
        link (js/document.createElement "a")]
    (.setAttribute link "href" encoded-uri)
    (.setAttribute link "download" file-name)
    (.appendChild js/document.body link)
    (.click link)
    (.remove link)))

(s/def ::date (s/and inst? date-fns/isValid))
(s/def ::id uuid?)
(s/def ::hour (s/keys :req-un [::date ::id]))
(s/def ::hours (s/coll-of ::hour))
(s/def ::chiffre string?)
(s/def ::patient (s/keys :req-un [::chiffre ::hours]))
(s/def ::data (s/coll-of ::patient))

(defn parse-csv [csv-string]
  (->> csv-string
       str/split-lines
       (map (fn [line]
              (cond
                (str/includes? line ",") (str/split line #"," {:limit -1})
                (str/includes? line ";") (str/split line #";" {:limit -1})
                :else (throw (ex-message "Invalid data format.")))))
       (apply map vector)
       rest
       (keep (fn [[chiffre & dates]]
               (when (seq chiffre)
                 [chiffre (remove (fn [date] (empty? date)) dates)])))
       (mapv (fn [[chiffre dates]]
               {:chiffre chiffre
                :hours (mapv #(hash-map
                               :id (random-uuid)
                               :date (parse-date %))
                             dates)}))))

(defn backup []
  (let [new-state! (r/atom nil)
        error! (r/atom nil)]
    (fn []
      [:div.bg-white.flex-1.w-full.pt-6.px-6
       [:div.mb-6
        [:h2.text-md.font-bold.mb-3 "Export"]
        [:div.mb-3
         [button {:on-click (fn []
                              (let [last-backup (format-date (new js/Date))
                                    backup-name (str "ambulante-stunden-backup-" last-backup ".csv")]
                                (-> (:data @state)
                                    backup-csv
                                    (download-csv backup-name))
                                (save-data (swap! state assoc :last-backup last-backup)))) }
          [:span.text-white.text-lg "Backup erstellen"]]]
        [:span.text-lg.pb-12 "Letztes Backup: " (if (:last-backup @state) (:last-backup @state) "-")]]
       [:div.mb-6
        [:h2.text-md.font-bold.mb-3 "Import"]
        [:style {:dangerouslySetInnerHTML {:__html
                                           ".custom-file-input::-webkit-file-upload-button {
                                              display: none;
                                            }
                                            .custom-file-input::before {
                                              content: 'Datei auswählen';
                                              display: inline-block;
                                              background: rgb(156 163 175/var(--tw-bg-opacity));;
                                              border-radius: 4px;
                                              padding: 8px 12px;
                                              outline: none;
                                              white-space: nowrap;
                                              margin-right: 6px;
                                              -webkit-user-select: none;
                                              cursor: pointer;
                                              font-size: 16px;
                                            }"}}]
        [:form {:on-submit (fn [^js e]
                             (.preventDefault e)
                             (-> state
                                 (swap! assoc :data @new-state!)
                                 save-data)
                             (reset! new-state! nil))}
         [:div.mb-3
          [:input {:type "file" :name "backup-file" :class "custom-file-input"
                   :on-change (fn [^js e]
                                (when-let [file (first (.-target.files e))]
                                  (let [reader (new js/FileReader)]
                                    (.readAsText reader file "UTF-8")
                                    (set! (.-onload reader)
                                          (fn [^js e]
                                            (let [csv-string (.-target.result e)
                                                  new-data (parse-csv csv-string)]
                                              (if (s/valid? ::data new-data)
                                                (do
                                                  (reset! error! nil)
                                                  (reset! new-state! new-data))
                                                (do
                                                  (js/console.error (s/explain-str ::data new-data))
                                                  (reset! error! "Datei konnte nicht gelesen werden")))))))))}]]
         [button {:type "submit" :disabled (nil? @new-state!)}
          [:span.text-white.text-lg "Backup laden"]]
         (when @error!
           [:div.text-red-700.mt-4 @error!])]]])))

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
       (when @finish
         [:canvas.absolute.top-0.left-0.w-full
          {:style {:height 212}
           :ref (fn [el] (when el
                          (let [end (+ (.now js/Date) (* 15 1000))]
                            (letfn [(frame []
                                      (confetti (clj->js
                                                 {:particleCount 2,
                                                  :angle 60,
                                                  :spread 55,
                                                  :origin { :x 0 :y 0.7 }
                                                  :colors ["#fb923c" "#e5e7eb"]}))
                                      (confetti (clj->js
                                                 {:particleCount 2,
                                                  :angle 120,
                                                  :spread 55,
                                                  :origin { :x 1 :y 0.7 }
                                                  :colors ["#fb923c" "#e5e7eb"]}))
                                      (when (< (.now js/Date) end)
                                        (js/requestAnimationFrame frame)))]
                              (frame)))))}])
       [:div.overflow-auto
        {:style {:height "calc(100% - 212px)"}
         :class (if @finish "bg-orange-100" "pb-32")}

        (when @new-chiffre
          [new-patient {:chiffre @new-chiffre
                        :update-chiffre #(reset! new-chiffre %)
                        :create-new-patient (fn [chiffre]
                                              (save-data (swap! state update :data (fn [data]
                                                                                     (vec
                                                                                      (concat
                                                                                       [{:chiffre chiffre :hours []}]
                                                                                       data)))))
                                              (reset! new-chiffre nil))}])
        (if @finish
          [:div.top-0.left-0.z-1.bg-orange-100.w-full.flex.justify-center.p-8.flex-col
           [:div.grid.grid-rows-3.grid-cols-2.gap-3
            [:div.col-span-2.flex.items-stretch
             [:img.object-cover.h-full
              {:src "https://media.tenor.com/vb6Jd-BQxwoAAAAC/nuts-xonh.gif"
               :key :minion}]]
            [:div.flex.items-stretch
             [:img.object-cover {:src "https://media.tenor.com/cZxq-tBfvjAAAAAd/good-luck-congratulations.gif"
                                 :key :ape}]]
            [:div.col-span-1.row-span-2.flex.items-stretch
             [:img.object-cover {:src "https://media.tenor.com/cVHtxeoQreQAAAAC/happy-food.gif"
                                 :key :girl}]]
            [:div.flex.items-stretch
             [:img.object-cover {:src "https://media.tenor.com/oTQvU-uT97UAAAAd/i-want-to-congratulate-you-joe-biden.gif"
                                 :key :biden}]]]]
          (if (seq (:data @state))
            [:<>
             (map-indexed
              (fn [idx {:keys [chiffre hours]}]
                [patient {:key idx :idx idx :chiffre chiffre :hours hours}])
              (:data @state))
             (when-not @new-chiffre
               [:div.absolute.p-6
                {:style {:bottom 0 :right 0}}
                [add-patient-button {:add #(reset! new-chiffre "")}]])]
            (when-not @new-chiffre
              [:div.flex.flex-col.justify-center.items-center.text-center.w-full.h-full.px-4
               [:h2.text-2xl.mb-6.font-bold "Keine Stunden gespeichert"]
               "Leg ein neues Chiffre an um deine Stunden zu speichern"
               [add-patient-button {:add #(reset! new-chiffre "")}]])))]])))

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
                       (let [new-date (parse-date (str/replace (.-target.value e) "T" " "))]
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
  [["/" {:name ::main :view home :title "Home" :nav true}]
   ["/details/:chiffre"
    {:name ::details :view details :parameters {:path {:chiffre string?}}}]
   ["/stats" {:name ::stats :view stats :title "Statistiken" :nav true}]
   ["/backup" {:name ::backup :view backup :nav true :title "Backup"}]])

(defn menu [{:keys [visible? toggle-menu]}]
  [:div.absolute.left-0.top-0.h-full.z-10.w-full.flex
   {:style
    {:left (if visible? "0%" "-100%")
     :transition "all 300ms"}}
   [:div.bg-gray-100
    {:class "w-2/3"
     :style {:margin-top 60 :height "calc(100% - 60px - 40px)"}}
    [:nav.px-6.py-4.text-gray-700
     (let [active-page (:name (:data @match))]
       [:ul
        (->> routes
             (filter (fn [[_ {:keys [nav]}]] nav))
             (map
              (fn [[_ {:keys [name title]}]]
                ^{:key name}
                [nav-link {:active? (= active-page name) :href (rfe/href name) :on-click toggle-menu}
                 title])))])]]
   [:div {:on-click toggle-menu :class "w-1/3 h-full"}]])

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
  (rfe/start!
   (rf/router routes {:data {:coercion rss/coercion}})
   (fn [m] (reset! match m))
   {:use-framgent true})
  (dom/render [root] (.getElementById js/document "app"))
  #_(register-service-worker))

(defn ^:dev/after-load clear-cache-and-render!
  []
  (init))
