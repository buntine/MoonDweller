(ns moon-dweller.util
 (:require [dommy.core :as dom :refer-macros [sel sel1]]))

(defn play-sound [path]
  "Plays an audio file"
  (let [sound (js/Audio. path)]
    (.play sound)))

(defn scroll-to-bottom []
  (.scrollTo js/window 0 (.. js/document -body -scrollHeight)))

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

(defn extend-vec [a b]
  "Merges two vecs by appending remainder of b onto a. Assumes a is larger or equal to a.
   e.g: (extend-vecs [1,2,3], [1,2,9,4]) -> [1,2,3,4]"
  (let [overflow (subvec b (count a))]
    (vec (concat a overflow))))

(defn direction? [verb] 
  (boolean 
    (some #{verb} 
          '(n e s w ne se sw nw north east south west northeast
            southeast southwest northwest in out up down))))
