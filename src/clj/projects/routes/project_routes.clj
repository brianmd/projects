(ns projects.routes.project-routes
  (:require [projects.layout :as layout]
            [compojure.core :refer [defroutes routes GET POST PUT PATCH DELETE]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]

            [projects.repo :refer [repo]]
            [projects.crud :as crud]
            ))

;; (crud/releases {:repo repo} 3)
;; (read-string "3")

(defn make-repo
  [req]
  {:repo repo})

(defn project-routes
  [version]
  (routes
   (GET "/ok" []
        (-> (response/ok {:ok true})
            (response/header "Content-Type" "application/json; charset=utf-8")))
   (GET "/releases/:id" [id]
        (-> (response/ok {:data (crud/releases (make-repo nil) (read-string id))})
            (response/header "Content-Type" "application/json; charset=utf-8")))
   (GET "/release/:id" [id]
        (-> (response/ok {:data (crud/release (make-repo nil) (read-string id))})
            (response/header "Content-Type" "application/json; charset=utf-8")))
        ;; (-> (response/ok {:ok true})
        ;;     (response/header "Content-Type" "application/text; charset=utf-8")))
        ;; (GET "/releases/:id" req
        ;;      :path-params [id :- Long]
        ;;      (crud/releases {:repo repo} id))
        ;; (GET "/docs" []
        ;;      (-> (response/ok (-> "docs/docs.md" io/resource slurp))
        ;;          (response/header "Content-Type" "text/plain; charset=utf-8")))
   ))
