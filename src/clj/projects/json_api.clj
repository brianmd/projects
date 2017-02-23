(ns projects.json-api
  (:require [projects.crud :as crud]
            [projects.neo4j :as n]

            [clojure.string :as string]))

(defn merge-lists [& maps]
  (reduce (fn [m1 m2]
            (reduce (fn [m [k v]]
                      (update-in m [k] (fnil conj []) v))
                    m1, m2))
          {}
          maps))

(defn lower-first-char
  "converts PascalCase to camelCase"
  [s]
  (str (-> s first string/lower-case str) (subs s 1)))

(defn remove-proxy-suffix
  "remove 'Proxy' suffix"
  [s]
  (if (string/ends-with? s "Proxy")
    (subs s 0 (- (count s) 5))
    s))

(defn relate-one
  [name id]
  {:type name :id id})

;; if need better, see https://github.com/trhura/clojure-humanize
(defn pluralize
  [s]
  (str s "s"))

(defn relate
  [[k ids]]
  (let [name (class-name->str k)
        one? (= 1 (count ids))
        plur (if one? identity pluralize)]
    {(-> name plur keyword)
     {:data
      (if one?
        (relate-one name (first ids))
        (map #(relate-one name %) ids))
      }}))

(defn class-name->str
  [n]
  ;; (-> n :_meta :class name lower-first-char remove-proxy-suffix))
  (-> n name lower-first-char remove-proxy-suffix))

(defn extract-relationships-by-type
  [relationships]
  (->>
   relationships
   (map (fn [r] [(-> r :_meta :class) (:id r)]))
   merge-lists
   (map relate)
   (apply merge)
   ))

(defn node->json-api
  [n]
  {:type (-> n :_meta :class class-name->str)
   :id (-> n :_meta :id)
   :attributes (dissoc n :id :_meta)
   :relationships (extract-relationships-by-type (n/relationships n))
   })

(defn ->json-api
  [n included]
  {:data (node->json-api n) :included (map node->json-api included)})

