(ns moon-dweller.util
 (:require [dommy.core :as dom :refer-macros [sel sel1]]))

(defn play-sound [path]
  "Plays an OGG audio file"
  (let [sound (js/Audio. path)]
    (.play sound)))

(defn insert-command! [text]
  (let [li (-> (dom/create-element :li)
               (dom/add-class! :command)
               (dom/set-text! (str "> " text)))]
    (dom/append! (sel1 :#history) li)))

(defn disable-input! []
  (-> (sel1 "#command")
      (dom/set-value! "")
      (dom/set-attr! :disabled)))

(defn enable-input! []
  (-> (sel1 "#command")
      (dom/remove-attr! :disabled)
      (.focus)))

(defn direction? [verb] 
  (boolean 
    (some #{verb} 
          '(n e s w ne se sw nw north east south west northeast
            southeast southwest northwest in out up down))))
