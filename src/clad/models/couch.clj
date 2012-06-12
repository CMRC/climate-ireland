(ns clad.models.couch
  (:require [com.ashafa.clutch :as clutch])
  (:use [com.ashafa.clutch.view-server]
        clojure.contrib.math))

(def db "climate")
(clutch/configure-view-server db (view-server-exec-string))

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
                        {:map (fn [doc] [[[(:region doc)
                                           (:year doc) (:months doc)
                                           (:model doc) (:scenario doc) (:datum.variable doc)]
                                          doc]])}}))
    (clutch/save-view "models"
                      (clutch/view-server-fns
                       :clojure
                       {:by-model
                        {:map (fn [doc] [[(:year doc),doc]])}}))))

(defn get-run-data [year months]
  (try
    (clutch/with-db db
      (map #(:value %) (clutch/get-view "vals" :by-ym {:key [(Integer/parseInt year) months]})))
    (catch java.net.ConnectException e [{:region "Kilkenny"}])))

(defn get-county-data [county months model scenario variable]
  (try
    (clutch/with-db db
      (clutch/get-view "counties" :by-county {:key [county months model scenario variable]})))
  (catch java.net.ConnectException e [{:region "Kilkenny"}]))

(defn get-county-by-year [county year months model scenario variable]
  (try
    (clutch/with-db db
      (clutch/get-view "counties-year" :by-county-year {:key [county year months model scenario variable]})
      (catch java.net.ConnectException e [{:region county :year year :months months :model model
                                           :scenario scenario :varibale variable :datum.value 1}]))))
  
(defn get-models []
  (try
    (clutch/with-db db
      (clutch/get-view "models" :by-model))
    (catch java.net.ConnectException e [{:region "Kilkenny"}])))

(defn data-by-county [county year months model scenario variable]
  (if-let [d (->> (get-county-by-year county year months model scenario variable)
                  first 
                  :value
                  :datum.value)] d -1))

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
    (- (ensemble-data county year months variable) 273.15)
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

