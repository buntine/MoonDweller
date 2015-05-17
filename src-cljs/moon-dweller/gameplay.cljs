(ns moon-dweller.gameplay
  (:require [moon-dweller.util :as u]
            [moon-dweller.text :as t]
            [moon-dweller.state :as s])
  (:use [clojure.string :only (join split)]))

(declare messages object-details kill-player)

(defn say 
  "Prints s to the game screen. If given a vector of strings, a random one will be chosen."
  [& {:keys [raw path speed]
       :or {speed (s/text-speed)}}]
   (if (nil? raw)
     (say
       :raw (apply t/text path) :speed speed)
     (if (vector? raw)
       (say
         :raw (rand-nth raw) :speed speed)
       (u/md-pr raw speed))))

(defn prospects-for [verb context]
  "Returns the prospective objects for the given verb.
   E.g: 'cheese' might mean objects 6 and 12 or object 9 or nothing."
  (let [objnums (s/object-identifiers verb)
        fns {:room #(s/room-has-object? %)
             :inventory s/in-inventory?}]
    (if (nil? objnums)
      '()
      (filter (fns context)
              (if (integer? objnums) #{objnums} objnums)))))

(defn highest-val [obj-counts]
  "Returns the key of the highest value in the given map. If no
   single highest value is available, returns a lazy seq of keys
   of the tied-highest. This is used during language parsing."
  (if (not (empty? obj-counts))
    (let [highest (apply max (vals obj-counts))
          matches (into {}
                        (filter #(-> % val (= highest))
                                obj-counts))]
      (if (= (count matches) 1)
        (key (first matches))
        (keys matches)))))

(defn deduce-object ([verbs context] (deduce-object verbs '() context))
  ([verbs realised context]
   "Attempts to realise a single object given a sequence of verbs and
    a context. This allows for the same term to identify multiple objects.
    Context must be either :room or :inventory"
   (if (empty? verbs)
     (highest-val (frequencies realised))
     (recur (rest verbs)
            (concat (prospects-for (first verbs) context) realised)
            context))))

(defn has-knife? []
  "Returns true if the player has a knife-like object"
  (some #((object-details %) :cutter) s/inventory))

(defn obj-weight [objnum]
  "Returns the weight assigned to the given object"
  ((object-details objnum) :weight))

(defn inventory-weight []
  "Returns the current weight of the players inventory"
  (reduce + 0 (map obj-weight s/inventory)))

(defn event-for [objnum evt]
  "Returns either the value (usually a fn) assigned to the given event, or nil"
  (((object-details objnum) :events) evt))

(defn describe-object ([objnum] (describe-object objnum :game))
  ([objnum context]
    "Returns the string which describes the given object, or nil"
    (let [info ((object-details objnum) context)]
      (if info
        (str info)))))

(defn object-is? [objnum k]
  "Returns true is the object adheres to the given keyword"
  ((object-details objnum) k))

(defn display-inventory []
  "Displays the players inventory"
  (let [descs (map #(describe-object % :inv) s/inventory)]
    (if (not (empty? descs))
      (u/print-with-newlines descs (s/text-speed) (t/text 'inventory 'have))
      (say :path '(inventory empty)))
    (say :raw (str (t/text 'inventory 'credits) s/credits))))

(defn describe-objects-for-room [room]
  "Prints a description for each object that's in the given room"
  (let [objs (s/room-objects room)]
    (if (not (empty? objs))
      (u/print-with-newlines
        (remove nil? (map describe-object objs)) (s/text-speed)))))

(defn describe-room ([room] (describe-room room false))
  ([room verbose?]
   "Prints a description of the current room"
   (let [visited? (some #{room} s/visited-rooms)
         descs (t/rooms room)]
     (if visited?
       (say :raw ((if verbose? first second) descs))
       (do
         (s/visit-room! room)
         (say :raw (first descs))))
     (describe-objects-for-room room))))

(defn take-object! [objnum]
  "Attempts to take an object from the current room. If the object
   has an event for :take, then it must return a boolean - if true,
   the object will be taken"
  (cond
    (object-is? objnum :permanent)
      (say :path '(commands cant-take))
    (> (+ (inventory-weight) (obj-weight objnum)) s/total-weight)
      (say :path '(commands no-space))
    :else
      (let [evt (event-for objnum :take)]
        (if (or (nil? evt) (evt))
          (let [c ((object-details objnum) :credits)]
            ; If we are taking credits, just add them to the players wallet.
            (if (integer? c)
              (s/pay-the-man! c)
              (s/add-object-to-inventory! objnum))
            (s/take-object-from-room! objnum)
            (say :path '(commands taken)))))))

(defn drop-object! [objnum]
  "Attempts to drop an object into the current room. If the object
   has an event for :drop, then it must return a boolean - if true,
   the object will be dropped"
  (let [evt (event-for objnum :drop)]
    (when (or (nil? evt) (evt))
      (s/remove-object-from-inventory! objnum)
      (s/drop-object-in-room! objnum)
      (say :path '(commands dropped)))))

(letfn
  [(give-or-put [evt objx objy err-msg]
     "Does give/put with objects x and y. E.g: give cheese to old man"
     (let [events (event-for objy evt)]
       (when (or (nil? events) (not (events objx)))
         (say :raw err-msg)
         ((events objx))
         (s/remove-object-from-inventory! objx))))]

  (defn give-object! [objx objy]
    (give-or-put :give objx objy (t/text 'commands 'give-error)))

  (defn put-object! [objx objy]
    (give-or-put :put objx objy (t/text 'commands 'put-error))))

(defn inspect-object [objnum]
  "Inspects an object in the current room"
  (say :raw (describe-object objnum :inspect)))

(defn fuck-object
  ([objnum]
   "Attempts to fuck the given object"
   (if (not (object-is? objnum :living))
     (say :path '(commands fuck-object))
     (do
       (if (s/game-options :sound)
         (u/play-sound "/sound/fuck.wav"))
       (say :path '(commands fuck-living)))))
  {:ridiculous true})

(defn cut-object [objnum]
  "Attempts to cut the given object"
  (if (not (has-knife?))
    (say :path '(commands no-knife))
    (let [evt (event-for objnum :cut)]
      (if (nil? evt)
        (if (object-is? objnum :living)
          (say :path '(commands cut-living))
          (say :path '(commands cut-object)))
        (if (string? evt) (say :raw evt) (evt))))))

(defn eat-object! [objnum]
  "Attempts to eat the given object"
  (let [evt (event-for objnum :eat)]
    (if (nil? evt)
      (do
        (say :path '(commands do-not-eat))
        (kill-player ((object-details objnum) :inv)))
      (do
        (if (s/game-options :sound)
          (u/play-sound "/sound/eat.wav"))
        (if (string? evt) (say :raw evt) (evt))
        (s/remove-object-from-inventory! objnum)))))

(defn drink-object! [objnum]
  "Attempts to drink the given object. The event must return a boolean value, if
   false then the side-effect will not occur (removal of item from game)."
  (let [evt (event-for objnum :drink)
        drink! #(if (@s/game-options :sound)
                  (u/play-sound "/sound/drink.wav"))
                (s/remove-object-from-inventory! objnum)]
    (if (nil? evt)
      (say :path '(commands cannot-drink))
      (if (string? evt)
        (do (say :raw evt) (drink!))
        (if (evt)
          (drink!))))))

(defn talk-to-object [objnum]
  "Attempts to talk to the given object"
  (if (not (object-is? objnum :living))
    (say :path '(commands cannot-talk))
    (let [evt (event-for objnum :speak)]
      (if (nil? evt)
        (say :path '(commands speechless))
        (if (string? evt) (say :raw evt) (evt))))))

(defn pull-object [objnum]
  "Attempts to pull the given object (probably a lever)"
  (let [pull-evt (event-for objnum :pull)]
    (if (nil? pull-evt)
      (say :path '(commands cannot-pull))
      (pull-evt))))

(defn messages []
  (println "Messages"))
