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
<<<<<<< HEAD
    (map #(:value %) (clutch/get-view "vals" :by-ym {:key [(Integer/parseInt year) months]}))))
=======
    (map #(:value %) (clutch/get-view "vals" :by-ym {:key [year months]}))))

(defn get-models []
  (clutch/with-db "icip"
    (clutch/save-view "models"
                      (clutch/view-server-fns
                       :clojure
                       {:by-model
                        {:map (fn [doc] [[(:year doc),doc]])}}))
    (clutch/get-view "models" :by-model)))
>>>>>>> 0b7e4ed39321700adf4fbf40c9bd6b80723b765b
