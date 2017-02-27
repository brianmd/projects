(ns projects.schema
  (:require [datascript.core :as d]
            [mount.core :refer [defstate]]))

;; (defn do-one
;;   [x]
;;   (let [lbl (:relate/from-group-label x)]
;;     (assoc x
;;            :relate/from-group-label (second lbl)
;;            :relate/from-label (first lbl))))
;; (do-one {:relate/from-group-label [1 2]})

;; (#(let [lbl (:relate/from-group-label %)]
;;      (assoc %
;;             :relate/from-group-label (second lbl)
;;             :relate/from-label (first lbl))
;;      )
;;  {:relate/from-group-label [1 2]})

;; helpers

(defn build-one-map
  [header row]
  (let [zipped (into {} (map vector header row))
        from (:relate/from-label zipped)
        to (:relate/to-label zipped)]
    (cond-> zipped
      (sequential? from) (assoc :relate/from-group-label (second from))
      (sequential? from) (assoc :relate/from-label (first from))
      (not (sequential? from)) (assoc :relate/from-group-label from)

      (sequential? to) (assoc :relate/to-group-label (second to))
      (sequential? to) (assoc :relate/to-label (first to))
      (not (sequential? to)) (assoc :relate/to-group-label to)
      )))

(defn build-maps
  [rs]
  (let [header (first rs)]
    (map
     #(build-one-map header %)
     (rest rs))))


;; queries

(declare db)

(def relationship-query
  '[:find ?lbl .
    :in $ ?from ?attr ?to
    :where
    [?relationship :relate/from ?from]
    [?relationship :relate/to ?to]
    [?relationship ?attr ?lbl]
    ])

(defn find-relationship-value
  [from-class to-attr inverse-attr to-class]
  (let [lbl (d/q relationship-query
                 @db from-class to-attr to-class)]
    (if lbl
      lbl
      (d/q relationship-query
           @db :ProjectProxy inverse-attr :Release)
      )))

(defn find-label
  [from-class to-class]
  (find-relationship-value from-class :relate/to-label :relate/from-label to-class))
(defn find-group-label
  [from-class to-class]
  (find-relationship-value from-class :relate/to-group-label :relate/from-group-label to-class))
(defn find-cardinality
  [from-class to-class]
  (find-relationship-value from-class :relate/to-type :relate/from-type to-class))


;; build database

(def schema {})

(def relationships
  [[:relate/from :relate/from-label :relate/from-type :relate/to-type :relate/to-label :relate/to]
   [:AccountProxy :account :db.cardinality/one :db.cardinality/many [:project :projects] :ProjectProxy]
   [:ProjectProxy :project :db.cardinality/one :db.cardinality/many [:release :releases] :Release]
   [:Release :release :db.cardinality/one :db.cardinality/many [:lineItem :lineItems] :ReleaseLineItem]
   [:ReleaseLineItem :release-line-item :db.cardinality/one :db.cardinality/one :project-line-item :ProjectLineItemProxy]
   ])

(defstate
  ^{:on-reload :noop}
  db
  :start (let [db (d/create-conn schema)]
           (d/transact! db (#'build-maps relationships))
           db))
;; (mount.core/stop)
;; (mount.core/start)
;; db

;; (d/q '[:find ?e
;;        :where [?e ?a ?v]
;;        ]
;;      @db)
;; (d/q '[:find (pull ?e [:db/id :relate/from-label :relate/to-label])
;;        :where [?e ?a ?v]
;;        ]
;;      @db)
;; (d/entity @db 4)  ;; only shows :db/id; rest is returned lazily
;; (keys (d/entity @db 4))
;; (vals (d/entity @db 4))
;; (into {} (d/entity @db 4))


(comment
  (find-label :ProjectProxy :Release)
  (find-label :Release :ProjectProxy)
  (find-group-label :ProjectProxy :Release)
  (find-group-label :Release :ProjectProxy)
  (find-cardinality :ProjectProxy :Release)
  (find-cardinality :Release :ProjectProxy)
  )

