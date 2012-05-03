(ns clad.models.couch
  (:require [com.ashafa.clutch :as clutch])
  (:use [com.ashafa.clutch.view-server]))

(def db "climate")
;;(clutch/configure-view-server db (view-server-exec-string))

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
  (clutch/with-db db
    (map #(:value %) (clutch/get-view "vals" :by-ym {:key [(Integer/parseInt year) months]}))))

(defn get-county-data [county months model scenario variable]
  (clutch/with-db db
    (clutch/get-view "counties" :by-county {:key [county months model scenario variable]})))

(defn get-county-by-year [county year months model scenario variable]
  (clutch/with-db db
    (clutch/get-view "counties-year" :by-county-year {:key [county year months model scenario variable]})))

(defn get-models []
  (clutch/with-db db
    (clutch/get-view "models" :by-model)))

(defn data-by-county [county year months model scenario variable]
  (if-let [d (->> (get-county-by-year county year months model scenario variable)
                  first 
                  :value
                  :datum.value)] d -1))


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
  (map #(str (bycounty-memo % year months model scenario variable) ",") counties))

(def ensemble [["CGCM31" "A1B"] ["HadGEM" "RCP45"] ["HadGEM" "RCP85"]])
