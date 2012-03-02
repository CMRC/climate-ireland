(ns clad.models.couch
  (:require [com.ashafa.clutch :as clutch])
  (:use [com.ashafa.clutch.view-server]))

(clutch/configure-view-server "icip" (view-server-exec-string))

(defn get-run-data [year months]
  (clutch/with-db "icip"
    (clutch/save-view "vals"
                      (clutch/view-server-fns
                       :clojure
                       {:by-ym
                        {:map (fn [doc] (when (:year doc)
                                          [[(:year doc)
                                            doc]]))}}))
    (map #(:year (:value %)) (clutch/get-view "vals" :by-ym {:key year}))))
