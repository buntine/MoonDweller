(ns moon-dweller.views
  (:use [hiccup core page]))

(defn core []
  (html
    [:head 
      [:title "Moon Dweller"]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]]
    [:body
      [:h1 "Moon Dweller"]
      [:div {:id "title"}
        [:img {:class "desktop" :src "/images/heading.png" :alt "Moon Dweller"}]]
      [:ul {:id "history"}]
      [:div {:id "block"}]
      [:form {:id "commands"}
        [:input {:type "text" :id "command" :autocomplete "off"}]]
      [:a {:id "github-ribbon" :href "https://github.com/buntine/moon-dweller"}
        [:img {:src "https://camo.githubusercontent.com/e7bbb0521b397edbd5fe43e7f760759336b5e05f/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f677265656e5f3030373230302e706e67" :alt "Fork me on GitHub" :data-canonical-src "https://s3.amazonaws.com/github/ribbons/forkme_right_green_007200.png"}]]]
     (include-css "/css/main.css")
     (include-js "/js/main.js")))
