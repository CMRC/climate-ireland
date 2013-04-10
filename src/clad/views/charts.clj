(ns clad.views.charts
  (:use incanter.core, incanter.stats, incanter.charts,
	incanter.io clad.models.couch)
  (:require [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clojure.tools.logging :as log])
  (:import (java.io ByteArrayOutputStream
                    ByteArrayInputStream)
           (java.lang Integer)))

(def variables {"T_2M" "Temperature", "TOT_PREC" "Precipitation"})

(defn plot-models [county months variable]
  (let [ylab (if (temp-var? variable) "°C" "%")
        diff-fn (if (some #(= variable %) temp-vars) - #(* (/ (- %1 %2) %2) 100))
        a1b (get-county-data county months "CGCM31" "A1B" variable)
        x (map #(coerce/to-long (time/date-time (:year (:value %)))) a1b)
	a1by (map #(diff-fn (:datum.value (:value %))
                      (ref-data county months "CGCM31" variable)) a1b)
	a2 (get-county-data county months "CGCM31" "A2" variable)
        a2y (map #(diff-fn (:datum.value (:value %))
                     (ref-data county months "CGCM31" variable)) a2)
        cc20 (get-county-data county months "CGCM31" "C20" variable)
        cc20y (map #(diff-fn (:datum.value (:value %))
                       (ref-data county months "CGCM31" variable)) cc20)		
        cc20x (map #(coerce/to-long (time/date-time (:year (:value %)))) cc20)
	ha45 (get-county-data county months "HadGEM" "RCP45" variable)
        ha45y (map #(diff-fn (:datum.value (:value %))
                       (ref-data county months "HadGEM" variable)) ha45)			
	hc20 (get-county-data county months "HadGEM" "C20" variable)
        hc20y (map #(diff-fn (:datum.value (:value %))
                       (ref-data county months "HadGEM" variable)) hc20)		
        ha85 (get-county-data county months "HadGEM" "RCP85" variable)
        ha85y (map #(diff-fn (:datum.value (:value %))
                      (ref-data county months "HadGEM" variable)) ha85)
        ica [2025 2055 2085]
        icax (map #(coerce/to-long (time/date-time %)) ica)
        icay (map #(data-by-county county (- % 5) months "ICARUS" "ICARUS" variable) ica)
        chart (doto (time-series-plot x a1by :y-label ylab :x-label "" :series-label "CGCM3.1 A1B"
                                      :legend true :title (str county " " variable " " months))
                (add-lines x a2y :series-label "CGCM3.1 A2")                
                (add-lines cc20x cc20y :series-label "CGCM3.1 C20")                
		(add-lines cc20x hc20y :series-label "HadGEM C20")                
		(add-lines x ha45y :series-label "HadGEM RCP45")
                (add-lines x ha85y :series-label "HadGEM RCP85")
                (add-lines icax icay :series-label "ICARUS"))
        out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (save chart out-stream :width 397 :height 600)
                    (ByteArrayInputStream. 
                     (.toByteArray out-stream)))]
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in-stream}))

(defn decadal [run r step diff-fn county months variable]
  (map
   (fn [start]
     (/ (reduce
         (fn [acc yr]
           
           (+ acc
              (diff-fn county yr months (first run)
                       (second run) variable)))
         0 
         (range start (+ start step)))
        step)) r))
  
(defn plot-models-decadal [county months variable]
  (let [ylab (if (temp-var? variable) "Δ°C" "%")
        step 10
        base-range (range 1961 1991 10)
        projected-range (range 2021 2031 10)
        a1b (decadal ["CGCM31" "A1B"] projected-range step diff-data county months variable)
        x (map #(coerce/to-long (time/date-time (+ 5 %))) projected-range)
        refx (map #(coerce/to-long (time/date-time (+ 5 %))) base-range)
        ica [2025 2055 2085]
        icax (map #(coerce/to-long (time/date-time %)) ica)
        icay (map #(data-by-county county (- % 5) months "ICARUS" "ICARUS" variable) ica)   
        chart (doto (time-series-plot x a1b :y-label ylab
                                      :x-label ""
                                      :series-label "CGCM3.1 A1B"
                                      :legend true
                                      :title (str county " " variable " " months))
                (add-lines x (decadal ["CGCM31" "A2"] projected-range step diff-data county months variable)
                           :series-label "CGCM3.1 A2")
                (add-lines refx (decadal ["HadGEM" "C20"] base-range step diff-data county months variable)
                           :series-label "HadGEM C20")
                (add-lines refx (decadal ["CGCM31" "C20"] base-range step diff-data county months variable)
                           :series-label "CGCM31 C20")
                (add-lines x (decadal ["HadGEM" "RCP45"] projected-range step diff-data county months variable)
                           :series-label "HadGEM RCP45")
                (add-lines x (decadal ["HadGEM" "RCP85"] projected-range step diff-data county months variable)
                           :series-label "HadGEM RCP85")
                (add-lines icax icay :series-label "ICARUS"))
        out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (save chart out-stream :width 397 :height 600)
                    (ByteArrayInputStream. 
                     (.toByteArray out-stream)))]
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in-stream}))

(defn decadal-box [county months variable]
  (let [vals-fn (fn [decade]
                  (map double
                       (filter #(not (nil? %))
                               (map #(diff-data
                                      county decade months
                                      (first %) (second %) variable)
                                    (get ensemble "ensemble")))))
        l (log/info "Decadal values for " county " " months " " variable ": "
                    (vals-fn "2051-60"))
        chart (doto (box-plot (vals-fn "2021-30")
                              :legend true
                              :title (str county " " (variables variable))
                              :y-label (if (temp-var? variable) "Difference in °C from baseline"
                                           "% difference from baseline")
                              :series-label "2021-30")
                (add-box-plot (vals-fn "2031-40")
                              :legend true
                              :series-label "2031-40")
                (add-box-plot (vals-fn "2041-50")
                              :legend true
                              :series-label "2041-50")
                (add-box-plot (vals-fn "2051-60")
                              :legend true
                              :series-label "2051-60"))
        out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (save chart out-stream :width 450 :height 400)
                    (ByteArrayInputStream. 
                     (.toByteArray out-stream)))]
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in-stream}))

(defn barchart [county year months variable]
  (let [y (cons (data-by-county county year months "ICARUS" "ICARUS" variable)
                (map #(diff-data county year months (first %) (second %) variable)
                     ensemble))
        runs (cons "ICARUS" (map #(str (first %) " " (second %)) ensemble))
        x (repeat (count runs) "")
        chart (bar-chart x y :title (str county " " year " " months " " variable)
                         :x-label "Model runs"			     
                         :y-label (if (temp-var? variable) "Difference in °C from baseline"
                                      "% difference from baseline")
                         :legend true
                         :group-by runs)
        out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (save chart out-stream :width 397 :height 580)
                    (ByteArrayInputStream. 
                     (.toByteArray out-stream)))]
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in-stream}))

