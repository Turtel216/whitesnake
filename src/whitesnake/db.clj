(ns whitesnake.db
  (:require [whitesnake.indexes :refer [make-index]])
  (:import [whitesnake.types Layer Database])
  (:require [whitesnake.storage :refer [get-entity]]))

(defn ref? [attr] (= :db/ref (:type (meta attr))))
(defn always [& more] true)

;; Create a dabase instance.
(defn make-db []
  (atom
   (Database. [(Layer.
                (fdb.storage.InMemory.) ; storage
                (make-index #(vector %3 %2 %1) #(vector %3 %2 %1) #(ref? %));VAET
                (make-index #(vector %2 %3 %1) #(vector %3 %1 %2) always);AVET
                (make-index #(vector %3 %1 %2) #(vector %2 %3 %1) always);VEAT
                (make-index #(vector %1 %2 %3) #(vector %1 %2 %3) always);EAVT
                )]0 0)))

;; Accessor for an entity with a given id.
(defn entity-at
  ([db ent-id] (entity-at db (:curr-time db) ent-id))
  ([db ts ent-id] (get-entity (get-in db [:layers ts :storage]) ent-id)))

;; Accessor for an attribute with a given id.
(defn attr-at
  ([db ent-id attr-name] (attr-at db ent-id attr-name (:curr-time db)))
  ([db ent-id attr-name ts] (get-in (entity-at db ts ent-id) [:attrs attr-name])))

;; Accessor for a value with a given id.
(defn value-of-at
  ([db ent-id attr-name]  (:value (attr-at db ent-id attr-name)))
  ([db ent-id attr-name ts] (:value (attr-at db ent-id attr-name ts))))

;; Accessor for an index for given kind.
(defn indx-at
  ([db kind] (indx-at db kind (:curr-time db)))
  ([db kind ts] (kind ((:layers db) ts))))

;; Function that returns a sequence of pairs, each consisting of the timestamp and value of an attribute’s update.
(defn evolution-of [db ent-id attr-name]
  (loop [res [] ts (:curr-time db)]
    (if (= -1 ts) (reverse res)
        (let [attr (attr-at db ent-id attr-name ts)]
          (recur (conj res {(:ts attr) (:value attr)}) (:prev-ts attr))))))
