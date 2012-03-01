(ns clad.models.couch
  (:require [com.ashafa.clutch :as clutch])
  (:use [com.ashafa.clutch.view-server]))

(clutch/configure-view-server "icip" (view-server-exec-string))

(defn get-run-data [year months]
  (clutch/save-view "icip" "test2"
   (clutch/view-server-fns
    :clojure
    {:test2
     {:map "(fn [doc] (when (= year (:year doc)) [[nil,(:year doc)]]))"}}))
  (clutch/get-view "icip" "test2" :test2))
