(ns projects.repo
  (:require [clojurewerkz.neocons.rest :as nr]
            ))

;; Note: username/password should be exist in NEO4J_LOGIN and NEO4J_PASSWORD.

(def repo (nr/connect (or (System/getenv "NEO4J_URL") "http://localhost:7474/db/data/")))

