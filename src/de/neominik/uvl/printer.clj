(ns de.neominik.uvl.printer
  (:require [clojure.string :as s]
            [instaparse.failure :as f]))

(defn- indent [text]
  (str "\t" (s/replace text #"\n(?=.)" "\n\t")))
(defn- prn-section [n xs]
  (when-let [ys (seq xs)] (format "\n%s\n%s" n (indent (s/join "\n" ys)))))


(defn- format-feature-name [name]
  "If the feature name contains an illegal name (cf. the following regex), parantheses are wrapped around the feature name."
  (if (re-matches #"(?!alternative|or|features|constraints|true|false|as|refer|_INDENT_|_DEDENT_)[^\s\"\{\}\[\]\(\)\=\>\<\|\&\!]+" name)
    name
    (let [ind (clojure.string/last-index-of name ".")]
      (if (nil? ind)
        (str "\"" name "\"")
        (str (subs name 0 (inc ind)) "\"" (subs name (inc ind)) "\"")))))

(defn UVLModel-prn [m]
  (str
   (format "namespace %s" (.getNamespace m))
   (prn-section "\nimports" (.getImports m))
   (prn-section "\nfeatures" (.getRootFeatures  m))
   (prn-section "\nconstraints" (concat
     (for [i (seq (.getOwnConstraints m))] (if (instance? String i) (format-feature-name i) (str i)))
     (for [i (seq (.getConstraints m))] (if (instance? String i) (format-feature-name i) (str i)))))))

(defn Import-prn [i]
  (str (.getNamespace i) (when-not (= (.getNamespace i) (.getAlias i)) (format " as %s" (.getAlias i)))))

(defn- prn-cardinality [lower upper]
  (let [l (if (= lower upper) "" (str lower ".."))
        u (if (= upper -1) "*" (str upper))]
    (format "[%s%s]" l u)))
(defn Group-prn [g]
  (let [type (if (= (.getType g) "cardinality") (prn-cardinality (.getLower g) (.getUpper g)) (.getType g))]
    (prn-section type (.getChildren g))))

(defn- prn-item [[k v]]
  (str k (when-not (= true v) (str " " (pr-str v)))))
(defn- prn-attrs [at-map]
  (when-let [as (seq at-map)]
    (if (> (count as) 2)
      (format " {\n%s\n}" (indent (s/join ",\n" (map prn-item as))))
      (format " {%s}" (s/join ", " (map prn-item as))))))
(defn Feature-prn [f]
  (str (format-feature-name(.getName f)) (prn-attrs (.getAttributes f)) (when-let [gs (seq (.getGroups f))] (indent (s/join "\n" (map Group-prn gs))))))

(def ^:private priority (zipmap ["String" "Not" "And" "Or" "Impl" "Equiv"] (range)))
(defn- needs-parens [parent child]
  (let [pc (.getSimpleName (class parent))
        cc (.getSimpleName (class child))]
    (or (< (priority pc) (priority cc)) (= "Not" pc cc))))
(defn- prn-constraint [parent child]
  (if (instance? String child)
    (format-feature-name child)
    (if (needs-parens parent child)
      (format "(%s)" (str child))
      (str child))))
(defn Not-prn [c] (str \! (prn-constraint c (.getChild c))))
(defn And-prn [c] (format "%s & %s" (prn-constraint c (.getLeft c)) (prn-constraint c (.getRight c))))
(defn Or-prn [c] (format "%s | %s" (prn-constraint c (.getLeft c)) (prn-constraint c (.getRight c))))
(defn Impl-prn [c] (format "%s => %s" (prn-constraint c (.getLeft c)) (prn-constraint c (.getRight c))))
(defn Equiv-prn [c] (format "%s <=> %s" (prn-constraint c (.getLeft c)) (prn-constraint c (.getRight c))))

(defn ParseError-prn [e]
  (with-out-str (f/pprint-failure
                 {:line (.getLine e)
                  :column (.getColumn e)
                  :text (s/replace (.getText e) "\t" " ")
                  :reason (mapv (fn [s] {:expecting s}) (.getExpected e))})))
