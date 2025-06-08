(ns whitesnake.indexes)

;; A database index.
(defn indexes [] [:VAET :AVET :VEAT :EAVT])

;; Create a database index.
(defn make-index [from-eav to-eav usage-pred]
  (with-meta {} {:from-eav from-eav :to-eav to-eav :usage-pred usage-pred}))

(defn from-eav [index] (:from-eav (meta index)))
(defn to-eav [index] (:to-eav (meta index)))
(defn usage-pred [index] (:usage-pred (meta index)))
