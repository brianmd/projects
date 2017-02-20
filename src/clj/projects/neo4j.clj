(ns projects.neo4j
  (:require
            [clojure.spec :as s]
            [clojure.walk :as walk]

            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.labels :as labels]
            [clojurewerkz.neocons.rest.relationships :as relations]
            [clojurewerkz.neocons.rest.cypher :as cypher]
            ;; [clojurewerkz.neocons.rest.batch :as batch]

            [summit.spec :as spec]

            [projects.repo :as repo]
            ;; [projects.keys :as k]
            ))

(defn node-id
  [n]
  (or (:id n) (-> n :metadata :id) (-> n first first second :id) (-> n first first second :metadata :id)))
;; (node-id x)

(defn relevant-info1
  [one-response]
  (into {}
        (map (fn [[k v]]
               (let [{:keys [:type :metadata :start :data]} v]
                 [(keyword k)
                  ;; {:id (:id metadata) :labels (:labels metadata) :data data}]))
                  (cond-> (merge data {:_meta metadata})
                    start (assoc-in [:_meta :type] :relationship)
                    (nil? start) (assoc-in [:_meta :type] :node)
                    type (assoc-in [:_meta :labels] [type]))]))
             one-response)))

(defn extract-essence
  [response]
  (map relevant-info1 response)
  )

(defn process-plain-query
  ([user-repo query] (process-plain-query user-repo query {}))
  ([{:keys [repo]} query data]
   (let [data (if data (walk/stringify-keys data) {})]
     (try
       (cypher/tquery repo query data)
       (catch Throwable e
         (let [s (str e)]
           (subs s 0 (min (count s) 600))))))))

(defn process-query
  ([user-repo query] (process-query user-repo query {}))
  ([{:keys [repo]} query data]
   (let [data (if data (walk/stringify-keys data) {})]
     (try
       (extract-essence
        (cypher/tquery repo query data))
       (catch Throwable e
         (let [s (str e)]
           (subs s 0 (min (count s) 600))))))))

(defn process-query-r
  "process query which should return one relationship named r"
  ([user-repo query data]
   (let [response (process-query user-repo query data)]
     (if (string? response)
       response
       (-> response first :n))))
  )

(defn process-query-n
  "process query which should return one node named n"
  ([user-repo query data]
   (let [response (process-query user-repo query data)]
     (if (string? response)
       response
       (-> response first :n))))
  )

(defn find-relationship
  "returns the map entry for key, or nil if key is not present (or does not match lebel)"
  ([user-repo key]
   (process-query-n user-repo "match (n1)-[n]-(n2) where ID(n)={key} return n" {"key" key}))
  ([user-repo key label]
   (let [query (str "match (n:" label ") where ID(n)={key} return n")]
     (process-query-n user-repo query {"key" key}))
   ))

(defn find
  "returns the map entry for key, or nil if key is not present (or does not match lebel)"
  ([user-repo key]
   (process-query-n user-repo "match (n) where ID(n)={key} return n" {"key" key}))
  ([user-repo key label]
   (let [query (str "match (n:" label ") where ID(n)={key} return n")]
     (process-query-n user-repo query {"key" key}))
   ))

(defn find-source-id
  "returns the map entry for key, or nil if key is not present (or does not match lebel)"
  ([user-repo source-id]
   (process-query-n user-repo "match (n {sourceId: {id}}) return n" {"id" source-id}))
  ([user-repo source-id label]
   (let [query (str "match (n:" label " {sourceId: {id}}) return n")]
     (process-query-n user-repo query {"id" source-id}))
   ))

(defn delete-all!
  [user-repo]
  (process-query user-repo "match (n) detach delete n"))


(defn create-node
  [user-repo m]
  )

(defn view-node
  [user-repo id m]
  (let [query "match (n) where ID(n)={id} set n += {data} return n"
        data {"id" id "data" m}]
    (process-query user-repo query data)))

(defn update-node
  [user-repo id m]
  (let [query "match (n) where ID(n)={id} set n += {data} return n"
        data {"id" id "data" m}]
    (process-query user-repo query data)))
;; (update-node projects.crud/user-repo 27 {"name" "LACC"})

(defn delete-node
  [user-repo id]
  (let [query "match (n) where ID(n)={id} detach delete n"
        data {"id" id}]
    (process-query user-repo query data)))


;; (def user-repo projects.crud/user-repo)

;; (delete-all! user-repo)
;; (def x (process-query user-repo "create (p:Project {sapProjectId: 3}) return p"))

;; (find-relationship user-repo 18)
;; (find user-repo 18)
;; (find user-repo 28 "Release")
;; (find-source-id user-repo 3)
;; (find-source-id user-repo 3 "Project")
;; (find user-repo 28 "Release")

;; (find-relationship user-repo 18)
;; (find user-repo 28)


;; (defn repo-from
;;   [user-repo]
;;   (:repo user-repo))

;; (defn add-label
;;   ([user-repo label] (fn [node] (add-label user-repo label node)))
;;   ([user-repo label node]
;;    (labels/add (repo-from user-repo) node label)))

;; (defn create-node
;;   ([user-repo label] (fn [m] (create-node user-repo label m)))
;;   ([user-repo label m]
;;    {:pre [(s/valid? :specs/user-repo user-repo)
;;           (s/valid? :specs/str label)
;;           (s/valid? :specs/map m)]}
;;    (let [node (cypher/tquery (:repo user-repo) (str "CREATE (n:" label " {data}) RETURN n") { "data" m})]
;;      (-> node first (get "n")))))
;;    ;; (let [node (nn/create (repo-from user-repo) m)]
;;    ;;   (if node
;;    ;;     (do
;;    ;;       (add-label user-repo label node)
;;    ;;       node)
;;    ;;     (throw node)))))

;; (defn create-nodes
;;   "optimization: use create batch"
;;   [user-repo label v]
;;   (doseq [m v]
;;     (create-node user-repo label m)))

;; (defn cypher-query
;;   [user-repo query]
;;   (cypher/tquery (repo-from user-repo) query))
