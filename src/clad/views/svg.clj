(ns clad.views.svg
  (:use [analemma.xml :only [emit add-content add-attrs
			     parse-xml transform-xml filter-xml]]
	analemma.svg
	analemma.xml
        clad.models.couch
        incanter.stats
        clojure.contrib.math
	[clojure.java.io :only [file]])
  (:require [c2.scale :as scale]
            [vomnibus.color-brewer :as color-brewer])
  (:import (org.apache.batik.transcoder.image PNGTranscoder)
           (org.apache.batik.transcoder TranscoderInput
                                        TranscoderOutput)
           (java.io ByteArrayOutputStream
                    ByteArrayInputStream)))

(def counties-svg (parse-xml (slurp "src/clad/views/counties.svg")))
(def provinces-svg (parse-xml (slurp "src/clad/views/provinces.svg")))

(defn counties-data [year months model scenario variable]
  (map #(data-by-county % year months model scenario variable) counties))

(defn quartiles-slow [year months model scenario variable cp]
  (map #(/ (round (* % 100)) 100)
       (quantile (map (fn [county] (temp-diff-data county year months model scenario variable))
                      (case cp :county counties :province provinces)))))

(def quartiles (memoize quartiles-slow))
  
(defn colour-on-quartiles [elem county year months model scenario variable]
  (let [val (temp-diff-data county year months model scenario variable)]
    (add-style elem :fill (cond (< val (nth (quartiles year months model scenario variable) 1))
                                "#56b"
                                (< val (nth (quartiles year months model scenario variable) 2))
                                "#769"
                                (< val (nth (quartiles year months model scenario variable) 3))
                                "#967"
                                :else "#b65"))))

(defn linear-rgb [val min max]
  (let [colour-scheme color-brewer/RdYlBu-11
        colour-scale (let [s (scale/linear :domain [max min]
                                           :range [0 (dec (count colour-scheme))])]
                       ;;todo: build interpolators so scales handle non-numeric ranges
                       (fn [d] (nth colour-scheme (floor (s d)))))]
    (colour-scale val)))
  
(defn colour-on-linear [elem county year months model scenario variable region]
  (let [val (temp-diff-data county year months model scenario variable)
        min 1.0 #_(nth (quartiles year months model scenario variable region) 0)
        max 2.5 #_(nth (quartiles year months model scenario variable region) 4)
        colour-scheme color-brewer/RdYlBu-11
        colour-scale (let [s (scale/linear :domain [max min]
                                           :range [0 (dec (count colour-scheme))])]
                      ;;todo: build interpolators so scales handle non-numeric ranges
                      (fn [d] (nth colour-scheme (floor (s d)))))]
    (add-style elem :fill (colour-scale val))))

(defn regions-map 
  [cp req]
  (let [{:keys [year months model scenario variable fill region]
         :or {model "ensemble" scenario "ensemble"}} req
        regions-svg (case cp
                      :county counties-svg
                      :province provinces-svg)
        regions (case cp
                  :county counties
                  :province provinces)
        fill-fns {"linear" colour-on-linear,
                  "quartiles" colour-on-quartiles}
        min 1.0
        max 2.5
        intyear (Integer/parseInt year)
        local-min (nth (quartiles intyear months model scenario variable cp) 0)]
    {:status 200
     :headers {"Content-Type" "image/svg+xml"}
     :body
     (emit (->
            (reduce #(transform-xml
                      %1
                      [{:id %2}]
                      (fn [elem]
                        (let [link (make-url "welcome/svgbar"
                                             (assoc-in req [:region] %2)
                                             :counties? (= cp :county))]
                          [:a {:xlink:href link :target "_top"}
                           (-> (add-attrs elem :onmouseover
                                          (str "value(evt,'"
                                               (->
                                                (temp-diff-data %2 intyear months model scenario variable)
                                                (* 100)
                                                round
                                                (/ 100)
                                                float)
                                               (if (temp-var? variable) "°C " "% ")
                                               %2
                                               "')"))
                               ((fill-fns fill) %2 intyear months model scenario variable region))])))
                    regions-svg			
                    regions)
            (transform-xml
             [{:id "min-text"}]
             #(set-content % (str (float local-min))))
            (transform-xml
             [{:id "max-text"}]
             #(set-content % (str (float (+ local-min 0.3)))))
            (transform-xml
             [{:id "min"}]
             #(add-style % :fill (linear-rgb local-min min max)))
            (transform-xml
             [{:id "min-6"}]
             #(add-style % :fill (linear-rgb (+ local-min 0.15) min max)))
            (transform-xml
             [{:id "max"}]
             #(add-style % :fill (linear-rgb (+ local-min 0.3) min max)))))}))

  
(defn counties-map 
  [req]
  (regions-map :county req))

(defn provinces-map
  [req]
  (regions-map :province req))


(defn counties-map-png 
  ([year months model scenario variable fill]
    (let [input (TranscoderInput. (str "http://www.climateireland.ie:8888/svg/" year "/" months "/" model
                                       "/" scenario "/" variable "/" fill))
          ostream (ByteArrayOutputStream.)
	  output (TranscoderOutput. ostream)
          t (PNGTranscoder.)
          n (. t transcode input output)
          istream (ByteArrayInputStream. 
                   (.toByteArray ostream))]
      {:status 200
       :headers {"Content-Type" "image/png"}
        :body
       istream})))

