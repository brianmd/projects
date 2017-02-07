(ns user
  (:require [mount.core :as mount]
            [projects.figwheel :refer [start-fw stop-fw cljs]]
            projects.core))

(defn start []
  (mount/start-without #'projects.core/http-server
                       #'projects.core/repl-server))

(defn stop []
  (mount/stop-except #'projects.core/http-server
                     #'projects.core/repl-server))

(defn restart []
  (stop)
  (start))


