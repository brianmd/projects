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
        project-id (-> data :relationships :release :data :id)
        requestor-id (-> data :relationships :requestor :data :id)
        id (:id data)
        ]
    (prn [id project-id requestor-id attrs])
    (if id
      (let [n (crud/release-update repo id attrs)]
        (clojure.pprint/pprint n)
        (ok-json {:id (:id n)})
        )
      (let [n (crud/release-create repo project-id requestor-id attrs)]
        (clojure.pprint/pprint n)
        (ok-json {:id (:id n)})
        )
      )))

(defn upsert-release-line-item
  [request data]
  (prn data)
  (let [attrs (:attributes data)
        release-id (-> data :relationships :release :data :id)
        project-line-item-id (-> data :relationships :project-line-item :data :id)
        id (:id data)
        ]
    (prn [id release-id project-line-item-id attrs])
    (if id
      (let [n (crud/release-line-item-update repo id attrs)]
        (clojure.pprint/pprint n)
        (ok-json {:id (:id n)})
        )
      (let [n (crud/release-line-item-create repo release-id project-line-item-id attrs)]
        (clojure.pprint/pprint n)
        (ok-json {:id (:id n)})
        )
      )))

(defn project-routes
  [version]
  (routes
   (GET "/ok" []
        (ok-json {:ok true}))
   (GET "/projects/:id/releases" [id]
        (let [data (map j/node->json-api (crud/releases (make-repo nil) (read-string id)))]
          (ok-json data)))
   (GET "/releases/:id" [id]
        (let [id (read-string id)
              node (#'crud/release (make-repo nil) id)
              json-data (#'crud/release->json-api repo node)
              ]
          (ok-json json-data)))
   (POST "/releases" [data :as request]
         (println "\n\nin post releases\n")
         (prn data)
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
             (crud/release-delete id)
             ;; )
           )

   (POST "/release-line-items" [data :as request]
         (println "\n\nin post release-line-items\n")
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
             (crud/release-line-item-delete id)
             ;; )
         )
   ))
