(ns projects.neo4j
  (:refer-clojure :exclude [find])
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

(defn merge-lists [& maps]
  (reduce (fn [m1 m2]
            (reduce (fn [m [k v]]
                      (update-in m [k] (fnil conj []) v))
                    m1, m2))
          {}
          maps))

(defn extract-relationships [n]
  (-> n :_meta :relationships))

(defn default-repo
  []
  projects.repo/repo)

(defn ->repo
  ([] (->repo nil))
  ([user-repo]
   (if user-repo
     (if (and (map? user-repo) (contains? user-repo :repo))
       (:repo user-repo)
       user-repo)
     (default-repo))))
;; (->repo)
;; (->repo projects.repo/repo)
;; (->repo {:repo projects.repo/repo})

(defn node-id
  [n]
  (or (:id n) (-> n :metadata :id) (-> n first first second :id) (-> n first first second :metadata :id)))
;; (node-id x)

(defn relevant-info1
  [one-response]
  (into {}
        (map (fn [[k v]]
               (let [{:keys [:type :metadata :start :data]} v
                     labels (-> metadata :labels)
                     class-names (and labels (-> labels set (disj "SAP" "SAPProxy")))
                     class-name (and class-names (= 1 (count class-names)) (-> class-names first keyword))]
                 [(keyword k)
                  ;; {:id (:id metadata) :labels (:labels metadata) :data data}]))
                  (cond-> (merge data {:_meta metadata})
                    start (assoc-in [:_meta :type] :relationship)
                    (nil? start) (assoc-in [:_meta :type] :node)
                    type (assoc-in [:_meta :labels] [type])
                    class-name (assoc-in [:_meta :class] class-name)
                    )]))
             one-response)))

(defn extract-essence
  [response]
  (map relevant-info1 response)
  )

(defn process-plain-query-nont
  "process the query but don't post-process the response"
  ([query] (process-plain-query nil query))
  ([user-repo query] (process-plain-query user-repo query {}))
  ([repo query data]
   (let [data (if data (walk/stringify-keys data) {})
         _ (prn query)
         _ (prn data)
         ]
     (try
       (cypher/query (->repo repo) query data)
       (catch Throwable e
         (let [s (str e)]
           (cond
             (re-find #"(?i)host url cannot be nil" s) (throw (ex-info "host url is nil" {:type :bad-host :error e :repo repo}))
             (re-find #"(?i)already exists" s) (throw (ex-info "already exists" {:type :already-exists :error s}))
             :else (throw (ex-info "unknown error" {:type :unhandled-error :error s}))
             ;; :else (subs s 0 (min (count s) 600))
             ))
         )))))

(defn process-plain-query
  "process the query but don't post-process the response"
  ([query] (process-plain-query nil query))
  ([user-repo query] (process-plain-query user-repo query {}))
  ([repo query data]
   (let [data (if data (walk/stringify-keys data) {})
         _ (prn query)
         _ (prn data)
         ]
     (try
       (cypher/tquery (->repo repo) query data)
       (catch Throwable e
         (let [s (str e)]
           (cond
             (re-find #"(?i)host url cannot be nil" s) (throw (ex-info "host url is nil" {:type :bad-host :error e :repo repo}))
             (re-find #"(?i)already exists" s) (throw (ex-info "already exists" {:type :already-exists :error s}))
             :else (throw (ex-info "unknown error" {:type :unhandled-error :error s}))
             ;; :else (subs s 0 (min (count s) 600))
             ))
         )))))

(defn process-query
  "extract essence out of process query"
  ([query] (process-query nil query))
  ([user-repo query] (process-query user-repo query {}))
  ([repo query data]
   (let [response (process-plain-query repo query data)]
     (if (sequential? response)
       (extract-essence response)
       response))))

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
     ;; response)))
     (if (string? response)
       response
       (-> response first :n)))))

(defn find-relationship
  "returns the map entry for key, or nil if key is not present (or does not match label)"
  ([user-repo key]
   (process-query-n user-repo "match (n1)-[n]-(n2) where ID(n)={key} return n" {"key" key}))
  ([user-repo key label]
   (let [query (str "match (n:" label ") where ID(n)={key} return n")]
     (process-query-n user-repo query {"key" key}))
   ))

(defn find-by-id
  "returns the map entry for key, or nil if key is not present (or does not match label)"
  ([user-repo key]
   (process-query-n user-repo "match (n) where ID(n)={key} return n" {"key" key}))
  ([user-repo key label]
   (let [query (str "match (n:" label ") where ID(n)={key} return n")]
     (process-query-n user-repo query {"key" key}))
   ))

(defn relate-one
  [nr]
  (let [relate-name (-> nr :r :_meta :labels first)
        ]
    {(keyword relate-name) (:n nr)}))

(defn find-relationships
  "returns the map entry for id, or nil if id is not present (or does not match label)"
  ([user-repo id]
   (let [rels (process-query user-repo "match (n1 {id: {id}})-[r]-(n) return n,r" {"id" id})]
     (apply merge-lists
      (map relate-one rels))))
  ([user-repo id label]
   (let [rels (process-query user-repo (str "match (n1:" label " {id: {id}})-[r]-(n) return n,r") {"id" id})]
     (apply merge-lists
            (map relate-one rels))))
  )
;; (find-relationships nil "Release-2615659335586")
;; {"hasReleaseLineitem"
;;  [{:qty 4, :id "ReleaseLineItem-2626487596465", :_meta {:id 19, :labels ["ReleaseLineItem"], :type :node, :class :ReleaseLineItem}}
;;   {:qty 7, :id "ReleaseLineItem-2625492262429", :_meta {:id 17, :labels ["ReleaseLineItem"], :type :node, :class :ReleaseLineItem}}],

;;  "hasRelease"
;;  [{:id 3, :_meta {:id 14, :labels ["ProjectProxy" "SAPProxy"], :type :node, :class :ProjectProxy}}]
;;  }


;; ([user-repo id label]
;;  ;; (let [query (str "match (n1:" label ")-[r]-(n) where ID(n1)={id} return n")]
;;  (let [query (str "match (n1:" label " {id: {id}})-[r]-(n) return n")]
;;    (map :n
;;         (process-query user-repo query {"id" id}))))

(defn find
  "returns the map entry for property id, or nil if id is not present (or does not match label)"
  ([user-repo id]
   (process-query-n user-repo "match (n {id: {id}}) return n" {"id" id}))
  ([user-repo id label]
   (let [query (str "match (n:" label " {id: {id}}) return n")]
     (process-query-n user-repo query {"id" id}))
   ))

(defn node-plus-relationships
  "returns the map entry for property id, or nil if id is not present (or does not match label)"
  ([user-repo id]
   (let [node (find user-repo id)]
     (when node
       (assoc-in node [:_meta :relationships]
                 (find-relationships user-repo id)))))
  ([user-repo id label]
   (let [node (find user-repo id label)]
     (when node
       (assoc-in node [:_meta :relationships]
                 (find-relationships user-repo id label))))))

;; (defn find-source-id
;;   "returns the map entry for key, or nil if key is not present (or does not match label)"
;;   ([user-repo source-id]
;;    (process-query-n user-repo "match (n {sourceId: {id}}) return n" {"id" source-id}))
;;   ([user-repo source-id label]
;;    (let [query (str "match (n:" label " {sourceId: {id}}) return n")]
;;      (process-query-n user-repo query {"id" source-id}))
;;    ))


(defn create-node
  [user-repo m]
  )

(defn get-node-by-id
  [user-repo id]
  (let [query "match (n) where ID(n)={id} set n += {data} return n"
        data {"id" id}]
    (process-query user-repo query data)))
;; (get-node-by-id (default-repo) 1 )

(defn update-node
  "update node with specified property id (property id, not node id)"
  ([user-repo id m]
   (let [query "match (n {id: {id}}) set n += {data} return n"
         data {"id" id "data" m}]
     (process-query user-repo query data)))
  ([user-repo id m label]
   (let [query (str "match (n:" label " {id: {id}}) set n += {data} return n")
         data {"id" id "data" m}]
     (process-query user-repo query data))))

(defn delete-node
  ([user-repo id]
   (let [query "match (n {id: {id}}) detach delete n"
         data {"id" id}]
     (process-plain-query user-repo query data)))
  ([user-repo id label]
   (let [query (str "match (n:" label " {id: {id}}) detach delete n")
         data {"id" id}]
     (process-plain-query user-repo query data))))

(defn delete-node-source-id
  "deletes node. throws error if node has relationships."
  [user-repo label source-id]
  (str "match (n:" label " {sourceId: {id}}) delete n"))

(defn delete-all!
  [user-repo]
  (process-query user-repo "match (n) detach delete n"))

