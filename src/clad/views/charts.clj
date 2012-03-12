(ns clad.views.charts
  (:use incanter.core, incanter.stats, incanter.charts,
	incanter.io clad.models.couch)
  (:require [clj-time.core :as time]
            [clj-time.coerce :as coerce])
  (:import (java.io ByteArrayOutputStream
                    ByteArrayInputStream)
           (java.lang Integer)))

(defn plot-models []
  (let [mam (get-county-data "Kilkenny" "mam")
        x (map #(coerce/to-long (time/date-time (Integer/parseInt (:year (:value %))))) mam)
        mamy (map #(:datum.value (:value %)) mam)
        jja (get-county-data "Kilkenny" "jja")
        jjay (map #(:datum.value (:value %)) jja)
        son (get-county-data "Kilkenny" "son")
        sony (map #(:datum.value (:value %)) son)
        djf (get-county-data "Kilkenny" "djf")
        djfy (map #(:datum.value (:value %)) djf)
        chart (doto (time-series-plot x mamy :y-label "%" :series-label "MAM" :legend true :title "Kilkenny")
                (add-lines x jjay :series-label "JJA")
                (add-lines x sony :series-label "SON")
                (add-lines x djfy :series-label "DJF"))
        out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (save chart out-stream)
                    (ByteArrayInputStream. 
                     (.toByteArray out-stream)))]
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in-stream}))

