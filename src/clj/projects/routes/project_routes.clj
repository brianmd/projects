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
      (response/header "Access-Control-Allow-Origin" "*")))

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
   (POST "/releases" req
         (println "\n\nin post releases\n")
         (prn (str (:body req)))
         (ok-json {:ok true})
         )
   (PATCH "/releases/:id" [id]
         )
   (POST "/releases/:id/release-line-items" []
         )
   (PATCH "/releases/:id/release-line-items/:id" [id]
         )
   ))
