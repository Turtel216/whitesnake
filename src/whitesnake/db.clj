(ns whitesnake.db
  (:require [whitesnake.indexes :refer [make-index from-eav to-eav usage-pred indexes]])
  (:require [whitesnake.types :refer [Database Layer]])
  (:require [clojure.set :as CS])
  (:require [whitesnake.storage :as fdb.storage])
  (:require [whitesnake.storage :refer [write drop-entity reffing-to get-entity]])
  (:require [whitesnake.storage :as fdb.storage]))

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

(defn- next-ts [db] (inc (:curr-time db)))

(defn- update-creation-ts [ent ts-val]
  (reduce #(assoc-in %1 [:attrs %2 :ts] ts-val) ent (keys (:attrs ent))))

(defn- next-id [db ent]
  (let [top-id (:top-id db)
        ent-id (:id ent)
        increased-id (inc top-id)]
    (if (= ent-id :db/no-id-yet)
      [(keyword (str increased-id)) increased-id]
      [ent-id top-id])))

(defn- fix-new-entity [db ent]
  (let [[ent-id next-top-id] (next-id db ent)
        new-ts               (next-ts db)]
    [(update-creation-ts (assoc ent :id ent-id) new-ts) next-top-id]))

(defn- update-entry-in-index [index path operation]
  (let [update-path (butlast path)
        update-value (last path)
        to-be-updated-set (get-in index update-path #{})]
    (assoc-in index update-path (conj to-be-updated-set update-value))))

(defn- update-attr-in-index [index ent-id attr-name target-val operation]
  (let [colled-target-val (collify target-val)
        update-entry-fn (fn [ind vl]
                          (update-entry-in-index
                           ind
                           ((from-eav index) ent-id attr-name vl)
                           operation))]
    (reduce update-entry-fn index colled-target-val)))

(defn- add-entity-to-index [ent layer ind-name]
  (let [ent-id (:id ent)
        index (ind-name layer)
        all-attrs  (vals (:attrs ent))
        relevant-attrs (filter #((usage-pred index) %) all-attrs)
        add-in-index-fn (fn [ind attr]
                          (update-attr-in-index ind ent-id (:name attr)
                                                (:value attr)
                                                :db/add))]
    (assoc layer ind-name  (reduce add-in-index-fn index relevant-attrs))))

(defn add-entity [db ent]
  (let [[fixed-ent next-top-id] (fix-new-entity db ent)
        layer-withupdated-storage (update-in
                                   (last (:layers db)) [:storage] write entity fixed-ent)
        add-fn (partial add-entity-to-index fixed-ent)
        new-layer (reduce add-fn layer-with-updated-storage (indexes))]
    (assoc db :layers (conj (:layers db) new-layer) :top-id next-top-id)))

(defn add-entities [db ents-seq] (reduce add-entity db ents-seq))

(defn- update-attr-modification-time
  [attr new-ts]
  (assoc attr :ts new-ts :prev-ts (:ts attr)))

(defn- update-attr-value [attr value operation]
  (cond
    (single? attr)    (assoc attr :value #{value})
    (= :db/reset-to operation)
    (assoc attr :value value)
    (= :db/add operation)
    (assoc attr :value (CS/union (:value attr) value))
    (= :db/remove operation)
    (assoc attr :value (CS/difference (:value attr) value))))

(defn- update-attr [attr new-val new-ts operation]
  {:pre  [(if (single? attr)
            (contains? #{:db/reset-to :db/remove} operation)
            (contains? #{:db/reset-to :db/add :db/remove} operation))]}
  (-> attr
      (update-attr-modification-time new-ts)
      (update-attr-value new-val operation)))

(defn update-entity
  ([db ent-id attr-name new-val]
   (update-entity db ent-id attr-name new-val :db/reset-to))
  ([db ent-id attr-name new-val operation]
   (let [update-ts (next-ts db)
         layer (last (:layers db))
         attr (attr-at db ent-id attr-name)
         updated-attr (update-attr attr new-val update-ts operation)
         fully-updated-layer (update-layer layer ent-id
                                           attr updated-attr
                                           new-val operation)]
     (update-in db [:layers] conj fully-updated-layer))))

(defn- remove-back-refs [db e-id layer]
  (let [reffing-datoms (reffing-to e-id layer)
        remove-fn (fn [d [e a]] (update-entity db e a e-id :db/remove))
        clean-db (reduce remove-fn db reffing-datoms)]
    (last (:layers clean-db))))

(defn remove-entity [db ent-id]
  (let [ent (entity-at db ent-id)
        layer (remove-back-refs db ent-id (last (:layers db)))
        no-ref-layer (update-in layer [:VAET] dissoc ent-id)
        no-ent-layer (assoc no-ref-layer :storage
                            (drop-entity
                             (:storage no-ref-layer) ent))
        new-layer (reduce (partial remove-entity-from-index ent)
                          no-ent-layer (indexes))]
    (assoc db :layers (conj  (:layers db) new-layer))))

;; Function that returns a sequence of pairs, each consisting of the timestamp and value of an attribute’s update.
(defn evolution-of [db ent-id attr-name]
  (loop [res [] ts (:curr-time db)]
    (if (= -1 ts) (reverse res)
        (let [attr (attr-at db ent-id attr-name ts)]
          (recur (conj res {(:ts attr) (:value attr)}) (:prev-ts attr))))))
