(ns clad.views.charts
  (:use incanter.core, incanter.stats, incanter.charts,
	incanter.io clad.models.couch)
  (:require [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clojure.tools.logging :as log])
  (:import (java.io ByteArrayOutputStream
                    ByteArrayInputStream)
           (java.lang Integer)
           (org.jfree.chart.annotations CategoryTextAnnotation)))

(def variables {"T_2M" "Temperature", "TOT_PREC" "Precipitation"})

(defn decadal-box [county months variable abs]
  (let [delta (= abs "Delta")
        diff-fn (if delta diff-data abs-data)
        vals-fn (fn [decade]
                  (map double
                       (filter #(not (nil? %))
                               (map #(diff-fn
                                      county decade months
                                      (first %) (second %) variable)
                                    (get ensemble "ensemble")))))
        decades ["2021-2050"
                 "2031-2060"
                 "2041-2070"
                 "2051-2080"
                 "2061-2090"
                 "2071-2100"]
        chart (doto (box-plot (vals-fn "1961-1990")
                              :legend true
                              :title (str county " " (variables variable))
                              :y-label (if (temp-var? variable)
                                         (if delta "Difference in °C from baseline" "°C")
                                         (if delta "% difference from baseline" "mm???"))
                              :series-label "decade:"
                              :x-label "")
                (add-box-plot (vals-fn "2021-2050")
                              :legend true
                              :series-label "2030s")
                (add-box-plot (vals-fn "2031-2060")
                              :legend true
                              :series-label "2040s")
                (add-box-plot (vals-fn "2041-2070")
                              :legend true
                              :series-label "2050s")
                (add-box-plot (vals-fn "2051-2080")
                              :legend true
                              :series-label "2060s")
                (add-box-plot (vals-fn "2061-2090")
                              :legend true
                              :series-label "2070s")
                (add-box-plot (vals-fn "2071-2100")
                              :legend true
                              :series-label "2080s"))
        chart2 (doseq [plot (zipmap ["1" "2" "3" "4" "5" "6"] decades)]
                 (doseq [[sim val] (zipmap (get ensemble "ensemble") (vals-fn (second plot)))]
                   (.addAnnotation (.getPlot chart) (CategoryTextAnnotation. (second sim) (first plot) val))))
        out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (save chart out-stream :width 450 :height 400)
                    (ByteArrayInputStream. 
                     (.toByteArray out-stream)))]
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in-stream}))
