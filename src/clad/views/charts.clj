(ns clad.views.charts
  (:use incanter.core, incanter.stats, incanter.charts,
	incanter.io clad.models.couch)
  (:require [clj-time.core :as time]
            [clj-time.coerce :as coerce])
  (:import (java.io ByteArrayOutputStream
                    ByteArrayInputStream)
           (java.lang Integer)))

(defn plot-models []
  (let [model (get-models)
        x (map #(coerce/to-long (time/date-time (Integer/parseInt (:key %)))) model)
        y (map #(:datum.value (:value %)) model)
        chart (doto (time-series-plot x y :y-label "%"))
        out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (save chart out-stream)
                    (ByteArrayInputStream. 
                     (.toByteArray out-stream)))]
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in-stream}))

