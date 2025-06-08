(ns whitesnake.types)

;; A database instance consisting of:
;; - Layers of entities.
;; - A top-id value which the next available unique ID.
;; - The time at which the database instance was last updated.
(defrecord Database [layers top-id curr-time])

;; A record describing a database layer consisting of:
;; - A data store for entities
;; - Indexes for queries
(defrecord Layer [storage VAET AVET VEAT EAVT])

;; A database entity consisting of:
;; - An ID
;; - Its attributes
(defrecord Entity [id attrs])

;; A function for making entities. If no id is given it is set to db/no-id-yet.
(defn make-entity
  ([] (make-entity :db/no-id-yet))
  ([id] (Entity. id {})))

;; An attribute of a database entity
(defrecord Attr [name value ts prev-ts])

;; A function for creating an Attr.
(defn make-attr
  ([name value type
    & {:keys [cardinality] :or {cardinality :db/single}}]
   {:pre [(contains? #{:db/single :db/multiple} cardinality)]}
   (with-meta (Attr. name value -1 -1) {:type type :cardinality cardinality})))

;; Adds a given attribyte to an entity's attribute map.
(defn add-attr [ent attr]
  (let [attr-id (keyword (:name attr))]
    (assoc-in ent [:attrs attr-id] attr)))
