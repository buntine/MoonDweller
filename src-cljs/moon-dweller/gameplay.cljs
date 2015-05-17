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
        drink! #((if (s/game-options :sound)
                  (u/play-sound "/sound/drink.wav"))
                (s/remove-object-from-inventory! objnum))]
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

; Functions to execute when player speaks to a given object.
(def speech-fn-for
  {:pod-manager
     #(cond
        (not (s/can-afford? 3))
          (say :path '(talk pod-manager broke))
        (not (s/hit-milestone? :speak-to-captain))
          (say :path '(talk pod-manager not-ready))
        :else
          (do
            (say :path '(talk pod-manager ready))
            (say :path '(talk pod-manager flying)
                 :speed 300)
            (s/pay-the-man! -3)
            (s/set-current-room! 12))),
   :repairs-captain
     #(if (s/hit-milestone? :speak-to-captain)
        (say :path '(talk repairs-captain finished))
        (do
          (doseq [x '(a b c d e)]
            (say :path (concat '(talk repairs-captain spiel) [x])))
          (s/add-milestone! :speak-to-captain))),
   :homeless-bum
     #(say :path '(talk homeless-bum))})

; Functions to execute when player gives a particular X to a Y.
(def give-fn-for
  {:porno-to-boy
     #(do
        (say :path '(give porno-to-boy))
        (s/take-object-from-room! 7)
        (s/drop-object-in-room! 4)),
   :whisky-to-bum
     #(if (not (s/hit-milestone? :alcohol-to-bum))
        (do
          (say :path '(give whisky-to-bum))
          (s/add-object-to-inventory! 19)
          (s/add-milestone! :alcohol-to-bum))
        (say :path '(give alcohol-to-bum))),
   :becherovka-to-bum
     #(if (not (s/hit-milestone? :alcohol-to-bum))
        (do
          (say :path '(give becherovka-to-bum))
          (s/add-object-to-inventory! 19)
          (s/add-milestone! :alcohol-to-bum))
        (say :path '(give alcohol-to-bum)))})

; Functions to execute when player eats particular objects.
(def eat-fn-for
  {:eats-candy
     #(do
        (say :path '(eat candy))
        (s/pay-the-man! 5))})

; Functions to execute when player drinks particular objects.
(def drink-fn-for
  {:red-potion
     #(do
        (say :path '(drink red-potion))
        (kill-player "Red potion")),
   :green-potion
     #(do
        (say :path '(drink green-potion))
        (s/add-milestone! :drinks-green-potion)
        true),
   :brown-potion
     #(do
        (say :path '(drink brown-potion a))
        (say :path '(drink brown-potion b) :speed 250)
        true)
   :salvika-whisky
     #(if (s/in-inventory? 17)
        (do (say :path '(drink whisky success)) true)
        (do (say :path '(drink whisky fail)) false))
   :becherovka
     #(if (s/in-inventory? 16)
        (do (say :path '(drink becherovka success)) true)
        (do (say :path '(drink becherovka fail)) false))})

; Functions to execute when player pulls particular objects.
(def pull-fn-for
  {:control-lever
     #(do
        (say :path '(pull control-lever))
        (s/take-object-from-room! 2)
        (s/drop-object-in-room! 3)
        (s/set-current-room! 0))})

; Functions to execute when player cuts particular objects.
(def cut-fn-for
  {:spider-web
     #(do
        (say :path '(cut spider-web))
        (s/take-object-from-room! 20))})
 
; Functions to execute when player takes particular objects.
(def take-fn-for
  {:salvika-whisky
     #(if (s/can-afford? 3)
        (do
          (s/pay-the-man! -3)
          true)
        (do
          (say :path '(take whisky))
          (kill-player "Rusty knife to the throat"))),
    :becherovka
      #(if (s/can-afford? 4)
        (do
          (s/pay-the-man! -4)
          true)
        (do
          (say :path '(take becherovka))
          (kill-player "Acid to the brain")))
    :paper
      (fn []
        (say :path '(take paper))
        true)})

(defn make-dets [id details]
  "A helper function to merge in some sane defaults for object details"
  (letfn
    [(text-path [basename]
       (t/text 'objects id basename))]

    (let [defaults {:weight 0, :edible false, :permanent false,
                    :living false, :events {}, :credits nil,
                    :game (text-path 'game), :inv (text-path 'inv),
                    :inspect (text-path 'inspect)}]
      (merge defaults details))))

; The details of all objects. Each object is assigned one or more numbers in s/object-identifiers,
; which corresponds to it's index here. Permanent object cannot be taken and thus don't require
; weights or inventory descriptions. Events, such as :eat, :drink, :speak, :give, :take and :put
; can be assigned and will be executed in the correct contexts.
; TODO: Refector. Do not use indexes to identify objects.
(def object-details
  (vec (map
    #(apply make-dets %)
    (vector
      ['candy-bar
       {:weight 1
        :events {:eat (eat-fn-for :eats-candy)}}]
      ['small-bed
       {:permanent true}]
      ['large-lever
       {:events {:pull (pull-fn-for :control-lever)}
         :permanent true}]
      ['porno-mag
       {:weight 2}]
      ['green-keycard
       {:weight 1}]
      ['red-keycard
       {:weight 1}]
      ['silver-keycard
       {:weight 1}]
      ['alien-boy 
       {:permanent true
        :events {:give {3 (give-fn-for :porno-to-boy)},
                 :speak (t/text 'objects 'alien-boy 'speak)}
        :living true}]
      ['pod-manager
       {:permanent true
        :events {:speak (speech-fn-for :pod-manager)}
        :living true}]
      ['repairs-captain
       {:permanent true
        :events {:speak (speech-fn-for :repairs-captain)}
        :living true}]
      ['small-robot
       {:permanent true
        :events {:speak (t/text 'objects 'small-robot 'speak)}
        :living true}]
      ['homeless-bum 
       {:events {:speak (speech-fn-for :homeless-bum)
                 :give {16 (give-fn-for :whisky-to-bum)
                        17 (give-fn-for :becherovka-to-bum)}}
        :permanent true
        :living true}]
      ['red-potion
       {:events {:drink (drink-fn-for :red-potion)}
        :weight 1}]
      ['green-potion
       {:events {:drink (drink-fn-for :green-potion)}
        :weight 1}]
      ['brown-potion 
       {:events {:drink (drink-fn-for :brown-potion)}
        :weight 1}]
      ['shop-att-a
       {:permanent true
        :events {:speak (t/text 'objects 'shop-att-a 'speak)}
        :living true}]
      ['salvika 
       {:events {:drink (drink-fn-for :salvika-whisky)
                 :take (take-fn-for :salvika-whisky)}
        :weight 2}]
      ['becherovka
       {:events {:drink (drink-fn-for :becherovka)
                 :take (take-fn-for :becherovka)}
        :weight 2}]
      ['five-credits
       {:credits 5}]
      ['small-knife
       {:cutter true
        :weight 2}]
      ['spider-web
       {:events {:cut (cut-fn-for :spider-web)}
        :permanent true}]
      ['fat-protester
       {:permanent true
        :living true
        :events {:speak (t/text 'objects 'fat-protester 'speak)}}]
      ['thin-protester
       {:permanent true
        :living true
        :events {:speak (t/text 'objects 'thin-protester 'speak)}}]
      ['gentle-old-man
       {:permanent true
        :living true
        :events {:speak (t/text 'objects 'gentle-old-man 'speak)}}]
      ['paper-a
       {:weight 1
        :events {:take (take-fn-for :paper)}}]
      ['book-a
       {:weight 2}]
      ['medium-stone
       {:weight 3}]
      ['wet-floor
       {:permanent true}]
      ['staircase-a
       {:permanent true}]))))
  
(def directions {'north 0 'east 1 'south 2 'west 3 'northeast 4
                 'southeast 5 'southwest 6 'northwest 7 'up 8 'down 9
                 'in 10 'out 11})

(defn messages []
  (describe-room s/current-room))
