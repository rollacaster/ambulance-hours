(ns ambulance-hours.core
  (:require
   ["react-native" :as rn]
   [reagent.core :as r]
   [shadow.expo :as expo]
   ["tailwind-rn" :as tw]
   [cljs.reader :refer [read-string]]
   ["@expo/vector-icons" :as icons]
   ["@react-navigation/native" :as nav]
   ["@react-navigation/stack" :as stack]
   ["date-fns" :as date-fns]
   ["@react-native-community/datetimepicker" :as picker]))

(defonce data (r/atom (list
                       {:chiffre "TS160990", :hours [{:date #inst "2020-11-09T07:47:22.498-00:00"}
                                                     {:date #inst "2020-11-09T10:47:22.965-00:00"}]})))

(defn button [{:keys [on-press secondary]} children]
  [:> rn/TouchableHighlight
   {:style (tw (str (if secondary "bg-gray-400" "bg-orange-500") " justify-center items-center rounded px-3 py-2"))
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
       [button {:on-press (fn [] (swap! data #(add-hours % chiffre)))}
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
    {:style (tw "mb-4 bg-orange-200 px-5 rounded-lg")}
    [:> rn/Text {:style (tw "text-gray-900 text-6xl font-extrabold tracking-wide")} (total-hours data)]]])

(defn footer []
  [:> rn/View
   {:style (tw "bg-orange-400 w-full items-center py-2")}
   [:> rn/Text
    {:style (tw "text-white")}
    "made with 💖"]])

(def nav-stack (stack/createStackNavigator))

(defn home [props]
  (let [new-chiffre (r/atom nil)]
    (fn []
      [:> rn/View
       {:style (tw "flex-1 pt-6 bg-white w-full")}
       [total {:data @data}]
       [:> rn/ScrollView
        (when @new-chiffre
          [new-patient {:chiffre @new-chiffre
                        :update-chiffre #(reset! new-chiffre %)
                        :create-new-patient (fn [chiffre]
                                              (swap! data conj {:chiffre chiffre :hours []})
                                              (reset! new-chiffre nil))}])
        (map-indexed
         (fn [idx {:keys [chiffre hours]}]
           [patient {:key idx :idx idx :chiffre chiffre :hours hours
                     :open-details (fn [] (.navigate (:navigation props) "details" chiffre))}])
         @data)]
       [add-patient-button {:add #(reset! new-chiffre "")}]])))

(defn details-date [{:keys [idx date on-remove on-edit]}]
  [:> rn/View {:style (tw (str "flex-row items-center px-6 py-4" (when (even? idx) " bg-gray-300")))}
   [:> rn/View {:style (tw "w-2/3")}
    [:> rn/Text ((.-format date-fns) date "yyyy-MM-dd HH:mm")]]
   [:> rn/View {:style (tw "w-1/3")}
    [:> rn/View {:style (tw "mb-2")}
     [button {:on-press on-edit} [:> rn/Text {:style (tw "text-white")} "Bearbeiten"]]]
    [button {:on-press on-remove} [:> rn/Text {:style (tw "text-white")} "Löschen"]]]])

(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn get-details-data [chiffre]
  (some #(when (= (:chiffre %) chiffre) %) @data))

(defn get-chiffre-from-props [props]
  (or ^js (.-params (:route props)) "TS160990"))

(defn update-details-data [new-details-data chiffre]
  (swap! data (fn [data] (map #(if (= chiffre (:chiffre %)) new-details-data %) data))))

(defn time-change [props]
  (let [{:keys [chiffre hours-idx]} (or (:params (:route (js->clj props :keywordize-keys true))) {:chiffre "TS160990" :hours-idx 0})
        details-data (get-details-data chiffre)
        {:keys [date]} (nth (:hours details-data) hours-idx)
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
                                (update-details-data (assoc-in details-data [:hours hours-idx :date] @new-date) chiffre)
                                (.goBack ^js (:navigation props)))}
           [:> rn/Text {:style (tw "text-white")} "Speichern"]]]
         [button {:secondary true
                  :on-press #(.goBack ^js (:navigation props))}
          [:> rn/Text {:style (tw "text-gray-700")} "Abbrechen"]]]]])))

(defn details [props]
  (let [details-data (get-details-data (get-chiffre-from-props props))
        {:keys [chiffre hours]} details-data]
    [:> rn/View
     [:> rn/Text {:style (tw "pt-6 px-6 mb-4 text-3xl")} chiffre]
     (map-indexed
      (fn [idx {:keys [date]}]
        [details-date {:key idx :idx idx :date date
                       :on-remove #(update-details-data (update details-data :hours vec-remove idx) chiffre)
                       :on-edit #(.navigate (:navigation props) "details-time-change" #js {:chiffre chiffre :hours-idx idx})}])
      hours)]))


(defn root []
  (-> (.getItem rn/AsyncStorage "data")
      (.then (fn [loaded-data] (if loaded-data (read-string loaded-data) @data)))
      (.then #(reset! data %)))
  (fn []
    [:> rn/SafeAreaView
     {:style (tw "flex-1 bg-orange-400")}
     [:> nav/NavigationContainer
      [:> (.-Navigator nav-stack)
       {:screenOptions #js {:headerStyle (tw "bg-orange-400")
                            :headerTintColor "#fff"
                            :headerTitleStyle (tw "text-2xl")
                            :title "Ambulante Stunden"}}
       [:> (.-Screen nav-stack) {:name "home" :component (r/reactify-component home)}]
       [:> (.-Screen nav-stack) {:name "details" :component (r/reactify-component details)}]
       [:> (.-Screen nav-stack) {:name "details-time-change" :component (r/reactify-component time-change)}]
       ]]
     [footer]]))

(defn start
  {:dev/after-load true}
  []
  (expo/render-root (r/as-element [root])))

(defn init []
  (start))

(comment
  (.removeItem rn/AsyncStorage "data"))
