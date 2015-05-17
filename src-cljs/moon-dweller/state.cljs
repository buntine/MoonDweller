(ns moon-dweller.state
  (:require [moon-dweller.util :as u]))

(def current-room 0)             ; Current room the player is in.
(def visited-rooms [])           ; Rooms that the player has visited.
(def inventory [])               ; Players inventory of items.
(def credits 0)                  ; Players credits (aka $$$).
(def milestones #{})             ; Players milestones. Used to track and manipulate story.
(def game-options {:retro true   ; Print to stdout with tiny pauses between characters.
                   :sound true}) ; Play sound during gameplay.

; A vector containing the objects that each room contains when the game starts. Each index
; corresponds to the room as defined in 'rooms'.
(def room-objects
  (vector
    [0 1]        ;0
    []           ;1
    [2]          ;2
    []           ;3
    []           ;4
    []           ;5
    []           ;6
    [7]          ;7
    []           ;8
    []           ;9
    [8]          ;10
    [9 10]       ;11
    [11]         ;12
    []           ;13
    []           ;14
    [12 13 14]   ;15
    [18]         ;16
    [15 16 17]   ;17
    []           ;18
    [20]         ;19
    []           ;20
    [21 22 26]   ;21
    []           ;22
    [23]         ;23
    []           ;24
    []           ;25
    [27]         ;26
    [25]         ;27
    [24]         ;28
    []           ;29
    []))         ;30

; Specifies the verbs that users can identify an object with (a gun might
; be "gun", "weapon", etc). A set means that the given term may refer to
; multiple objects. The system will try to deduce the correct object when
; a command is entered. Each index corresponds to the same index in room-objects.
(def object-identifiers
    {'candy 0 'bar 0 'bed 1 'lever 2 'mag 3 'magazine 3 'porno 3 'boy 7
     'teenager 7 'keycard #{4 5 6} 'key #{4 5 6} 'man #{8 9 21 22 23} 'robot 10
     'green #{4 13} 'red #{5 12} 'brown 14 'silver 6 'bum 11 'potion #{12 13 14}
     'credits 18 'attendant 15 'woman 15 'salvika 16 'whisky 16 'becherovka 17
     'web 20 'knife 19 'small 19 'thin 22 'skinny 22 'fat 21 'paper 24 'book 25
     'stone 26 'rock 26})

(defn set-option! [option value]
  "Sets one of the pre-defined game options. Assumes valid input."
  (set! game-options (assoc game-options option value)))

(defn valid-option? [option]
  (let [opts (keys game-options)]
    (boolean (some #{option} opts))))

(defn can-afford? [n]
  (>= credits n))

(defn hit-milestone? [m]
  (contains? milestones m))

(defn add-milestone! [m]
  "Adds the given milestone to the players list"
  (set! milestones (conj milestones m)))

(defn set-current-room! [room]
    (set! current-room room))

(defn objects-in-room ([] (objects-in-room current-room))
  ([room]
   (nth room-objects room)))

(defn room-has-object?
  "Returns true if the gien room currently houses the given object"
  ([objnum] (room-has-object? current-room objnum))
  ([room objnum]
   (boolean (some #{objnum} (objects-in-room room)))))

(defn in-inventory? [objnum]
  "Returns true if object assigned to 'objnum' is in players inventory"
  (boolean (some #{objnum} inventory)))

(letfn
  [(alter-room! [room changed]
     "Physically alters the contents of the given. Must be called from within
      a dosync form"
     (set! room-objects 
           (assoc-in room-objects [room] changed)))]

  (defn take-object-from-room!
    ([objnum] (take-object-from-room! current-room objnum))
    ([room objnum]
     (alter-room! room
                  (vec (remove #(= objnum %)
                                 (objects-in-room room))))))

  (defn drop-object-in-room!
    ([objnum] (drop-object-in-room! current-room objnum))
    ([room objnum]
     (alter-room! room
                  (conj (objects-in-room room) objnum)))))

(defn remove-object-from-inventory! [objnum]
  "Physically removes an object from the players inventory."
  (set! inventory (vec (remove #(= % objnum) inventory))))
 
(defn add-object-to-inventory! [objnum]
  "Physically adds an object to the players inventory."
  (set! inventory (conj inventory objnum)))

(defn pay-the-man! [c]
  (set! credits (+ credits c)))

(defn visit-room! [room]
  (set! visited-rooms (conj visited-rooms room)))

(defn save-game! []
  (u/md-pr "Not yet implemented!"))

(defn load-game! []
  (u/md-pr "Not yet implemented!"))
