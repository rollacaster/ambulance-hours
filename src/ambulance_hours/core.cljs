(ns ambulance-hours.core
  (:require [clojure.string :as str]
            ["react-native" :as rn]
            [reagent.core :as r]
            [shadow.expo :as expo]
            ["tailwind-rn" :as tw]
            [cljs.reader :refer [read-string]]
            ["@expo/vector-icons" :as icons]
            ["@react-navigation/drawer" :as drawer]
            ["@react-navigation/native" :as nav]
            ["@react-navigation/stack" :as stack]
            ["date-fns" :as date-fns]
            ["@react-native-community/datetimepicker" :as picker]
            ["victory-native" :as victory]
            ["react-native-svg" :as svg]
            ["expo-device" :as device]))

(def state (r/atom {:data []}))

(defn format-date [date]
  ((.-format date-fns) date "yyyy-MM-dd HH:mm"))

(defn save-data [data]
  (.setItem rn/AsyncStorage "state" (prn-str data)))


(defn button [{:keys [on-press secondary]} children]
  [:> rn/TouchableHighlight
   {:style (merge {:shadowColor "#000",
                   :shadowOffset {:width 0, :height 2},
                   :shadowOpacity 0.25,
                   :shadowRadius 3.84,
                   :elevation 5}
                  (js->clj (tw (str (if secondary "bg-gray-400" "bg-orange-400") " justify-center items-center rounded px-3 py-2"))))
    :on-press on-press}
   children])

(defn total-hours [data]
  (->> data
       (map :hours)
       flatten
       count))

(defn add-hours [data chiffre]
  (map (fn [patient] (if (= (:chiffre patient) chiffre)
                      (update patient :hours conj {:date (new js/Date)})
                      patient))
       data))

(defn patient [{:keys [idx chiffre hours open-details]}]
  (let [hours-count (->> hours (map :hours) count)]
    [:> rn/View
     {:style (tw (str "flex-row px-6 py-6 justify-between " (when (even? idx) "bg-gray-300")))}
     [:> rn/TouchableHighlight
      {:on-press (if (> hours-count 0) open-details identity)
       :style (tw (str "px-2 justify-center items-center rounded-lg flex-row " (if (even? idx) "bg-gray-100" "bg-gray-700")))}
      [:<>
       [:> rn/Text
        {:style (tw (str "text-2xl " (when (odd? idx) "text-white")))}
        chiffre]
       (when (> hours-count 0)
         [:> icons/EvilIcons {:name "eye" :size 32 :color (if (even? idx) "black" "white")
                              :style (tw "mr-1")}])]]
     [:> rn/View
      {:style (tw "flex-row items-center pt-3")}
      [:> rn/View
       [:> rn/Text
        {:style (tw "text-2xl px-4 pb-3")}
        hours-count]]
      [:> rn/View {:style (tw "mb-3")}
       [button {:on-press (fn [] (save-data (swap! state update :data #(add-hours % chiffre))))}
        [:> rn/Text {:style (tw "text-2xl text-white")} "+"]]]]]))

(defn new-patient [{:keys [chiffre update-chiffre create-new-patient]}]
  [:> rn/View
   {:style (tw "flex-row p-6  justify-between items-center bg-gray-700")}
   [:> rn/TextInput
    {:style (tw "bg-white w-1/2 text-2xl rounded p-2 border-2")
     :auto-focus true
     :on-blur #(when (= chiffre "") (update-chiffre nil))
     :placeholder "Chiffre"
     :default-value chiffre :on-change-text update-chiffre}]
   [:> rn/TouchableHighlight
    {:on-press (fn []) :style (merge
                              {:shadowColor "#000",
                               :shadowOffset {:width 0, :height 2},
                               :shadowOpacity 0.25,
                               :shadowRadius 3.84,
                               :elevation 5}
                              (js->clj (tw "bg-orange-500 justify-center items-center rounded p-2")))}
    [:> rn/Text
     {:style (tw "text-2xl text-white") :on-press #(create-new-patient chiffre)}
     "Hinzufügen"]]])

(defn add-patient-button [{:keys [add]}]
  [:> rn/TouchableHighlight
   {:style (merge {:bottom 0
                   :right 0
                   :shadowColor "#000",
                   :shadowOffset {:width 0, :height 2},
                   :shadowOpacity 0.25,
                   :shadowRadius 3.84,
                   :elevation 5}
                  (js->clj (tw "absolute p-6")))
    :on-press add}
   [:> rn/View
    {:style (tw "bg-orange-400 rounded-full w-16 h-16 items-center justify-center")}
    [:> rn/Text
     {:style (tw "text-white text-5xl font-bold")}
     "+"]]])

(defn header []
  [:> rn/View
   {:style (tw "bg-orange-400 w-full p-3")}
   [:> rn/View {:style (tw "flex-row items-center")}
    [:> icons/EvilIcons {:name "clock" :size 48 :color "white"
                         :style (tw "mr-1")}]
    [:> rn/Text
     {:style (tw "text-3xl text-white")}
     "Ambulante Stunden"]]])

(defn total [{:keys [data]}]
  [:> rn/View
   {:style (tw "items-center")}
   [:> rn/View
    {:style (tw "relative justify-center")}
    (let [size 200]
      [:> svg/Svg {:width size :height size}
       [:> victory/VictoryPie
        {:standalone false
         :padding 20
         :width size :height size
         :innerRadius (* size 0.35)
         :colorScale [(.-color (tw "text-gray-300"))
                      (.-color (tw "text-orange-400"))]
         :labels (fn [] nil)
         :data [{ :y (- 600 (total-hours data)) }
                { :y (total-hours data) }]}]])
    [:> rn/Text {:style (merge
                         {:top 60 :left "50%" :transform [{:translateX (case (count (str (total-hours data)))
                                                                         1 -110
                                                                         2 -127
                                                                         3 -155)}]}
                         (js->clj (tw "absolute text-6xl font-extrabold tracking-wide text-center")))}
     (total-hours data)]]])

(defn footer []
  [:> rn/View
   {:style (tw "bg-orange-400 w-full items-center py-2")}
   [:> rn/Text
    {:style (tw "text-white")}
    "made with 💖"]])

(def nav-stack (stack/createStackNavigator))
(def main-stack (stack/createStackNavigator))
(def nav-drawer (drawer/createDrawerNavigator))

(defn screen [children]
  [:> rn/SafeAreaView
   {:style (tw "flex-1 bg-orange-400")}
   children
   [footer]])

(defn yearly-stats [data]
  (->> data
       (mapcat :hours)
       (map #(assoc % :year (js/parseInt ((.-format date-fns) (:date %) "yyyy"))))
       (group-by :year)
       (map (fn [[year hours]] {:year year :hours-count (count hours)}))
       (sort-by :year)
       reverse))

(defn prepare-device-name [device-name]
  (str/replace (str/lower-case device-name) #"[\s\W]" ""))

(defn prepare-last-backup [last-backup]
  (-> last-backup
      (str/replace #" " "-")
      (str/replace #":" "-")))

(defn backup []
  (let [error (r/atom nil)
        backup-loading (r/atom nil)]
    (fn []
      [screen
       [:> rn/View {:style (tw "bg-white flex-1 w-full pt-6 px-6")}
        [:> rn/Text {:style (tw "text-2xl pb-6")} "Backup"]
        [:> rn/View {:style (tw "mb-3")}
         [button {:on-press (fn []
                              (let [last-backup (format-date (new js/Date))]
                                (-> (js/fetch
                                     "http://192.168.178.20:8000/backup"
                                     (clj->js {:method "POST"
                                               :headers {"content-type" "application/edn"}
                                               :body (prn-str
                                                      {:device-name (prepare-device-name device/deviceName)
                                                       :state (assoc @state :last-backup last-backup)})}))
                                    (.then (fn []
                                             (reset! error false)
                                             (save-data (swap! state assoc :last-backup last-backup))))
                                    (.catch (fn [e]
                                              (prn e)
                                              (reset! error true)))))) }
          [:> rn/Text {:style (tw "text-white text-lg")} "Backup erstellen"]]]
        [:> rn/Text {:style (tw "text-lg pb-12")} "Letztes Backup: " (if @error "Fehlgeschlagen"
                                                                         (if (:last-backup @state) (:last-backup @state) "-"))]
        [:> rn/View {:style (tw "mb-3")}
         [button {:on-press (fn []
                              (reset! backup-loading "LOADING")
                              (->(js/fetch
                                  (str "http://192.168.178.20:8000/backup?device-name="
                                       (prepare-device-name device/deviceName)
                                       "&last-backup="
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
          [:> rn/Text {:style (tw "text-white text-lg")} "Backup laden"]]]
        (when @backup-loading
          [:> rn/Text {:style (tw "text-lg pb-12")} "Letztes Backup: " @backup-loading])]])))

(defn stats []
  [screen
   [:> rn/View {:style (tw "bg-white flex-1 w-full pt-6 px-6")}
    [:> rn/Text {:style (tw "text-2xl pb-6")}
     "Statistiken"]
    (map
     (fn [{:keys [year hours-count]}]
       [:<> {:key year}
        [:> rn/View {:style (tw "flex-row justify-between pr-12 mb-2")}
         [:> rn/Text {:style (tw "text-xl mr-6")} year]
         [:> rn/Text {:style (tw "text-xl font-bold text-right")} (str hours-count (if (> hours-count 1) " Stunden" " Stunde"))]]])
     (yearly-stats (:data @state)))]])

(defn home [props]
  (let [new-chiffre (r/atom nil)]
    (fn []
      [screen
       [:> rn/View
        {:style (tw "flex-1 bg-white w-full")}
        [total {:data (:data @state)}]
        [:> rn/ScrollView
         (when @new-chiffre
           [new-patient {:chiffre @new-chiffre
                         :update-chiffre #(reset! new-chiffre %)
                         :create-new-patient (fn [chiffre]
                                               (save-data (swap! state update :data conj {:chiffre chiffre :hours []}))
                                               (reset! new-chiffre nil))}])
         (map-indexed
          (fn [idx {:keys [chiffre hours]}]
            [patient {:key idx :idx idx :chiffre chiffre :hours hours
                      :open-details (fn [] (.navigate (:navigation props) "details" chiffre))}])
          (:data @state))]
        [add-patient-button {:add #(reset! new-chiffre "")}]]])))

(defn details-date [{:keys [idx date on-remove on-edit]}]
  [:> rn/View {:style (tw (str "flex-row items-center px-6 py-4" (when (even? idx) " bg-gray-300")))}
   [:> rn/View {:style (tw "w-2/3")}
    [:> rn/Text {:style (tw "text-lg mb-1 font-bold")}(str "Stunde " (inc idx))]
    [:> rn/Text {:style (tw "text-gray-800")}((.-format date-fns) date "yyyy-MM-dd HH:mm")]]
   [:> rn/View {:style (tw "w-1/3")}
    [:> rn/View {:style (tw "mb-2")}
     [button {:on-press on-edit} [:> rn/Text {:style (tw "text-white")} "Bearbeiten"]]]
    [button {:on-press on-remove :secondary true}
     [:> rn/Text {:style (tw "text-gray-700")} "Löschen"]]]])

(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn get-details-data [chiffre]
  (some #(when (= (:chiffre %) chiffre) %) (:data @state)))

(defn get-chiffre-from-props [props]
  (or ^js (.-params (:route props)) "TS160990"))

(defn update-details-data [new-details-data chiffre]
  (-> (swap! state update :data (fn [data] (map #(if (= chiffre (:chiffre %)) new-details-data %) data)))
      save-data))

(defn time-change [props]
  (let [{:keys [on-edit date]} (:params (:route (js->clj props :keywordize-keys true)))
        new-date (r/atom date)]
    (fn []
      [:> rn/View
       [:> (.-default picker)
        {:value @new-date
         :locale "de-DE"
         :onChange (fn [_ updated-date] (reset! new-date updated-date))}]
       [:> (.-default picker)
        {:value @new-date
         :locale "de-DE"
         :is24Hour true
         :mode "time"
         :onChange (fn [_ updated-date] (reset! new-date updated-date))}]
       [:> rn/View {:style (tw "px-6 items-center")}
        [:> rn/View {:style (tw "w-2/3")}
         [:> rn/View {:style (tw "mb-3")}
          [button {:on-press #(do
                                (on-edit @new-date)
                                (.goBack ^js (:navigation props)))}
           [:> rn/Text {:style (tw "text-white")} "Speichern"]]]
         [button {:secondary true
                  :on-press #(.goBack ^js (:navigation props))}
          [:> rn/Text {:style (tw "text-gray-700")} "Abbrechen"]]]]])))

(defn details [props]
  (let [details-data (get-details-data (get-chiffre-from-props props))
        {:keys [chiffre hours]} details-data]
    [:> rn/View {:style (tw "mb-8 pb-8")}
     [:> rn/Text {:style (tw "pt-6 px-6 mb-4 text-3xl")} chiffre]
     [:> rn/ScrollView
      (let [sorted-hours (sort-by :date (fn [a b] (> (.getTime a) (.getTime b))) hours)]
        (map-indexed
         (fn [idx {:keys [date]}]
           [details-date {:key idx :idx (- (dec (count hours)) idx) :date date
                          :on-remove #(update-details-data (assoc details-data :hours (vec-remove (into [] sorted-hours) idx)) chiffre)
                          :on-edit #(.navigate (:navigation props) "details-time-change" #js
                                               {:date date
                                                :on-edit (fn [new-date]
                                                           (update-details-data (assoc-in details-data [:hours idx :date] new-date) chiffre))})}])
         sorted-hours))]]))

(defn home-nav []
  [:> (.-Navigator nav-stack)
   {:headerMode "none"}
   [:> (.-Screen nav-stack) {:name "home" :component (r/reactify-component home)}]
   [:> (.-Screen nav-stack) {:name "details" :component (r/reactify-component details)}]
   [:> (.-Screen nav-stack) {:name "details-time-change" :component (r/reactify-component time-change)}]])

(defn main-nav []
  [:> (.-Navigator nav-drawer) {:initialRouteName "Home"}
   [:> (.-Screen nav-drawer)
    {:name "Home"
     :component (r/reactify-component home-nav)}]
   [:> (.-Screen nav-drawer)
    {:name "Statistiken"
     :component (r/reactify-component stats)}]
   [:> (.-Screen nav-drawer)
    {:name "Backup"
     :component (r/reactify-component backup)}]])


(defn root []
  (-> (.getItem rn/AsyncStorage "state")
      (.then (fn [loaded-state] (if loaded-state (read-string loaded-state) {:data []})))
      (.then #(reset! state %)))
  (fn []
    [:> nav/NavigationContainer
     [:> (.-Navigator nav-stack)
      {:screenOptions #js {:headerStyle (tw "bg-orange-400")
                           :headerTintColor "#fff"
                           :headerTitleStyle (tw "text-2xl")
                           :title "Ambulante Stunden"}}
      [:> (.-Screen nav-stack) {:name "main" :component (r/reactify-component main-nav)}]]]))

(defn start
  {:dev/after-load true}
  []
  (expo/render-root (r/as-element [root])))

(defn init []
  (start))

(comment
  (.removeItem rn/AsyncStorage "state"))
