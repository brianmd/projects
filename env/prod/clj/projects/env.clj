(ns projects.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[projects started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[projects has shut down successfully]=-"))
   :middleware identity})
