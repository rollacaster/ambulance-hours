(ns ambulance-hours.core
  (:require
   ["react-native" :as rn]
   [reagent.core :as r]
   [shadow.expo :as expo]
   ["tailwind-rn" :as tw]
   [cljs.reader :refer [read-string]]
   ["@expo/vector-icons" :as icons]))

(defonce data (r/atom (list
                       {:chiffre "K300391" :hours [{:date (new js/Date) :hours 2}
                                                  {:date (new js/Date) :hours 2}
                                                  {:date (new js/Date) :hours 2}]}
                       {:chiffre "S160990" :hours [{:date (new js/Date) :hours 2}
                                                  {:date (new js/Date) :hours 2}
                                                  {:date (new js/Date) :hours 2}]}
                       {:chiffre "A280388" :hours [{:date (new js/Date) :hours 2}
                                                   {:date (new js/Date) :hours 2}
                                                   {:date (new js/Date) :hours 2}]})))

(defn button [{:keys [on-press]} children]
  [:> rn/TouchableHighlight
   {:style (tw "bg-orange-500 justify-center w-10 h-10 items-center rounded mb-4")
    :on-press on-press}
   children])

(defn total-hours [data]
  (->> data
       (map :hours)
       flatten
       (map :hours)
       (reduce +)))

(defn patient [{:keys [idx chiffre hours]}]
  [:> rn/View
   {:style (tw (str "flex-row px-6 py-6 justify-between " (when (even? idx) "bg-gray-300")))}
   [:> rn/TouchableHighlight
    {:on-press #()
     :style (tw (str "px-2 justify-center items-center rounded-lg flex-row " (if (even? idx) "bg-gray-100" "bg-gray-700")))}
    [:<>
     [:> rn/Text
      {:style (tw (str "text-2xl " (when (odd? idx) "text-white")))}
      chiffre]
     [:> icons/EvilIcons {:name "eye" :size 32 :color (if (even? idx) "black" "white")
                          :style (tw "mr-1")}]]]
   [:> rn/View
    {:style (tw "flex-row items-center pt-3")}
    [button {:on-press #(prn "hi")}
     [:> rn/Text {:style (tw "text-3xl text-white")} "-"]]
    [:> rn/View
     [:> rn/Text
      {:style (tw "text-2xl px-4 pb-3")}
      (->> hours
           (map :hours)
           (reduce +))]]
    [button {:on-press #(prn "hi")}
     [:> rn/Text {:style (tw "text-3xl text-white")} "+"]]]])

(defn root []
  (-> (.getItem rn/AsyncStorage "data")
      (.then (fn [loaded-data] (if loaded-data (read-string loaded-data) @data)))
      (.then #(reset! data %)))

  (let [new-chiffre (r/atom nil)]
    (fn []
      [:> rn/SafeAreaView
       {:style (tw "flex-1 items-center bg-orange-400")}
       [:> rn/View
        {:style (tw "bg-orange-400 w-full p-3")}
        [:> rn/View {:style (tw "flex-row items-center")}
         [:> icons/EvilIcons {:name "clock" :size 48 :color "white"
                              :style (tw "mr-1")}]
         [:> rn/Text
          {:style (tw "text-3xl text-white")}
          "Ambulante Stunden"]]]
       [:> rn/View
        {:style (tw "flex-1 pt-6 bg-white w-full")}
        [:> rn/View
         {:style (tw "items-center")}
         [:> rn/View
          {:style (tw "mb-4 bg-orange-200 px-5 rounded-lg")}
          [:> rn/Text {:style (tw "text-gray-900 text-6xl font-extrabold tracking-wide")} (total-hours @data)]]]
        [:> rn/ScrollView
         (when @new-chiffre
             [:> rn/View
              {:style (tw "flex-row p-6  justify-between items-center bg-gray-700")}
              [:> rn/TextInput
               {:style (tw "bg-white w-1/2 text-2xl rounded p-2 border-2")
                :auto-focus true
                :placeholder "Chiffre"
                :value @new-chiffre :on-change-text (fn [text] (reset! new-chiffre text))}]
              [:> rn/TouchableHighlight
               {:on-press (fn []) :style (merge
                                         {:shadowColor "#000",
                                          :shadowOffset {:width 0, :height 2},
                                          :shadowOpacity 0.25,
                                          :shadowRadius 3.84,
                                          :elevation 5}
                                         (js->clj (tw "bg-orange-500 justify-center items-center rounded p-2")))}
               [:> rn/Text
                {:style (tw "text-2xl text-white") :on-press (fn []
                                                               (swap! data conj {:chiffre @new-chiffre :hours []})
                                                               (reset! new-chiffre nil))}
                "Hinzufügen"]]])
         (map-indexed
            (fn [idx {:keys [chiffre hours]}]
              [patient {:key idx :idx idx :chiffre chiffre :hours hours}])
            @data)]
        [:> rn/TouchableHighlight
         {:style (merge {:bottom 0
                         :right 0
                         :shadowColor "#000",
                         :shadowOffset {:width 0, :height 2},
                         :shadowOpacity 0.25,
                         :shadowRadius 3.84,
                         :elevation 5}
                        (js->clj (tw "absolute p-6")))
          :on-press #(reset! new-chiffre "")}
         [:> rn/View
          {:style (tw "bg-orange-400 rounded-full w-16 h-16 items-center justify-center")}
          [:> rn/Text
           {:style (tw "text-white text-5xl font-bold")}
           "+"]]]]
       [:> rn/View
        {:style (tw "bg-orange-400 w-full items-center py-2")}
        [:> rn/Text
         {:style (tw "text-white")}
         "made with 💖"]]])))

(defn start
  {:dev/after-load true}
  []
  (expo/render-root (r/as-element [root])))

(defn init []
  (start))

(comment
  (.removeItem rn/AsyncStorage "data"))
