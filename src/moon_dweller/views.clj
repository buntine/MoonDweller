(ns moon-dweller.views
  (:use [hiccup core page]))

(defn core []
  (html
    [:head 
      [:title "Moon Dweller"]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]]
    [:body
      [:h1 "Moon Dweller"]
      [:ul {:id "game-content"}]
      [:form {:id "commands"}
        [:input {:type "text" :id "command"}]]]
     (include-css "/css/main.css")
     (include-js "/js/main.js")))
