(ns moon-dweller.core
  (:require [moon-dweller.util :as u])
  (:use [moon-dweller.gameplay :only [messages]]))

(enable-console-print!)

(defn main []
    "Game initializer. Welcomes user and starts loop."
    (u/print-welcome-message)
    (messages))

(main)
