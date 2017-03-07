(ns projects.crud-test
  (:require [projects.crud :as crud]
            [projects.neo4j :as n]
            [projects.json-api :as j]
            [clj-http.client :as http]
            [clojure.test :refer :all]))

;; (deftest test-app
;;   (testing "main route"
;;     (let [response ((app) (request :get "/"))]
;;       (is (= 200 (:status response)))))

;;   (testing "not-found route"
;;     (let [response ((app) (request :get "/invalid"))]
;;       (is (= 404 (:status response))))))

(def repo {:repo (n/default-repo) :customer-id 28})
;; (def repo {:repo (n/->repo) :customer-id 28})
;; (def repo {:repo projects.repo/repo :customer-id 28})

(deftest read-release
  (testing "release id -1 should not exist (after deleting)"
    (crud/release-delete-all -1)
    (let [r (crud/release (repo) -1)]
      (is (= nil r)))
    ))

;; (crud/project repo 5)
(crud/project-create repo 7)






(def repo nil)
(n/delete-all! repo)
(def x (crud/release-create repo 3 28 {:name "robot"}))
(def x2 (crud/release-create repo 3 28 {:name "drawing #123"}))
;; (def x3 (crud/release-create repo 3 28 {:name "drawing #456"}))
;; (def x4 (crud/release-create repo 3 28 {:name "drawing #789"}))
;; {:name "robot", :id "Release-1275449831043", :_meta {:id 1, :labels ["Release"], :type :node, :class :Release}}

(def rid (-> x :id))
(def rid "Release-100668671362083")
(project.routes/upsert-release-line-item
 nil {:attributes {:quantity 77}
      :relationships
      {:release {:data {:id rid}}
       :project-line-item {:data {:id "1234-10"}}}})
(def y (crud/release-line-item-create repo rid "1234-10" {:qty 7}))
(def y2 (crud/release-line-item-create repo rid "1234-20" {:qty 4}))
({:rli {:qty 7, :id "ReleaseLineItem-100679703131981", :_meta {:id 35, :labels ["ReleaseLineItem"], :type :node, :class :ReleaseLineItem}}})



{:attrs (:attributes create-request)
 :project-id (-> create-request :relationships :release :data :id)
 :requestor-id (-> create-request :relationships :requestor :data :id)}

(http/get
 "http://10.9.0.124:3000/api/v2/releases/Release-58077327383647"
 {:content-type :json
  :accept :json
  })

(def create-request {:type "release", :attributes {:name "test-create"}, :relationships {:release {:data {:type "project", :id 3}}, :requestor {:data {:type "customer", :id 28}}}})
(http/post
 "http://10.9.0.124:3000/api/v2/releases"
 {:content-type :json
  ;; :accept :json
  :form-params {:data create-request}
  ;; :headers {"Access-Control-Allow-Origin" "*"
  ;;           "Access-Control-Allow-Headers" "Content-Type"
  ;;           "Access-Control-Allow-Methods" "GET,POST,PUT,OPTIONS"}
  }
 )
(def update-request {:id "Release-58202621922838" :type "release", :attributes {:name "test-update"}, :relationships {:release {:data {:type "project", :id 3}}, :requestor {:data {:type "customer", :id 28}}}})
(http/post
 "http://10.9.0.124:3000/api/v2/releases"
 {:content-type :json
  :form-params {:data update-request}
  }
 )
(def create-li-request {:type "release", :attributes {:name "test-create"}, :relationships {:release {:data {:type "project", :id 3}}, :requestor {:data {:type "customer", :id 28}}}})

(crud/release nil "Release-58078116596729")
(crud/release-delete nil "Release-58078116596729")
(n/delete-node nil "ReleaseLineItem-58080544550500" "ReleaseLineItem")
(crud/release-line-item-delete nil "ReleaseLineItem-58080544550500")
(def jrls (j/node->json-api (n/node-plus-relationships nil "Release-47822831120348")))
{:attrs (:attributes jrls)
 :project-id (-> jrls :relationships :release :data :id)
 :requestor-id (-> jrls :relationships :requestor :data :id)}



(def p clojure.pprint/pprint)

(crud/releases repo 3)
(def rid (-> (crud/releases nil 3) first :id))
(crud/release repo rid)
(crud/release repo -1)
(clojure.pprint/pprint (crud/release repo rid))
(n/node-plus-relationships repo rid)
(n/node-plus-relationships repo -1)
(n/find nil -1)
(clojure.pprint/pprint (n/node-plus-relationships repo rid))
(clojure.pprint/pprint (n/node-plus-relationships repo -1))
(n/node-plus-relationships repo rid "Release")
"Release-36136766693960"


(j/->json-api (n/node-plus-relationships repo rid) [])
(j/node->json-api (n/node-plus-relationships repo rid))
(p (j/node->json-api (n/node-plus-relationships repo rid)))

(def rls (crud/release repo rid))
(n/find-relationships nil rid)
(clojure.pprint/pprint (crud/release->json-api repo rls))
(clojure.pprint/pprint (crud/release->json-api repo -1))




;; (def rid "Release-2615659335586")
;; (clojure.pprint/pprint (crud/release nil rid))
;; (def rli-id (-> (crud/release nil rid) :_meta :relationships first :id))
;; (def rls (crud/release nil rid))
;; (extract-relationships-by-type (-> (crud/release nil rid) :_meta :relationships))
;; (extract-relationships-by-type (n/relationships (crud/release nil rid)))
;; (node->json-api (crud/release nil rid))
;; (clojure.pprint/pprint (node->json-api (crud/release nil rid)))
;; (node->json-api (crud/release-line-item nil rli-id))

;; (release->json-api nil rls)
;; (clojure.pprint/pprint (release->json-api nil rls))
