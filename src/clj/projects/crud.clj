(ns projects.crud
  ;; (:refer-clojure :exclude [find])
  (:require [clojure.spec :as s]

            ;; [clojure.tools.logging :as log]
            [mishmash.log :as log]
            [mount.core :refer [defstate]]



            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.labels :as labels]
            [projects.repo :as repo]

            [clojure.walk :as walk]

            [datascript.core :as d]
            [projects.schema :as schema]



            [clojure.spec :as s]
            [summit.spec :as spec]

            ;; [projects.keys :as k]
            [projects.neo4j :as n]
            [projects.json-api :as j]

            [clojurewerkz.neocons.rest.relationships :as relations]
            [clojurewerkz.neocons.rest.cypher :as cypher]
            [clojure.string :as str]
            ))

;; (def schema
;;   {:service/service-instance {:db/valueType :db.type/ref
;;                             :db/doc "definition of a service, including its uri prefix"}
;;    :service/instance-name {:db/unique :db.unique/identity
;;                          :db/doc "keyword name used to identity a service instance"}
;;    :proxy/from-uri {:db/cardinality :db.cardinality/many}
;;    })

;; (def labelConversions
;;   ;;  label    to-node
;;   {[:Release :release :ProjectProxy]      :project
;;    [:ReleaseLineItem :lineItems :Release] :release
;;    })


(def default-services
  [
   {:db/id -1
    :service/instance-name :blue-harvest.dev
    :service/service :blue-harvest
    :service/env :prod
    :service/uri "http://marketing-02.insummit.com:7442"}
   {:db/id -2
    :service/instance-name :blue-harvest.prod
    :service/service :blue-harvest
    :service/env :dev
    :service/uri "http://marketing-22.insummit.com:7442"}
   {:db/id -3
    :service/instance-name :project.brian
    :service/service :projects.10.9.0.124
    :service/env :brian
    :service/uri-prefix "http://10.9.0.124:3000"}
   ])

;; (d/transact! schema/db default-services)





;; helper functions

(defn ->int [o]
  (let [t (type o)]
    (condp = true
      (nil? o) 0
      (= t Integer) o
      (= t Long) o
      (number? o) (-> o Math/round int)
      (string? o)
      (let [v (str/trim o)]
        (if (empty? v)
          0
          (-> v Double/parseDouble Math/round int)))
      true (type o)
      )))

(defn now-nano []
  (System/nanoTime))

(defn make-id [label]
  (str (name label) "-" (now-nano)))

;; (def user-repo
;;   {:repo projects.repo/repo :customer-id 28}
;;   )


;; setup functions

(def initial-setup-commands
  ["CREATE CONSTRAINT ON (n:ProjectProxy) ASSERT n.id IS UNIQUE"
   "CREATE CONSTRAINT ON (n:ProjectLineItemProxy) ASSERT n.id IS UNIQUE"

   ;; ecommerce
   "CREATE CONSTRAINT ON (n:Customer) ASSERT n.id IS UNIQUE"
   "CREATE CONSTRAINT ON (n:Account) ASSERT n.id IS UNIQUE"

   ;; project
   "CREATE CONSTRAINT ON (n:Release) ASSERT n.id IS UNIQUE"
   "CREATE CONSTRAINT ON (n:ReleaseLineItem) ASSERT n.id IS UNIQUE"
   ])

(def constraint-labels
  [:ProjectProxy :ProjectLineItemProxy
   :Customer :Account
   :Release :ReleaseLineItem
   ])

(defn create-id-constraint-on
  [user-repo label]
  (n/process-query user-repo (str "CREATE CONSTRAINT ON (n:" (name label) ") ASSERT n.id IS UNIQUE")))

;; (create-id-constraint-on nil "Customer")
(declare project-create)
(defn init! [user-repo]
  ;; (doseq [cmd initial-setup-commands]
  ;;   (n/process-query user-repo cmd))
  (doseq [label constraint-labels]
    (create-id-constraint-on user-repo label))
  )
;; (n/delete-all! nil)
;; (init! (n/->repo))


;; project functions

;; (defn project-create
;;   ([user-repo project-id]
;;    (n/process-query-n user-repo
;;                       "merge (n:ProjectProxy:SAPProxy {type: \"project\", id: {id}}) return n"
;;                       {"id" project-id})))


;; release functions

(defn releases-light
  "get all releases (id, name) for specified project sans relationships"
  [user-repo project-id]
  (println "get releases for project " project-id)
  (let [data (n/process-query user-repo "match (p:ProjectProxy {id: {id}})-[]->(n:Release) return n" {"id" project-id})
        data (map (comp first vals) data)
        data (map (fn [n] {:type "release" :id (:id n) :attributes (dissoc n :_meta :id)}) data)
        ]
    data
    ))
;; (releases-light nil 3)


(declare release)

(defn releases
  "get all releases (id, name) for specified project"
  [user-repo project-id]
  (println "get releases for project " project-id)
  (let [data (n/process-query user-repo "match (p:ProjectProxy {id: {id}})-[]->(n:Release) return n" {"id" project-id})
        data (map (comp first vals) data)
        ;; data (map (fn [n] {:type "release" :id (:id n) :attributes (dissoc n :_meta :id)}) data)
        release-ids (map :id data)
        ]
    (map #(release user-repo %) release-ids)
    ))
;; (releases nil 3)

;; (defn releases
;;   "get all releases (id, name) for specified project"
;;   [user-repo project-id]
;;   (println "get releases for project " project-id)
;;   (let [data (n/process-query user-repo "match (p:ProjectProxy {id: {id}})-[release]->(n:Release)<-[requestor]-(a:Customer) return n,a" {"id" project-id})
;;         ;; data (map (comp first vals) data)
;;         ;; data (map (fn [n] {:type "release" :id (:id n) :attributes (dissoc n :_meta :id)}) data)
;;         ]
;;     data
;;     ))
;; (releases nil 3)

(defn release-create
  "create new release against project"
  [user-repo sap-project-id requestor-id m]
  (println "create release for project " sap-project-id)
  (let [query "merge (project:ProjectProxy:SAPProxy {id: {projectId}}) MERGE (requestor:Customer {id: {requestorId}}) create (project)-[l:release]->(n:Release {data})<-[r:requestor]-(requestor) return n"
        id (make-id :Release)
        data {:projectId (->int sap-project-id) :requestorId (->int requestor-id) :data (assoc m :id id)}]
    (n/process-query-n user-repo query data)))

(defn release
  [user-repo id]
  (println "get release id " id)
  (n/node-plus-relationships user-repo id "Release"))

(defn release-update
  [user-repo release-id m]
  (println "update release id " release-id)
  (let [query "match (n:Release {id: {id}}) set n += {data} return n"
        data (dissoc m :id)]
    (n/process-query-n user-repo query {:id release-id :data data}))
  )

;; (defn release-update-state
;;   [user-repo release-id state]
;;   {:pre [(s/valid? ::user-repo user-repo)]}
;;   )

;; (log/info ::test {:error? 3})
;; (log/log 3 (range 15))
;; (log/logp 3 (range 45))

(defn release-delete
  [user-repo release-id]
  (println "delete release id " release-id)
  (let [query "match (n:Release {id: {id}}) optional match (n)-[]->(li:ReleaseLineItem) detach delete n,li"
        data {"id" release-id}]
    (n/process-query user-repo query data)))

(defn release-line-item-create
  [user-repo release-id project-line-item-id m]
  (println "creating line item for release " release-id)
  (when (nil? release-id)
    (throw (Exception. (str "release-id not found: " release-id))))
  (let [query (str "match (r:Release {id: {id}})
merge (pli:ProjectLineItemProxy:SAPProxy {id: \"" project-line-item-id "\"})
create unique (r)-[l:lineItem]->(n:ReleaseLineItem {data})-[:projectLineItem]->(pli) return n")
        data {"id" release-id "data" (assoc m :id (make-id "ReleaseLineItem"))
              ;; "pliData" {:type "projectlineitem" :id project-line-item-id}
              }]
    (println "before" data)
    (prn query)
    (let [x
          (n/process-query-n user-repo query data)
          ]
      (println "finished ")
      (println "x:" x)
      x)
    ))


(defn release-line-item
  [user-repo id]
  (println "getting release line item " id)
  (n/node-plus-relationships user-repo id "ReleaseLineItem"))

(defn release-line-item-update
  [user-repo release-line-item-id m]
  (println "updating release line item " release-line-item-id)
  (let [query "match (n:ReleaseLineItem {id: {id}}) set n += {data} return n"
        data (dissoc m :id)]
    (n/process-query-n user-repo query {:id release-line-item-id :data data})))

(defn release-line-item-delete
  [user-repo release-line-item-id]
  (println "deleting release line item " release-line-item-id)
  (n/delete-node user-repo release-line-item-id "ReleaseLineItem"))


(def labelConversions
  ;;  label    to-node
  {[:Release :release :ProjectProxy]      :project
   [:ReleaseLineItem :lineItems :Release] :release
   })
;; (labelConversions [:lineItems :Release])
;; (get labelConversions [:lineItems :Release])
-
(defn release->json-api
  [repo n]
  (if (and n (map? n))
    (let [rli-ids (->> n :_meta :relationships :lineItem (map :id))
          rlis (map #(n/node-plus-relationships repo %) rli-ids)]
      (println "\n\nrlis" rlis "\n\n" rli-ids "\n\n")
      (clojure.pprint/pprint n)
      (j/->json-api n rlis labelConversions)
      )
    (throw (ex-info (str "Requested release not found") {:type :not-found}))))
