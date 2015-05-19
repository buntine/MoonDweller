(ns moon-dweller.util
 (:require [dommy.core :as dom :refer-macros [sel sel1]]))

(defn play-sound [path]
  "Plays an OGG audio file"
  (let [sound (js/Audio. path)]
    (.play sound)))

(defn disable-input! []
  (-> (sel1 "#command")
      (dom/set-attr! :disabled)))

(defn enable-input! []
  (-> (sel1 "#command")
      (dom/remove-attr! :disabled)))

(defn md-pr [text i & {:keys [finished] :or {finished #()}}]
  "Prints a string one character at a time with an interval of i milliseconds"
  (let [li (dom/create-element :li)]
    (letfn [(populate [t]
             (if (not (empty? t))
               (let [f (first t)
                     html (dom/html li)]
                 (dom/set-html! li (str html f))
                 (.setTimeout js/window #(populate (rest t)) i))
               (do
                 (enable-input!)
                 (finished))))]
      (disable-input!)
      (populate text)
      (dom/append! (sel1 :#history) li))))

(defn print-with-newlines
  ([lines speed] (print-with-newlines lines speed ""))
  ([lines speed prepend]
    "Prints a sequence of strings, separated by newlines. Only useful for side-effects"
    (if (not (empty? prepend))
      (md-pr prepend speed :finished #(print-with-newlines lines speed))
      (if (not (empty? lines))
        (md-pr (str " - " (first lines)) speed
               :finished #(print-with-newlines (rest lines) speed))))))

(defn print-welcome-message []
  (print-with-newlines
    ["Moon Dweller"
     "A hobby project of Andrew Buntine"
     "https://github.com/buntine"] 10))

(defn direction? [verb] 
  (boolean 
    (some #{verb} 
          '(n e s w ne se sw nw north east south west northeast
            southeast southwest northwest in out up down))))
