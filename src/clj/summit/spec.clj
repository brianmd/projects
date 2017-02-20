(ns summit.spec
  (:require [clojure.spec :as s]
            ))

(def prefixes
  {:foaf  "http://xmlns.com/foaf/0.1/"
   :gr    "http://purl.org/goodrelations/v1#"
   :rdf   "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
   :rdfa  "http://www.w3.org/ns/rdfa#"
   :rdfdf "http://www.openlinksw.com/virtrdf-data-formats#"
   :rdfs  "http://www.w3.org/2000/01/rdf-schema#"
   :vcard "http://www.w3.org/2006/vcard/ns#"
   })

(s/def :spec/str #(string? %))
(s/def :spec/map #(map? %))
(s/def :spec/int #(integer? %))
(s/def :spec/non-negative #(<= 0 %))
(s/def :spec/whole (s/and :spec/int :spec/non-negative))
(s/def :spec/id :spec/int)

(s/def :spec/customer-id :spec/whole)

(s/def :spec/user-repo (s/keys :req-un [::repo ::customer-id]))
(s/def :spec/audited (s/keys :opt [:spec/creator :spec/created-on :spec/updater :spec/updated-on]))
(s/def :spec/release-map (s/merge
                      ::audited
                      (s/keys :req [:spec/id :spec/name :spec/project-id]
                              :opt [:spec/created])))
