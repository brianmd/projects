(ns projects.neo4j
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.labels :as labels]
            [clojurewerkz.neocons.rest.relationships :as relations]
            [clojurewerkz.neocons.rest.cypher :as cypher]
            ;; [clojurewerkz.neocons.rest.batch :as batch]

            [clojure.spec :as s]

            [projects.repo :as repo]
            [projects.keys :as k]
            ))

;; (s/def ::str #(string? %))
;; (s/def ::map #(map? %))
;; (s/def ::int #(integer? %))
;; (s/def ::id ::int)
;; (s/def ::user-repo (s/keys :req-un [::repo ::customer-id]))
;; (s/def ::audited (s/keys :opt [:k/creator :k/created-on :k/updater :k/updated-on]))
;; (s/def ::release-map (s/merge
;;                       ::audited
;;                       (s/keys :req [:k/id :k/name :k/project-id]
;;                               :opt [:k/created])))

(defn repo-from
  [user-repo]
  (:repo user-repo))

(defn add-label
  ([user-repo label] (fn [node] (add-label user-repo label node)))
  ([user-repo label node]
   (labels/add (repo-from user-repo) node label)))

(defn create-node
  ([user-repo label] (fn [m] (create-node user-repo label m)))
  ([user-repo label m]
   {:pre [(s/valid? :specs/user-repo user-repo)
          (s/valid? :specs/str label)
          (s/valid? :specs/map m)]}
   (let [node (cypher/tquery (:repo user-repo) (str "CREATE (n:" label " {data}) RETURN n") { "data" m})]
     (-> node first (get "n")))))
   ;; (let [node (nn/create (repo-from user-repo) m)]
   ;;   (if node
   ;;     (do
   ;;       (add-label user-repo label node)
   ;;       node)
   ;;     (throw node)))))

(defn create-nodes
  "optimization: use create batch"
  [user-repo label v]
  (doseq [m v]
    (create-node user-repo label m)))

(defn cypher-query
  [user-repo query]
  (cypher/tquery (repo-from user-repo) query))
