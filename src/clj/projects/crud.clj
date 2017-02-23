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

   "CREATE CONSTRAINT ON (n:Release) ASSERT n.id IS UNIQUE"
   "CREATE CONSTRAINT ON (n:ReleaseLineItem) ASSERT n.id IS UNIQUE"
   ])

(declare project-create)
(defn init! [user-repo]
  (doseq [cmd initial-setup-commands]
    (n/process-query user-repo cmd))
  (project-create user-repo 5)
  )
;; (n/delete-all! user-repo)
;; (init! (n/->repo))


;; project functions

(defn project-create
  ([user-repo project-id]
   (n/process-query-n user-repo
                      "merge (n:ProjectProxy:SAPProxy {type: \"project\", id: {id}}) return n"
                      {"id" project-id})))


;; release functions

(defn releases
  "get all releases (id, name) for specified project"
  [user-repo project-id]
  (map (comp first vals)
       ;; (n/process-plain-query user-repo "match (p:ProjectProxy {id: {id}})-[]->(n:Release) return {id: id(n), name: n.name}" {"id" project-id})))
       ;; (n/process-plain-query user-repo "match (p:ProjectProxy {id: {id}})-[]->(n:Release) return {id: n.id, name: n.name}" {"id" project-id})))
       (n/process-query user-repo "match (p:ProjectProxy {id: {id}})-[]->(n:Release) return n" {"id" project-id})))

(defn release-create
  "create new release against project"
  [user-repo sap-project-id requestor-id m]
  (let [query "merge (project:ProjectProxy:SAPProxy {id: {projectId}}) MERGE (requestor:Customer {id: {requestorId}}) create (project)-[l:release]->(n:Release {data})-[r:requestor]->(requestor) return n"
        id (make-id :Release)
        data {:projectId sap-project-id :requestorId requestor-id :data (assoc m :id id)}]
    (n/process-query-n user-repo query data)))

(defn project
  [user-repo project-id]
  {:project (n/find-source-id user-repo project-id)
   ;; :release-line-items (map :rli (n/process-query user-repo "match (r:Release) where ID(r)={id} optional match (r)-[]->(rli:ReleaseLineItem) return rli" {"id" release-id}))
   })

(defn release
  [user-repo id]
  (n/node-plus-relationships user-repo id "Release"))

(defn release-line-item
  [user-repo id]
  (n/node-plus-relationships user-repo id "ReleaseLineItem"))



  ;; (let [release-project (n/process-query user-repo "match (r:Release {id: {id}}) optional match (p)-[]->(r) return p,r" {"id" release-id})]
  ;; (let [release-project (n/process-query user-repo "match (release:Release {id: {id}})<-[]-(project) return project,release" {"id" release-id})
  ;;       items
  ;;       (n/process-query user-repo "match (r:Release {id: {id}})-[:hasReleaseLineitem]->(rli:ReleaseLineItem)-[:hasProjectLineItem]->(pli) return rli,pli" {"id" release-id})
  ;;       ]
  ;;   (apply merge release-project)
  ;;   (map :rli items)
  ;;   items
  ;;   ))
;; (release user-repo release-id)
;; (n/node-plus-relationships user-repo release-id)
;; (n/node-plus-relationships user-repo release-id "Release")

(defn release-update
  [user-repo release-id m]
  {:pre [(s/valid? ::user-repo user-repo)]}
  )

(defn release-update-state
  [user-repo release-id state]
  {:pre [(s/valid? ::user-repo user-repo)]}
  )

(defn release-delete
  [user-repo release-id]
  {:pre [(s/valid? ::user-repo user-repo)]}
  )

(defn ensure-project-line-item
  [user-repo project-line-item-id]
  )

(defn release-line-item-create
  ;; map has qty
  [user-repo release-id project-line-item-id m]
  ;; (let [query "match (r) where ID(r)={id} create unique (r)-[l:hasReleaseLineitem]->(rli:ReleaseLineItem {data})-[l2:hasProjectLineItem]->(:ProjectLineItemProxy:SAPProxy {pliData}) return rli"
  (let [query "match (r:Release {id: {id}}) create unique (r)-[l:lineItems]->(rli:ReleaseLineItem {data})-[l2:projectLineItem]->(:ProjectLineItemProxy:SAPProxy {pliData}) return rli"
        data {"id" release-id "data" (assoc m :id (make-id "ReleaseLineItem")) "pliData" {:type "projectlineitem" :id project-line-item-id}}]
    (n/process-query user-repo query data)))
;; (def y (release-line-item-create user-repo xid "1234-20" {:qty 4}))


(defn release-line-item-delete
  [user-repo release-line-item-id]
  (let [query "match (n) where ID(n)={id} detach delete n"
        data {"id" release-line-item-id}]
    (n/process-query user-repo query data)))




(def release-line-item-update n/update-node)
(def release-line-item-delete n/delete-node)



;; (releases user-repo 3)

;; (releases user-repo 28)
;; (process-query user-repo "match (r) where ID(r)=32 return r")

;; (init! user-repo)
;; (n/delete-all! user-repo)
;; (def x (release-create user-repo 3 {:name "robot"}))
;; (def xid 1)
;; (release user-repo xid)
;; (n/find user-repo xid)

;;   (n/view-node user-repo 2)


;; (clojure.pprint/pprint (n/process-plain-query user-repo "match (n) where ID(n)={key} return n" {"key" xid}))
;; (clojure.pprint/pprint (-> (n/process-plain-query user-repo "match (n) where ID(n)={key} return n" {"key" xid}) first (get "n") keys))
;; (n/find user-repo 10019)
;; (n/find-relationships user-repo xid)

;; (def r (n/get-node user-repo 1))
;; (def y (release-line-item-create user-repo xid "1234-30" {:qty 4}))
;; (def id (-> y first (get :rli) :id))
;; (n/delete-node-id user-repo id "ReleaseLineItem")
;; (release-line-item-update user-repo "1234-20" {:qty 7})
;; (n/update-node-id user-repo "ReleaseLineItem-65295424410379" {:qty 7} "ReleaseLineItem")

;; (def y (release-line-item-update user-repo 19433 {:id "123-10" :name :item2 :qty 4 :project-line-item-id "1234"}))

;; (release-create user-repo 3 {:name "release 1"})

;; (node-id x)
;; (def y (release-line-item-create user-repo (node-id x) {:id "123-10" :name :item2 :qty 4 :project-line-item-id "1234"}))
;; (node-id y)

;; (def y (release-line-item-update user-repo 22 {:qty 7}))
;; (release-line-item-update user-repo 22 {:qty 7})
;; (delete-node user-repo 6)

;; (def xyz {"id" 3 "label" "Release" "nodeData" {:sourceId 3}})
;; (try
;;   (node-id
;;    (n/process-query user-repo
;;                   (if-let [label (get xyz "label")]
;;                     ;; (str "match (n:" label " {sourceId: {id}}) return n")
;;                     (str "match (n:" label " {sourceId: {id}}) return n")
;;                     "match (n {sourceId: {id}}) return n.id")
;;                   xyz
;;                   ))
;;   (catch Throwable e
;;     (str e)
;;     ))

;; (defn get-data
;;   [data-element]
;;   (:data (data-element ")n")))

;; (def x
;;   (release-create
;;    user-repo
;;    {:k/id 7 :k/name "lll" :k/project-id 3}))
;; (:id x)
;; (-> x :metadata :id)
;; (keys x)
;; (prn x)
;; (:data x)
;; (nn/get (n/->repo user-repo) (:id x))
;; (nn/get (n/->repo user-repo) 2)
;; (nn/get (n/->repo user-repo) 2)
;; (-> (nn/get (n/->repo user-repo) (:id x)) :data)

;; (def y (nn/get (n/->repo user-repo) 2))
;; (def z (nn/get (n/->repo user-repo) 4))
;; (:id y)
;; (-> y keys)
;; (-> y prn)
;; (def yy (cypher/query (n/->repo user-repo) "START n = node({ids}) return n" { "ids" 2}))
;; (def yy (cypher/tquery (n/->repo user-repo) "START n = node({ids}) return n" { "ids" 2}))
;; (def yy (cypher/tquery (n/->repo user-repo) "START n=node({ids}) MATCH (n)-[:toasted]->(m) return m" { "ids" 2}))
;; (-> yy :data first first prn)
;; (-> yy first (get "n") keys)
;; (-> yy first (get "n") :metadata :id)
;; (relations/create (n/->repo user-repo) y z :toast {:x 9})
;; (relations/create (n/->repo user-repo) {:id 2} {:id 4} :toasty {:x 9})
;; (relations/create (n/->repo user-repo) 2 4 :toasted {:x 7})



;; (cypher/tquery (n/->repo user-repo) "CREATE (n:Auto {data}) RETURN n" { "data" {:name "Toby" :rank "Squire"}})

;; (cypher/tquery (n/->repo user-repo) (str "CREATE (n:" "Auto" " {data}) RETURN n") { "label" "Auto" "data" {:name "Arthur" :rank "King"}})
;; (cypher/tquery (n/->repo user-repo) (str "CREATE (n:" "Auto" " {data}) RETURN n" { "data" {:name "Arthur" :rank "King"}}))

;; (nn/update (n/->repo user-repo) (:id x) {:a 7})

;; (->>
;;  (n/cypher-query user-repo "match (n:Release) return n")
;;  ;; first
;;  (map get-data)
;;  ;; :k/id
;;  clojure.pprint/pprint
;;  )
;; (defn a
;;   []
;;   )


(def labelConversions
  ;;  label    to-node
  {[:Release :release :ProjectProxy]      :project
   [:ReleaseLineItem :lineItems :Release] :release
   })
;; (labelConversions [:lineItems :Release])
;; (get labelConversions [:lineItems :Release])

(defn release->json-api
  [repo n]
  (if (and n (map? n))
    (let [rli-ids (->> n :_meta :relationships :lineItems (map :id))
          rlis (map #(n/node-plus-relationships repo %) rli-ids)]
      (j/->json-api n rlis labelConversions)
      )
    (throw (ex-info "Not found in release->json-api" {:type :not-found}))))
