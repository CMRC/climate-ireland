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
	a1by (map #(- (:datum.value (:value %)) 273.15) a1b)
	a2 (get-county-data county months "CGCM31" "A2" variable)
        a2y (map #(- (:datum.value (:value %)) 273.15) a2)		
	cc20 (get-county-data county months "CGCM31" "C20" variable)
        cc20y (map #(:datum.value (:value %)) cc20)		
        cc20x (map #(coerce/to-long (time/date-time (:year (:value %)))) cc20)
	ha45 (get-county-data county months "HadGEM" "RCP45" variable)
        ha45y (map #(- (:datum.value (:value %)) 273.15) ha45)			
	hc20 (get-county-data county months "HadGEM" "C20" variable)
        hc20y (map #(- (:datum.value (:value %)) 273.15) hc20)		
        ha85 (get-county-data county months "HadGEM" "RCP85" variable)
        ha85y (map #(- (:datum.value (:value %)) 273.15) ha85)
        chart (doto (time-series-plot x a1by :y-label "°C" :x-label "" :series-label "CGCM3.1 A1B" :legend true :title (str county " " variable " " months))
                (add-lines x a2y :series-label "CGCM3.1 A2")                
                #_(add-lines cc20x cc20y :series-label "CGCM3.1 C20")                
		#_(add-lines cc20x hc20y :series-label "HadGEM C20")                
		(add-lines x ha45y :series-label "HadGEM RCP45")
                (add-lines x ha85y :series-label "HadGEM RCP85"))
        out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (save chart out-stream)
                    (ByteArrayInputStream. 
                     (.toByteArray out-stream)))]
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in-stream}))

(defn plot-models-decadal [county months variable]
  (let [r (range 2026 2065 10)
       	decadal (fn [run] (map (fn [mid] (/ (reduce (fn [acc yr] (+ acc (- (:datum.value (:value (first (get-county-by-year county yr months (first run) (second run) variable)))) 273.15)))   
	    	     	      	      0 
			      	      (range (- mid 5) (+ mid 5))) 
			    10)) r))
        a1b (decadal ["CGCM31" "A1B"])
        x (map #(coerce/to-long (time/date-time %)) r)
        chart (doto (time-series-plot x a1b :y-label "°C" :x-label "" :series-label "CGCM3.1 A1B" :legend true :title (str county " " variable " " months))
	      	    (add-lines x (decadal ["HadGEM" "RCP45"]) :series-label "HadGEM RCP45")	      	       	      	     (add-lines x (decadal ["HadGEM" "RCP85"]) :series-label "HadGEM RCP85"))
        out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (save chart out-stream)
                    (ByteArrayInputStream. 
                     (.toByteArray out-stream)))]
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in-stream}))

(defn barchart [county year months variable]
  (let [y (map #(- (:datum.value (:value (first (get-county-by-year county year months (first %) (second %) variable)))) 273.15) ensemble)
        x (map #(str (first %) " " (second %)) ensemble)
        chart (bar-chart x y :title "Model runs"
	      		     :x-label ""			     
			     :y-label "°C"
			     :group-by x)
        out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (save chart out-stream)
                    (ByteArrayInputStream. 
                     (.toByteArray out-stream)))]
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in-stream}))

