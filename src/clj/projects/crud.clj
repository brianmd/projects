(ns projects.crud
  (:refer-clojure :exclude [find])
  (:require [clojure.spec :as s]




            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.labels :as labels]
            [projects.repo :as repo]

            [clojure.walk :as walk]




            [clojure.spec :as s]
            [summit.spec :as spec]

            ;; [projects.keys :as k]
            [projects.neo4j :as n]

            [clojurewerkz.neocons.rest.relationships :as relations]
            [clojurewerkz.neocons.rest.cypher :as cypher]))

;; (def user-repo
;;   {:repo repo/repo :customer-id 28}
;;   )

(def initial-setup-commands
  ["CREATE CONSTRAINT ON (n:Project) ASSERT n.sourceId IS UNIQUE"
   "CREATE CONSTRAINT ON (n:ProjectLineItem) ASSERT n.sourceId IS UNIQUE"
   "create (p:Project {name: \"LACC\", sourceId: 3}) return p"
   ])

(defn init! [user-repo]
  (doseq [cmd initial-setup-commands]
    (cypher/tquery (:repo user-repo) cmd)))
(init! user-repo)

(defn releases
  "get all releases (id, name) for specified project"
  [user-repo project-id]
  (map (comp first vals)
       (n/process-plain-query user-repo "match (p:Project {sourceId: {id}})-[]->(n:Release) return {id: id(n), name: n.name}" {"id" project-id})))




(defn release-create
  "create new release against project"
  [user-repo sap-project-id m]
  (let [query "merge (project:Project {sourceId: {sourceId}}) create (project)-[l:hasRelease]->(n:Release {data}) return n"
        data {:sourceId sap-project-id :data m}]
    (n/process-query-n user-repo query data)))

(defn release
  [user-repo release-id]
  {:release (find user-repo release-id)
   :release-line-items (map :rli (n/process-query user-repo "match (r:Release) where ID(r)={id} optional match (r)-[]->(rli:ReleaseLineItem) return rli" {"id" release-id}))
   })

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
  [user-repo release-id project-line-item-id qty]
  (n/process-query user-repo "match (p:Project {sourceId: {id}} create unique )")
  (let [query "match (r) where ID(r)={id} create (r)-[l:releaseLineitem]->(rli:ReleaseLineItem {data}) return rli"
        data {"id" release-id "data" {:qty qty}}]
    (n/process-query user-repo query data)))


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

;; (def x (release-create user-repo 3 {:name "robot" :sourceId 3}))
;; (release-create user-repo 3 {:name "release 1"})

;; (node-id x)
;; (def y (release-line-item-create user-repo (node-id x) {:name :item2 :qty 4 :project-line-item-id "1234"}))
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
;; (nn/get (:repo user-repo) (:id x))
;; (nn/get (:repo user-repo) 2)
;; (-> (nn/get (:repo user-repo) (:id x)) :data)

;; (def y (nn/get (:repo user-repo) 2))
;; (def z (nn/get (:repo user-repo) 4))
;; (:id y)
;; (-> y keys)
;; (-> y prn)
;; (def yy (cypher/query (:repo user-repo) "START n = node({ids}) return n" { "ids" 2}))
;; (def yy (cypher/tquery (:repo user-repo) "START n = node({ids}) return n" { "ids" 2}))
;; (def yy (cypher/tquery (:repo user-repo) "START n=node({ids}) MATCH (n)-[:toasted]->(m) return m" { "ids" 2}))
;; (-> yy :data first first prn)
;; (-> yy first (get "n") keys)
;; (-> yy first (get "n") :metadata :id)
;; (relations/create (:repo user-repo) y z :toast {:x 9})
;; (relations/create (:repo user-repo) {:id 2} {:id 4} :toasty {:x 9})
;; (relations/create (:repo user-repo) 2 4 :toasted {:x 7})



;; (cypher/tquery (:repo user-repo) "CREATE (n:Auto {data}) RETURN n" { "data" {:name "Toby" :rank "Squire"}})

;; (cypher/tquery (:repo user-repo) (str "CREATE (n:" "Auto" " {data}) RETURN n") { "label" "Auto" "data" {:name "Arthur" :rank "King"}})
;; (cypher/tquery (:repo user-repo) (str "CREATE (n:" "Auto" " {data}) RETURN n" { "data" {:name "Arthur" :rank "King"}}))

;; (nn/update (:repo user-repo) (:id x) {:a 7})

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

