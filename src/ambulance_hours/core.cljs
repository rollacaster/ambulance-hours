(ns ambulance-hours.core
  (:require
    ["react-native" :as rn]
    [reagent.core :as r]
    [shadow.expo :as expo]
    ["tailwind-rn" :as tw]))

(defonce counter (r/atom 0))

(defn root []
  (-> (.getItem rn/AsyncStorage "counter")
      (.then (fn [counter] (if counter (js/parseInt counter) 154)))
      (.then #(reset! counter %)))
  (fn []
    [:> rn/SafeAreaView
     {:style (tw "flex-1 justify-between items-center")}
     [:> rn/View
      [:> rn/View
       {:style (tw "my-6 px-2")}
       [:> rn/Text {:style (tw "text-3xl text-center")} "💖"]
       [:> rn/Text
        {:style (tw "text-3xl text-center")}
        "Prinzessin's Ambulante Stunden Counter"]
       [:> rn/Text {:style (tw "text-3xl text-center")} "💖"]]
      [:> rn/View
       {:style (tw "items-center")}
       [:> rn/TouchableHighlight
        {:style (tw "bg-orange-500 justify-center w-10 h-10 items-center rounded mb-4")
         :on-press #(.setItem rn/AsyncStorage "counter" (str (swap! counter inc)))}
        [:> rn/Text {:style (tw "text-3xl text-white")} "+"]]
       [:> rn/Text {:style (tw "text-3xl mb-4")} @counter]
       [:> rn/TouchableHighlight
        {:style (tw "bg-orange-500 justify-center w-10 h-10 items-center rounded mb-4")
         :on-press #(.setItem rn/AsyncStorage "counter" (str (swap! counter dec)))}
        [:> rn/Text {:style (tw "text-3xl text-white")} "-"]]]]
     [:> rn/View [:> rn/Text "made with 💖"]]]))

(defn start
  {:dev/after-load true}
  []
  (expo/render-root (r/as-element [root])))

(defn init []
  (start))
