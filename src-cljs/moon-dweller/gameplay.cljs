(ns moon-dweller.gameplay
  (:require [moon-dweller.util :as u]
            [moon-dweller.text :as t]
            [moon-dweller.state :as s])
  (:use [clojure.string :only (join split)]))

(declare messages object-details)

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
       (s/visit-room! room)
       (say :raw (first descs)))
     (describe-objects-for-room room))))

(defn messages []
  (println "Messages"))
