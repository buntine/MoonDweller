(ns moon-dweller.core
  (:require [moon-dweller.util :as u])
  (:use [moon-dweller.gameplay :only [messages]]))

(enable-console-print!)

(defn main []
    "Game initializer. Welcomes user and starts loop."
 ;   (u/play-sound "/sound/opening.wav")
    (messages))

(main)
