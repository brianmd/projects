(ns projects.crud-test
  (:require [projects.crud :as crud]
            [projects.neo4j :as n]
            [projects.json-api :as j]
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



(n/delete-all! repo)
(def x (crud/release-create repo 3 {:name "robot"}))
(def x2 (crud/release-create repo 3 {:name "tomorrow"}))
;; {:name "robot", :id "Release-1275449831043", :_meta {:id 1, :labels ["Release"], :type :node, :class :Release}}

(def rid (-> x :id))
(def y (crud/release-line-item-create repo rid "1234-10" {:qty 7}))
(def y2 (crud/release-line-item-create repo rid "1234-20" {:qty 4}))




(crud/releases repo 3)
(crud/release repo rid)
(clojure.pprint/pprint (crud/release repo rid))
(n/node-plus-relationships repo rid)
(n/node-plus-relationships repo rid "Release")

(def rls (crud/release repo rid))
(clojure.pprint/pprint (crud/release->json-api repo rls))




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
