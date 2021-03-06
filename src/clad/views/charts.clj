(ns clad.views.charts
  (:use incanter.core, incanter.stats, incanter.charts,
	incanter.io clad.models.couch)
  (:require [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clojure.tools.logging :as log])
  (:import (java.io ByteArrayOutputStream
                    ByteArrayInputStream)
           (java.lang Integer)
           (org.jfree.chart StandardChartTheme)
           (org.jfree.chart.annotations CategoryTextAnnotation)
           (org.jfree.chart.axis CategoryAxis)
           (org.jfree.chart.block BlockBorder)))

(def variables {"T_2M" "Temperature"
                "TMIN_2M" "Min Temp"
                "TMAX_2M" "Max Temp"
                "TOT_PREC" "Precipitation"})

(defn decadal-box [county months variable abs model ensemble]
  (let [models (if (= model "ensemble")
                 (get ensembles ensemble)
                 (vector (vector model ensemble)))
        delta (= abs "Change")
        diff-fn (if delta diff-by-county abs-data)
        log (log/info county months variable abs model ensemble)
        log1 (log/info models)
        vals-fn (fn [decade]
                  (map #(vector (first %) (second %))
                       (filter #(not (nil? (first %)))
                               (map vector (diff-fn
                                            county decade months
                                            models variable)
                                    models))))
        add-decade (fn [[cat decade] chart]
                     (when-let [vals (vals-fn decade)]
                       #_(doseq [[val sim] vals]
                         (log/info decade " ... " val sim)
                         (when val (.addAnnotation (.getPlot chart)
                                                   (CategoryTextAnnotation. (second sim) (str cat) val))))
                       (add-box-plot chart (map first vals)
                                     :legend true
                                     :series-label (str (* 10 (inc cat)) "s"))))
        decades ["2011-2040"
                 "2021-2050"
                 "2031-2060"
                 "2041-2070"
                 "2051-2080"
                 "2061-2090"
                 "2071-2100"]
        light-gray (java.awt.Color. 0xf2 0xf2 0xf2)
        chart (doto (box-plot (vals-fn "1961-1990")
                              :legend true
                              :title (str county " " (variables variable))
                              :y-label (if (temp-var? variable)
                                         (if delta "Difference in °C from baseline" "°C")
                                         (if delta "% difference from baseline" "mm/hr"))
                              :series-label "decade:"
                              :x-label "")
                (set-y-range (get-in mins [abs variable])
                             (get-in maxs [abs variable]))
                (set-theme (StandardChartTheme/createLegacyTheme))
                (.setBackgroundPaint light-gray)
                (->
                 .getPlot
                 (.setBackgroundPaint light-gray))
                (->
                 .getPlot
                 .getDomainAxis
                 (.setVisible false))
                (->
                 .getLegend
                 (.setBackgroundPaint light-gray))
                (->
                 .getLegend
                 (.setFrame (BlockBorder. 0 0 0 0))))
        labeled (reduce #(add-decade %2 %1) chart (map vector (range 1 7) decades))
        out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (save labeled out-stream :width 500 :height 400)
                    (ByteArrayInputStream. 
                     (.toByteArray out-stream)))]
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in-stream}))
