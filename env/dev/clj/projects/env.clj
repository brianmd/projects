(ns projects.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [projects.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[projects started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[projects has shut down successfully]=-"))
   :middleware wrap-dev})
