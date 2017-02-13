(ns projects.crud
  (:require [clojure.spec :as s]




            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.labels :as labels]
            [projects.repo :as repo]




            [clojure.spec :as s]

            [projects.keys :as k]
            [projects.neo4j :as n]

            [clojurewerkz.neocons.rest.relationships :as relations]
            [clojurewerkz.neocons.rest.cypher :as cypher]))

(def initial-setup-commands
  ["CREATE CONSTRAINT ON (project:Project) ASSERT project.sapProjectId IS UNIQUE"
   "create (p:Project {sapProjectId: 3}) return p"
   ])

(s/def ::str #(string? %))
(s/def ::map #(map? %))
(s/def ::user-repo (s/keys :req-un [::customer-id ::repo]))
(s/def ::audited (s/keys :opt [:k/creator :k/created-on :k/updater :k/updated-on]))
(s/def ::release-map (s/merge
                      ::audited
                      (s/keys :req [:k/id :k/name :k/project-id]
                              :opt [:k/created])))

;; (s/valid? ::user-repo {:customer-id 3 :repo 4})
;; (s/valid? ::release-map {:k/id 2 ::name 4 :a 4})

(defn releases
  "get all releases (id, name) for specified project"
  [user-repo project-id]
  {:pre [(s/valid? ::user-repo user-repo)]}
  )

(defn release
  "get release"
  [user-repo release-id]
  {:pre [(s/valid? ::user-repo user-repo)]}
  )

(defn release-create
  "create new release"
  [user-repo sap-project-id m]
  ;; {:pre [(s/valid? ::user-repo user-repo)
  ;;        (s/valid? ::release-map m)
  ;;        ]}
  (let [cyp "match (project:Project {sapProjectId: {sapProjectId}}) create (project)-[l:hasRelease]->(r:Release {data}) return project,l,r"]
    (cypher/tquery (:repo user-repo) cyp {"sapProjectId" sap-project-id "data" m})))

(release-create user-repo 3 {:name :boo :a 4})

(defn get-data
  [data-element]
  (:data (data-element "n")))

(def user-repo
  {:repo repo/repo :customer-id 28}
  )
(def x
  (release-create
   user-repo
   {:k/id 7 :k/name "lll" :k/project-id 3}))
(:id x)
(-> x :metadata :id)
(keys x)
(prn x)
(:data x)
(nn/get (:repo user-repo) (:id x))
(nn/get (:repo user-repo) 2)
(-> (nn/get (:repo user-repo) (:id x)) :data)

(def y (nn/get (:repo user-repo) 2))
(def z (nn/get (:repo user-repo) 4))
(:id y)
(-> y keys)
(-> y prn)
(def yy (cypher/query (:repo user-repo) "START n = node({ids}) return n" { "ids" 2}))
(def yy (cypher/tquery (:repo user-repo) "START n = node({ids}) return n" { "ids" 2}))
(def yy (cypher/tquery (:repo user-repo) "START n=node({ids}) MATCH (n)-[:toasted]->(m) return m" { "ids" 2}))
(-> yy :data first first prn)
(-> yy first (get "n") keys)
(-> yy first (get "n") :metadata :id)
(relations/create (:repo user-repo) y z :toast {:x 9})
(relations/create (:repo user-repo) {:id 2} {:id 4} :toasty {:x 9})
(relations/create (:repo user-repo) 2 4 :toasted {:x 7})



(cypher/tquery (:repo user-repo) "CREATE (n:Auto {data}) RETURN n" { "data" {:name "Toby" :rank "Squire"}})

(cypher/tquery (:repo user-repo) (str "CREATE (n:" "Auto" " {data}) RETURN n") { "label" "Auto" "data" {:name "Arthur" :rank "King"}})
(cypher/tquery (:repo user-repo) (str "CREATE (n:" "Auto" " {data}) RETURN n" { "data" {:name "Arthur" :rank "King"}}))

(nn/update (:repo user-repo) (:id x) {:a 7})

(->>
 (n/cypher-query user-repo "match (n:Release) return n")
 ;; first
 (map get-data)
 ;; :k/id
 clojure.pprint/pprint
 )

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

(defn release-line-item-create
  [user-repo release-id m]
  ;; :req #{::project-line-item-id ::release-id}
  ;; {:pre [(s/valid? ::user-repo user-repo)]}
  (let [cyp "match (r) where ID(r)={id} create (r)-[l:releaseLineitem]->(rli:ReleaseLineItem {data}) return r,l,rli"]
    (cypher/tquery (:repo user-repo) cyp {"id" release-id "data" m})))
;; (release-line-item-create user-repo 11 {:name :boo :a 4})

  ;; (cypher/tquery (:repo user-repo) "CREATE (release:Release) (n:ReleaseLineItem {data}) RETURN n" { "data" {:name "Toby" :rank "Squire"}})
  ;; )

(defn release-line-item-update
  [user-repo m]
  {:pre [(s/valid? ::user-repo user-repo)]}
  )

(defn release-line-item-delete
  [user-repo m]
  {:pre [(s/valid? ::user-repo user-repo)]}
  )

(defn a
  []
  )

