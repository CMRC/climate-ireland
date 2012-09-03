(ns clad.models.couch
  (:require [com.ashafa.clutch :as clutch]
            [cemerick.friend [credentials :as creds]])
  (:use [com.ashafa.clutch.view-server]
        clojure.contrib.math))

;; (clutch/with-db db
;;   (clutch/save-view "users"
;;                     (clutch/view-server-fns
;;                      :clojure
;;                      {:users
;;                       {:map (fn [doc] (if (:username doc) [[(:username doc) doc]]))}})))

;;(clutch/with-db db
;;   (clutch/put-document {:username ""
;;                         :password (creds/hash-bcrypt "")
;;                         :roles #{::user}}))

(def db "climate")
;;(clutch/configure-view-server db (view-server-exec-string))

(defn get-users []
  (try
    (clutch/with-db db
      (reduce #(assoc %1 (:key %2) (:value %2)) {} (clutch/get-view "users" :users)))
    ;;for testing locally without the database we supply a default password
    (catch java.io.IOException e {"local" {:username "local"
                                                 :password (creds/hash-bcrypt "local")
                                                 :roles #{::user}}})))


(def provinces ["Leinster" "Munster" "Connaught" "Ulster"])


(defn save-views []
  (clutch/with-db db
    (clutch/save-view "vals"
                      (clutch/view-server-fns
                       :clojure
                       {:by-ym
                        {:map (fn [doc] [[[(:year doc) (:months doc)]
                                          doc]])}}))
    (clutch/save-view "counties"
                      (clutch/view-server-fns
                       :clojure
                       {:by-county
                        {:map (fn [doc] [[[(:region doc)
                                           (:months doc) (:model doc) 
                                           (:scenario doc) (:datum.variable doc)]
                                          doc]])}}))
    (clutch/save-view "counties-year"
                      (clutch/view-server-fns
                       :clojure
                       {:by-county-year
                        {:map (fn [doc] (if (= (class (:year doc)) java.lang.Integer)
                                          [[[(:region doc)
                                             (:year doc) (:months doc)
                                             (:model doc) (:scenario doc) (:datum.variable doc)]
                                            doc]]))}}))
    (clutch/save-view "models"
                      (clutch/view-server-fns
                       :clojure
                       {:by-model
                        {:map (fn [doc] [[(:year doc),doc]])}}))))
#_(save-views)

(defn get-run-data [year months]
  (try
    (clutch/with-db db
      (map #(:value %) (clutch/get-view "vals" :by-ym {:key [(Integer/parseInt year) months]})))
    (catch java.net.ConnectException e [{:region "Kilkenny"}])))

(defn get-county-data [county months model scenario variable]
  (try
    (clutch/with-db db
      (clutch/get-view "counties" :by-county {:key [county months model scenario variable]}))
    (catch java.net.ConnectException e [{:region "Kilkenny"}])))

(defn get-county-by-year [county year months model scenario variable]
  (try
    (clutch/with-db db
      (clutch/get-view "counties-year" :by-county-year {:key [county year months model scenario variable]})
      (catch java.net.ConnectException e [{:region county :year year :months months :model model
                                           :scenario scenario :variable variable :datum.value 1}]))))
(defn get-models []
  (try
    (clutch/with-db db
      (clutch/get-view "models" :by-model))
    (catch java.net.ConnectException e [{:region "Kilkenny"}])))

(defn data-by-county [county year months model scenario variable]
  (when-let [d (->> (get-county-by-year county year months model scenario variable)
                  first 
                  :value
                  :datum.value)] d))

(defn ref-data-slow [county months model variable]
  (try
    (clutch/with-db db
      (/ (reduce
          #(+ %1 (data-by-county county %2 months model "C20" variable))
          0
          (range 1961 1990))
         (- 1990 1961)))
    (catch java.net.ConnectException e [{:region "Kilkenny"}])))

(def ref-data (memoize ref-data-slow))

(def ensemble [["CGCM31" "A1B"]
               ["CGCM31" "A2"]
               ["HadGEM" "RCP45"]
               ["HadGEM" "RCP85"]
               #_["ICARUS" "ICARUS"]])

(defn ensemble-data [county year months variable]
  (/ (reduce #(+ %1 (data-by-county county year months (first %2) (second %2) variable))
             0
             ensemble) (count ensemble)))

(defn diff-data [county year months model scenario variable]
  (if (= model "ensemble")
    (let [ref (/ (reduce #(+ %1 (ref-data county months (first %2) variable)) 0 ensemble)
                 (count ensemble))
          comp (/ (reduce #(+ %1 (data-by-county county year months (first %2) (second %2) variable))
                          0 ensemble)
                  (count ensemble))
          res (->
               (- comp ref)
               (/ ref)
               (* 10000)
               round
               (/ 100)
               float)]
      res)
    (let [ref (ref-data county months model variable)
          comp (data-by-county county year months model scenario variable)
          res (->
               (- comp ref)
               (/ ref)
               (* 10000)
               round
               (/ 100)
               float)]
      res)))

(defn temp-diff-data [county year months model scenario variable]
  (if (= model "ensemble")
    (let [ref (/ (reduce #(+ %1 (ref-data county months (first %2) variable)) 0 ensemble)
                 (count ensemble))
          comp (/ (reduce #(+ %1 (data-by-county county year months (first %2) (second %2) variable))
                          0 ensemble)
                  (count ensemble))
          res (- comp ref)]
      res)
    (let [ref (ref-data county months model variable)
          comp (data-by-county county year months model scenario variable)
          res (- comp ref)]
      res)))

(defn decadal-min-temp [months model scenario variable regions diff-fn]
  (apply min
         (map (fn [decade]
                (apply min
                       (map (fn [region] (diff-fn region decade months model scenario variable))
                            regions)))
              [202130 203140 204150 205160])))

(defn decadal-max-temp [months model scenario variable regions diff-fn]
  (apply max
         (map (fn [decade]
                (apply max
                       (map (fn [region] (diff-fn region decade months model scenario variable))
                            regions)))
              [202130 203140 204150 205160])))

(def bycounty-memo (memoize data-by-county))

(def counties
       ["Carlow" "Cavan" "Clare" "Cork"
        "Donegal" "Dublin" "Galway" "Kerry"
        "Kildare" "Kilkenny" "Laois" "Leitrim"
        "Limerick" "Longford" "Louth" "Mayo"
        "Meath" "Monaghan" "North Tipperary"
        "Offaly" "Roscommon" "Sligo"
        "South Tipperary" "Waterford" "Westmeath"
        "Wexford" "Wicklow"])

(def counties-by-province
  {"Carlow" "Leinster"
   "Cavan" "Ulster"
   "Clare" "Connaught"
   "Cork" "Munster"
   "Donegal" "Ulster"
   "Dublin" "Leinster"
   "Galway" "Connaught"
   "Kerry" "Munster"
   "Kildare" "Leinster"
   "Kilkenny" "Leinster"
   "Laois" "Leinster"
   "Leitrim" "Connaught"
   "Limerick" "Munster"
   "Longford" "Leinster"
   "Louth" "Leinster"
   "Mayo" "Connaught"
   "Meath" "Leinster"
   "Monaghan" "Ulster"
   "North Tipperary" "Munster"
   "Offaly" "Leinster"
   "Roscommon" "Connaught"
   "Sligo" "Connaught"
   "South Tipperary" "Munster"
   "Waterford" "Munster"
   "Westmeath" "Leinster"
   "Wexford" "Leinster"
   "Wicklow" "Leinster"})

(defn all-counties [year months model scenario variable]
  (map #(str (data-by-county % year months model scenario variable) ",") counties))


(def temp-vars ["T_2M" "TMAX_2M" "TMIN_2M"])

(defn temp-var? [variable] (some #(= variable %) temp-vars))

(defn make-url [view req & {:keys [counties?] :or {counties? false}}]
  (str "/ci/"
       view "/"
       (apply str (interpose "/" (vals req)))
       (when counties? "/counties")))
  
