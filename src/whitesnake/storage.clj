(ns whitesnake.storage)

;; Protocol for managing db storage.
(defprotocol Storage
  (get-entity [storage e-id])
  (write-entity [storage entity])
  (drop-entity [storage entity]))

;; Db storage implementation using a map.
(defrecord InMemory [] Storage
           (get-entity [storage e-id] (e-id storage))
           (write-entity [storage entity] (assoc storage (:id entity) entity))
           (drop-entity [storage entity] (dissoc storage (:id entity))))
