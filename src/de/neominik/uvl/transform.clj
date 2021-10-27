(ns de.neominik.uvl.transform
  (:require [clojure.string :as s]
            [de.neominik.uvl.ast :refer :all])
  (:import [de.neominik.uvl.ast
            UVLModel
            Import
            Feature
            Group
            Not
            And
            Or
            Impl
            Equiv]))

(def ^:dynamic *features*)
(def ^:dynamic *constraints*)
(def ^:dynamic *own-constraints*)
(def ^:dynamic *nsp*)

(defn xf-ref [& ids] ;TODO resolve references
  (s/join "." ids))

(defn xf-featuremodel [& kvs]
  (let [{:keys [Ns imports features constraints] :or {imports [] features [] constraints []}} (into {} kvs)
        ns (or Ns (some-> (first features) .getName) "")]
    (doto (UVLModel.)
      (.setNamespace ns)
      (.setImports (into-array Import imports))
      (.setRootFeatures (into-array Feature features))
      (.setConstraints (into-array Object constraints)))))

(defn xf-import
  ([namespace] (Import. namespace namespace))
  ([namespace [_ alias]] (Import. namespace alias)))
(defn xf-imports
  ([& imports] [:imports imports])
  ([] [:imports []]))

(defn xf-feature [name & kvs]
  (let [{:keys [attributes groups] :or {attributes {} groups []}} (into {} kvs)
        qf-name (str *nsp* name)
        feature (Feature. qf-name attributes (into-array Group groups))]
    (swap! *features* assoc qf-name feature)
    feature))
(defn xf-features
  ([& features] [:features features])
  ([] [:features []]))

(defn xf-attribute
  ([[_ k] v] [k v])
  ([[_ k]] [k true]))
(defn xf-attributes [& keyvals]
  [:attributes (into {} keyvals)])

(defn- to-number [n]
  (if (= n "*") -1 (clojure.edn/read-string n)))
(defn xf-cardinality
  ([upper] (repeat 2 (to-number upper)))
  ([lower upper] (map to-number [lower upper])))
(defn xf-group [type & children]
  (if (string? type)
    (doto (Group.) (.setType type) (.setChildren (into-array Feature children)))
    (let [[lower upper] type]
      (doto (Group.) (.setType "cardinality") (.setLower lower) (.setUpper upper) (.setChildren (into-array Feature children))))))
(defn xf-groups [& groups]
  [:groups groups])

(defn xf-constraints
  ([& constraints]
   (swap! *own-constraints* concat constraints)
   (swap! *constraints* concat constraints)
   [:constraints constraints])
  ([] [:constraints []]))
(defn xf-reference [s] (str *nsp* s))
(defn xf-not [x] (Not. x))
(defn xf-and [x y] (And. x y))
(defn xf-or [x y] (Or. x y))
(defn xf-impl [x y] (Impl. x y))
(defn xf-equiv [x y] (Equiv. x y))

(def transform-map {:FeatureModel xf-featuremodel
                    :REF xf-ref
                    :REF-Restrictive xf-ref
                    :Imports xf-imports
                    :Import xf-import

                    :Features xf-features
                    :FeatureSpec xf-feature
                    :Attributes xf-attributes
                    :Attribute xf-attribute
                    :Value identity
                    :Boolean clojure.edn/read-string
                    :Number clojure.edn/read-string
                    :String clojure.edn/read-string
                    :Vector vector
                    :Cardinality xf-cardinality
                    :Group xf-group
                    :Groups xf-groups

                    :Constraints xf-constraints
                    :Reference xf-reference
                    :Not xf-not
                    :And xf-and
                    :Or xf-or
                    :Impl xf-impl
                    :Equiv xf-equiv})
