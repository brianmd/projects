(ns projects.routes.project-routes
  (:require [projects.layout :as layout]
            [compojure.core :refer [defroutes routes GET POST PUT PATCH DELETE]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]

            [projects.repo :refer [repo]]
            [projects.crud :as crud]
            [projects.json-api :as j]
            ))

(defn make-repo
  [req]
  {:repo repo})

(defn json-204
  []
  {:body {}
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :status 204
   }


      ;; (response/header "Content-Type" "application/json; charset=utf-8")
      ;; (response/header "Access-Control-Allow-Origin" "*")
  )

(defn ok-json
  [json-data]
  (-> (response/ok json-data)
      (response/header "Content-Type" "application/json; charset=utf-8")
      ;; (response/header "Access-Control-Allow-Origin" "*")
      ))

(defn upsert-release
  [request data]
  (prn data)
  (let [attrs (:attributes data)
        project-id (-> data :relationships :project :data :id)
        requestor-id (-> data :relationships :requestor :data :id)
        id (:id data)
        ]
    (prn [id project-id requestor-id attrs])
    (if id
      (let [n (crud/release-update repo id attrs)]
        (clojure.pprint/pprint n)
        (ok-json {:data (j/node->json-api n)})
        )
      (let [n (crud/release-create repo project-id requestor-id attrs)]
        (clojure.pprint/pprint n)
        (ok-json {:data (j/node->json-api n)})
        )
      )))

(defn upsert-release-line-item
  [request data]
  (println "in upsert-release-line-item")
  (prn data)
  (let [attrs (:attributes data)
        release-id (-> data :relationships :release :data :id)
        project-line-item-id (-> data :relationships :project-line-item :data :id)
        id (:id data)
        ]
    (prn ["upsert-release-line-item" id release-id project-line-item-id attrs])
    (prn request)
    (if id
      (let [n (crud/release-line-item-update repo id attrs)
            ;; rli (crud/release-line-item repo (-> n first :id))
            ]
        (clojure.pprint/pprint n)
        ;; (ok-json {:data (j/node->json-api n)})
        (ok-json {:data (j/node->json-api n)})
        )
      (let [n (crud/release-line-item-create repo release-id project-line-item-id attrs)]
        (println "\n\ncreated release line item!!!!\n")
        (clojure.pprint/pprint n)
        ;; (clojure.pprint/pprint rli)
        (ok-json {:data (j/node->json-api n)})
        )
      )))

;; (crud/releases nil "3")
(defn project-routes
  [version]
  (routes
   (GET "/ok" []
        (ok-json {:ok true}))
   (GET "/releases" []
        (let [id "3"
              data (map j/node->json-api (crud/releases (make-repo nil) (read-string id)))]
          (println "\n\nasdf \n")
          (ok-json {:data data})))
   (GET "/projects/:id/releases" [id]
        (let [_ (println "project-id" id)
              data (crud/releases (make-repo nil) (read-string id))
              _ (prn data)
              json-api-data (map j/node->json-api data)]
          (ok-json {:data data})))
   (GET "/releases/:id" [id]
        (let [id (read-string id)
              node (#'crud/release (make-repo nil) id)
              json-data (#'crud/release->json-api repo node)
              ]
          (ok-json json-data)))
   (POST "/releases" [data :as request]
         (println "\n\nin post releases\nHere is the incoming data")
         (prn data)
         (prn request)
         (upsert-release request data)
         )
   (PATCH "/releases/:id" [id data :as request]
          (println "\n\nin patch releases\n")
          (prn data)
          (upsert-release request data)
          )
   (DELETE "/releases/:id" [id :as request]
           (println "\n\nin delete release\n")
           (prn id)
           ;; (let [id (:id data)]
             (crud/release-delete repo id)
             (json-204)
             ;; )
           )

   (POST "/release-line-items" [data :as request]
         (println "\n\nin POST release-line-items\nHere is the incoming data:")
         (prn data)
         (upsert-release-line-item request data)
         )
   (PATCH "/release-line-items/:id" [id data :as request]
          (println "\n\nin patch release-line-items\n")
          (prn data)
          (upsert-release-line-item request data)
          )
   (DELETE "/release-line-items/:id" [id :as request]
           (println "\n\nin delete release-line-items\n")
           ;; (prn data)
           ;; (let [id (:id data)]
             (crud/release-line-item-delete repo id)
             (json-204)
         )
   ))


;; (let [inc-xf (comp (map inc)
;;                    (filter even?))
;;       rf (inc-xf conj)]
;;   (rf (reduce rf (rf) [1 2 3 4])))
;; (let [inc-xf (comp (map inc)
;;                    (filter even?))
;;       rf (inc-xf conj)]
;;   (reduce rf (rf) [1 2 3 4]))
;; (let [rf ((comp (map inc)
;;                 (filter even?))
;;           conj)]
;;   (reduce rf (rf) [1 2 3 4]))

;; (reduce (comp conj inc) [] (range 3))
;; (reduce + (range 3))
