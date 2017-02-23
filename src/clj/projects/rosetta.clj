(ns projects.rosetta
  (:require [clojure.string :as string]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.walk :as walk]

            [projects.crud :as crud]
            ;; [datascript.core :as d]
            ))

;; (def rosetta-schema {:aka {:db/cardinality :db.cardinality/many}
;;                      :mfr/name {:db/unique :db.unique/identity}})
;; (def rosetta (d/create-conn rosetta-schema))


;; (d/transact! rosetta [ { :db/id -1
;;                         :mfr/name  "Maksim"
;;                         :mfr/age   45
;;                         :mfr/aka   ["Max Otto von Stierlitz", "Jack Ryan"] } ])
;; (d/transact! rosetta [{:db/id -1
;;                        :mfr/name  "3M"
;;                        :mfr/age   102
;;                        :mfr/aka   ["Tape", "Electrical"] } ])
;; (d/transact! rosetta [{:db/id 1
;;                        :order/line-items [{:lineItem/product "abc"
;;                                            :lineItem/quantity 2
;;                                            :lineItem/id 17}
;;                                           {:lineItem/product "def"
;;                                            :lineItem/quantity 4
;;                                            :lineItem/id 23}
;;                                           ]}])

;; ;; ;; return the whole database
;; ;; (d/q '[:find ?e ?a ?v
;; ;;        :where [?e ?a ?v]]
;; ;;      @rosetta)

;; ;; (defn entity->hash [entity-id]
;; ;;   "return specific entity as a hash"
;; ;;   (into {}
;; ;;         (d/q '[:find ?attr ?value
;; ;;                :in $ ?entity-id
;; ;;                :where [?entity-id ?attr ?value]]
;; ;;              @rosetta
;; ;;              entity-id)))
;; ;; (entity->hash 1)
;; ;; (entity->hash [:mfr/name "Maksim"])    ;; this one is shocking!!

;; ;; ;; note: this isn't necessary as the above works!
;; ;; (defn entity->hash2 [entity-attr entity-value]
;; ;;   "return specific entity as a hash"
;; ;;   (into {}
;; ;;         (d/q '[:find ?attr ?value
;; ;;                :in $ ?a ?v
;; ;;                :where
;; ;;                [?eid ?a ?v]
;; ;;                [?eid ?attr ?value]]
;; ;;              @rosetta
;; ;;              entity-attr entity-value)))
;; ;; (entity->hash2 :mfr/name "Maksim")

;; ;; ;; list all attribute names for entities that also have :mfr/name
;; ;; (d/q '[:find ?attr :where [?entity :mfr/name] [?entity ?attr]] @rosetta)

;; ;; ;; using a filter
;; ;; (d/q '[:find ?entity-id ?a ?v
;; ;;        :where
;; ;;        [?entity-id :mfr/age ?age]
;; ;;        [?entity-id ?a ?v]
;; ;;        [(<= 21 ?age)]]
;; ;;      @rosetta)

;; ;; (d/q '[ :find  ?e ?a ?v
;; ;;        :where [?e ?a ?v]]
;; ;;      @rosetta)
;; ;; (d/q '[ :find  ?a ?v
;; ;;        :where [1 ?a ?v]]
;; ;;      @rosetta)
;; ;; (-> ;; find aliases for entity id = 1
;; ;;  (d/q '[ :find  ?v
;; ;;         :where [1 :mfr/aka ?v]]
;; ;;       @rosetta)
;; ;;  vec
;; ;;  flatten
;; ;;  )
;; ;; (into {}   ;; note: this retains only one aka
;; ;;       (d/q '[ :find  ?a ?v
;; ;;              :where [1 ?a ?v]]
;; ;;            @rosetta))

;; ;; (->
;; ;;  (d/q '[:find ?v
;; ;;         :in $ ?entity-id
;; ;;         :where [?entity-id :order/line-items ?v]]
;; ;;       @rosetta
;; ;;       1)
;; ;;  first
;; ;;  first
;; ;;  first
;; ;; :lineItem/product
;; ;;  )


;; ;; (let [schema {:aka {:db/cardinality :db.cardinality/many}}
;; ;;       conn   (d/create-conn schema)]
;; ;;   (d/transact! conn [ { :db/id -1
;; ;;                        :name  "Maksim"
;; ;;                        :age   45
;; ;;                        :aka   ["Max Otto von Stierlitz", "Jack Ryan"] } ])
;; ;;   (d/q '[ :find  ?e ?a ?v
;; ;;          :where [?e ?a ?v]]
;; ;;        @conn))

;; ;; (let [schema {:aka {:db/cardinality :db.cardinality/many}}
;; ;;       conn   (d/create-conn schema)]
;; ;;   (d/transact! conn [ { :db/id -1
;; ;;                         :name  "Maksim"
;; ;;                         :age   45
;; ;;                         :aka   ["Max Otto von Stierlitz", "Jack Ryan"] } ])
;; ;;   (d/q '[ :find  ?n ?a
;; ;;           :where [?e :aka "Max Otto von Stierlitz"]
;; ;;                  [?e :name ?n]
;; ;;                  [?e :age  ?a] ]
;; ;;        @conn))

;; ;; (d/q '[:find ?e ?a ?v
;; ;;        [?e ?a ?v]])

;; ;; (let [schema {:aka {:db/cardinality :db.cardinality/many}}
;; ;;       conn   (d/create-conn schema)]
;; ;;   (d/q '[ :find  ?n ?a
;; ;;          :where [?e :aka "Max Otto von Stierlitz"]
;; ;;          [?e :name ?n]
;; ;;          [?e :age  ?a] ]))

;; ;; (d/q '[ :find  ?k ?x
;; ;;         :in    [[?k [?min ?max]] ...] ?range
;; ;;         :where [(?range ?min ?max) [?x ...]]
;; ;;                [(even? ?x)]
;; ;;        ]
;; ;;      { :a [1 7], :b [2 4] }
;; ;;      range)



;; ;; (def rosetta-url "http://marketing-10.insummit.com:3004")

;; ;; (defn retrieve-json
;; ;;   [uri]
;; ;;   (let [url (str rosetta-url uri)
;; ;;         response (http/get url)]
;; ;;     (if (= 200 (:status response))
;; ;;       (-> (:body response) json/parse-string walk/keywordize-keys)
;; ;;       response)))

;; (defn of-type
;;   [type v]
;;   (filter #(= (name type) (:type %)) (:included v)))

;; (def x (retrieve-json "/api/v2/projects/3"))
;; (let [
;;       project-id
;;       (-> x :data :id)
;;       project-name
;;       (-> x :data :attributes :name)
;;       account-id
;;       (-> x :data :relationships :account :data :id)
;;       project-order-ids
;;       (map :id (-> x :data :relationships :project-orders :data))
;;       project-drawing-ids
;;       (map :id (-> x :data :relationships :drawings :data))
;;       project-circuit-ids
;;       (map :id (-> x :data :relationships :circuits :data))

;;       project-line-item-ids
;;       (->
;;        (map (fn [order] (map :id (-> order :relationships :project-line-items :data )))
;;             (of-type :project-order x)
;;             )
;;        flatten
;;        set)
;;       ])

;; (:included x)
;; (filter #(= "project-order" (:type %)) (:included x))
;; (of-type :project-order x)
;; (of-type :account x)
;; (-> (of-type :project-order x) first :relationships)
;; ;; (map (fn [order] (-> order :relationships :project-line-items :data :id))
;; (->
;;  (map (fn [order] (map :id (-> order :relationships :project-line-items :data )))
;;       (of-type :project-order x)
;;       )
;;  flatten
;;  set)







