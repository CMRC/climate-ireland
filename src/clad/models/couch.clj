(ns clad.models.couch
  (:require [com.ashafa.clutch :as clutch])
  (:use [com.ashafa.clutch.view-server]))

(clutch/configure-view-server "icip" (view-server-exec-string))

(defn get-run-data [year months]
  (clutch/with-db "icip"
    #_(clutch/save-view "vals"
                        (clutch/view-server-fns
                         :clojure
                         {:by-ym
                          {:map (fn [doc] [[[(:year doc) (:months doc)]
                                            doc]])}}))
    (map #(:value %) (clutch/get-view "vals" :by-ym {:key [(Integer/parseInt year) months]}))))

(defn get-county-data [county months]
  (clutch/with-db "icip"
    (clutch/save-view "vals"
                      (clutch/view-server-fns
                       :clojure
                       {:by-county
                        {:map (fn [doc] [[[(:county doc) (:months doc)]
                                          doc]])}}))
    (clutch/get-view "vals" :by-county {:key [county months]})))

(defn get-models []
  (clutch/with-db "icip"
    (clutch/save-view "models"
                      (clutch/view-server-fns
                       :clojure
                       {:by-model
                        {:map (fn [doc] [[(:year doc),doc]])}}))
    (clutch/get-view "models" :by-model)))


(defn data-by-county [county run]
  10.0)

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

(defn all-counties [run]
  (map #(str (bycounty-memo % run) ",") counties))
