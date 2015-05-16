(ns moon-dweller.util)

(defn print-welcome-message []
  (println "Welcome"))

(defn play-sound [path]
  "Plays an OGG audio file"
  (let [sound (js/Audio. path)]
    (.play sound)))

; TODO: Implement
(defn md-pr [text i]
  "Prints a string one character at a time with an interval of i milliseconds"
  (println text))

(defn print-with-newlines
  ([lines speed] (print-with-newlines lines speed ""))
  ([lines speed prepend]
    "Prints a sequence of strings, separated by newlines. Only useful for side-effects"
    (if (not (empty? prepend))
      (md-pr prepend speed))
    (doseq
      [l (map #(str " - " %) lines)]
      (md-pr l speed))))
