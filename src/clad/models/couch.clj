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

(defn get-county-data [county months model scenario variable]
  (clutch/with-db "icip"
    (clutch/save-view "counties"
                      (clutch/view-server-fns
                       :clojure
                       {:by-county
                        {:map (fn [doc] [[[(:county doc) (:months doc) (:model doc) 
			(:scenario doc) (:datum.variable doc)]
                                          doc]])}}))
    (clutch/get-view "counties" :by-county {:key [county months model scenario variable]})))

(defn get-models []
  (clutch/with-db "icip"
    #_(clutch/save-view "models"
                      (clutch/view-server-fns
                       :clojure
                       {:by-model
                        {:map (fn [doc] [[(:year doc),doc]])}}))
    (clutch/get-view "models" :by-model)))
