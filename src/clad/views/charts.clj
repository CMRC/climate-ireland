(ns clad.views.charts
  (:use incanter.core, incanter.stats, incanter.charts,
	incanter.io clad.models.couch)
  (:require [clj-time.core :as time]
            [clj-time.coerce :as coerce])
  (:import (java.io ByteArrayOutputStream
                    ByteArrayInputStream)
           (java.lang Integer)))


(defn plot-models [county months variable]
  (let [a1b (get-county-data county months "CGCM31" "A1B" variable)
        x (map #(coerce/to-long (time/date-time (:year (:value %)))) a1b)
	a1by (map #(- (:datum.value (:value %))
                      (ref-data county months "CGCM31" variable)) a1b)
	a2 (get-county-data county months "CGCM31" "A2" variable)
        a2y (map #(- (:datum.value (:value %))
                     (ref-data county months "CGCM31" variable)) a2)
        cc20 (get-county-data county months "CGCM31" "C20" variable)
        cc20y (map #(- (:datum.value (:value %))
                       (ref-data county months "CGCM31" variable)) cc20)		
        cc20x (map #(coerce/to-long (time/date-time (:year (:value %)))) cc20)
	ha45 (get-county-data county months "HadGEM" "RCP45" variable)
        ha45y (map #(- (:datum.value (:value %))
                       (ref-data county months "HadGEM" variable)) ha45)			
	hc20 (get-county-data county months "HadGEM" "C20" variable)
        hc20y (map #(- (:datum.value (:value %))
                       (ref-data county months "HadGEM" variable)) hc20)		
        ha85 (get-county-data county months "HadGEM" "RCP85" variable)
        ha85y (map #(- (:datum.value (:value %))
                      (ref-data county months "HadGEM" variable)) ha85)
        ica [2025 2055 2085]
        icax (map #(coerce/to-long (time/date-time %)) ica)
        icay (map #(data-by-county county (- % 5) months "ICARUS" "ICARUS" variable) ica)
        chart (doto (time-series-plot x a1by :y-label "ΔK" :x-label "" :series-label "CGCM3.1 A1B"
                                      :legend true :title (str county " " variable " " months))
                (add-lines x a2y :series-label "CGCM3.1 A2")                
                (add-lines cc20x cc20y :series-label "CGCM3.1 C20")                
		(add-lines cc20x hc20y :series-label "HadGEM C20")                
		(add-lines x ha45y :series-label "HadGEM RCP45")
                (add-lines x ha85y :series-label "HadGEM RCP85")
                (add-lines icax icay :series-label "ICARUS"))
        out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (save chart out-stream :width 400 :height 600)
                    (ByteArrayInputStream. 
                     (.toByteArray out-stream)))]
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in-stream}))

(defn plot-models-decadal [county months variable]
  (let [step 10
        base-range (range 1961 1981)
        projected-range (range 2021 2051)
        decadal (fn [run r]
                  (map
                   (fn [start]
                     (/ (reduce
                         (fn [acc yr]

                          (+ acc
                              (temp-diff-data county yr months (first run)
                                              (second run) variable)))
                         0 
                         (range start (+ start step)))
                        step)) r))
        a1b (decadal ["CGCM31" "A1B"] projected-range)
        x (map #(coerce/to-long (time/date-time (+ 5 %))) projected-range)
        refx (map #(coerce/to-long (time/date-time (+ 5 %))) base-range)
        ica [2025 2055 2085]
        icax (map #(coerce/to-long (time/date-time %)) ica)
        icay (map #(data-by-county county (- % 5) months "ICARUS" "ICARUS" variable) ica)   
        chart (doto (time-series-plot x a1b :y-label "ΔK"
                                      :x-label ""
                                      :series-label "CGCM3.1 A1B"
                                      :legend true
                                      :title (str county " " variable " " months))
                (add-lines x (decadal ["CGCM31" "A2"] projected-range)
                           :series-label "CGCM3.1 A2")
                (add-lines refx (decadal ["HadGEM" "C20"] base-range)
                           :series-label "HadGEM C20")
                (add-lines refx (decadal ["CGCM31" "C20"] base-range)
                           :series-label "CGCM31 C20")
                (add-lines x (decadal ["HadGEM" "RCP45"] projected-range)
                           :series-label "HadGEM RCP45")
                (add-lines x (decadal ["HadGEM" "RCP85"] projected-range)
                           :series-label "HadGEM RCP85")
                (add-lines icax icay :series-label "ICARUS"))
        out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (save chart out-stream :width 400 :height 600)
                    (ByteArrayInputStream. 
                     (.toByteArray out-stream)))]
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in-stream}))

(defn barchart [county year months variable]
  (let [y (cons (data-by-county county year months "ICARUS" "ICARUS" variable)
                (map #(- (data-by-county county year months (first %) (second %) variable)
                         (ref-data county months (first %) variable))
                     ensemble))
        x (cons "ICARUS" (map #(str (first %) " " (second %)) ensemble))
        chart (bar-chart x y :title (str county " " year " " months " " variable)
                         :x-label ""			     
                         :y-label "ΔK"
                         :legend true
                         :group-by x)
        out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (save chart out-stream :width 450 :height 600)
                    (ByteArrayInputStream. 
                     (.toByteArray out-stream)))]
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in-stream}))

