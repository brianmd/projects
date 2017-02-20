(ns projects.routes.home
  (:require [projects.layout :as layout]
            [compojure.core :refer [defroutes context GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]

            [projects.routes.project-routes :refer [project-routes]]
            ))

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" []
       (home-page))
  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
           (response/header "Content-Type" "text/plain; charset=utf-8")))
  (context "/api/:version" [version]
           (project-routes version))
  )

