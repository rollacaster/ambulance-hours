(ns ambulance-hours.core
  (:require
   ["react-native" :as rn]
   [reagent.core :as r]
   [shadow.expo :as expo]
   ["tailwind-rn" :as tw]
   [cljs.reader :refer [read-string]]
   ["@expo/vector-icons" :as icons]))

(defonce data (r/atom {"K3003" [{:date (new js/Date) :hours 2}
                                {:date (new js/Date) :hours 2}
                                {:date (new js/Date) :hours 2}]
                       "S1609" [{:date (new js/Date) :hours 2}
                                {:date (new js/Date) :hours 2}
                                {:date (new js/Date) :hours 2}]}))

(defn total-hours [data]
  (->> data
       vals
       flatten
       (map :hours)
       (reduce +)))

(defn root []
  (-> (.getItem rn/AsyncStorage "data")
      (.then read-string)
      (.then #(reset! data %)))
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
       [:> rn/Text {:style (tw "text-6xl font-extrabold tracking-wide  mb-4")} (total-hours @data)]]]
     [:> rn/View
      {:style (tw "bg-orange-400 w-full items-center pt-2")}
      [:> rn/Text
       {:style (tw "text-white")}
       "made with 💖"]]]))

(defn start
  {:dev/after-load true}
  []
  (expo/render-root (r/as-element [root])))

(defn init []
  (start))
