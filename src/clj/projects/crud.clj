(ns projects.crud
  ;; (:refer-clojure :exclude [find])
  (:require [clojure.spec :as s]

            [clojure.tools.logging :as log]



            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.labels :as labels]
            [projects.repo :as repo]

            [clojure.walk :as walk]




            [clojure.spec :as s]
            [summit.spec :as spec]

            ;; [projects.keys :as k]
            [projects.neo4j :as n]
            [projects.json-api :as j]

            [clojurewerkz.neocons.rest.relationships :as relations]
            [clojurewerkz.neocons.rest.cypher :as cypher]))

;; helper functions

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

(defn releases
  "get all releases (id, name) for specified project"
  [user-repo project-id]
  (map (comp first vals)
       (n/process-query user-repo "match (p:ProjectProxy {id: {id}})-[]->(n:Release) return n" {"id" project-id})))

(defn release-create
  "create new release against project"
  [user-repo sap-project-id requestor-id m]
  (let [query "merge (project:ProjectProxy:SAPProxy {id: {projectId}}) MERGE (requestor:Customer {id: {requestorId}}) create (project)-[l:release]->(n:Release {data})-[r:requestor]->(requestor) return n"
        id (make-id :Release)
        data {:projectId sap-project-id :requestorId requestor-id :data (assoc m :id id)}]
    (n/process-query-n user-repo query data)))

(defn release
  [user-repo id]
  (n/node-plus-relationships user-repo id "Release"))

(defn release-line-item
  [user-repo id]
  (n/node-plus-relationships user-repo id "ReleaseLineItem"))


(defn release-update
  [user-repo release-id m]
  (let [query "match (n:Release {id: {id}}) set n += {data} return n"
        data (dissoc m :id)]
    (n/process-query-n user-repo query {:id release-id :data data}))
  )

(defn release-update-state
  [user-repo release-id state]
  {:pre [(s/valid? ::user-repo user-repo)]}
  )

(defn release-delete
  [user-repo release-id]
  (let [query "match (n:Release {id: {id}})-[]->(li:ReleaseLineItem) detach delete n,li"
        data {"id" release-id}]
    (n/process-query user-repo query data)))

(defn release-line-item-create
  [user-repo release-id project-line-item-id m]
  (let [query "match (r:Release {id: {id}}) create unique (r)-[l:lineItem]->(rli:ReleaseLineItem {data})-[l2:projectLineItem]->(:ProjectLineItemProxy:SAPProxy {pliData}) return rli"
        data {"id" release-id "data" (assoc m :id (make-id "ReleaseLineItem")) "pliData" {:type "projectlineitem" :id project-line-item-id}}]
    (n/process-query user-repo query data)))


(defn release-line-item-update
  [user-repo release-line-item-id m]
  (let [query "match (n:ReleaseLineItem {id: {id}}) set n += {data} return n"
        data (dissoc m :id)]
    (n/process-query-n user-repo query {:id release-line-item-id :data data})))

(defn release-line-item-delete
  [user-repo release-line-item-id]
  (n/delete-node user-repo release-line-item-id "ReleaseLineItem"))

