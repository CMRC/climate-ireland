(ns clad.views.svg
  (:use [analemma.xml :only [emit add-content add-attrs
			     parse-xml transform-xml filter-xml]]
	analemma.svg
	analemma.xml
        clad.models.couch
        incanter.stats
        clojure.math.numeric-tower
	[clojure.java.io :only [file]])
  (:require [c2.scale :as scale]
            [vomnibus.color-brewer :as color-brewer]
            [clojure.tools.logging :as log]
            [clojure.stacktrace :as trace])
  (:import (org.apache.batik.transcoder.image PNGTranscoder)
           (org.apache.batik.transcoder TranscoderInput
                                        TranscoderOutput)
           (java.io ByteArrayOutputStream
                    ByteArrayInputStream)))

(def counties-svg (parse-xml (slurp (clojure.java.io/resource "clad/views/counties.svg"))))
(def provinces-svg (parse-xml (slurp (clojure.java.io/resource "clad/views/provinces.svg"))))

(defn quartiles-slow [year months model scenario variable cp diff-fn]
  (map #(/ (round (* % 100)) 100)
       (quantile (map (fn [county] (diff-fn county year months model scenario variable))
                      (case cp :county counties :province provinces)))))

(def quartiles (memoize quartiles-slow))
  
(defn colour-on-quartiles [elem county year months model scenario variable])

(defn linear-rgb [val domain colour-scheme]
  "Returns a colour string based on the value"
  (if (nil? val)
    "grey"
    (let [colour-scale (let [s (scale/linear :domain domain
                                             :range [0 (count colour-scheme)])
                             idx (fn [d] (floor (s d)))
                             checked-idx (fn [d] (if (< (idx d) 0) 0
                                                     (if (> (idx d) (dec (count colour-scheme)))
                                                       (dec (count colour-scheme))
                                                       (idx d))))]
                         (fn [d] (nth colour-scheme (checked-idx d))))]
      (colour-scale val))))

(defn colour-on-linear
  "Calls CouchDB and returns a colour string based on the query parameters"
  [elem county year months models variable region lmin lmax diff-fn colour-scheme]
  (let [domain [lmax lmin]
        vals (filter (fn [x] (not (nil? x))) (diff-fn county year months models variable))
        val (/ (reduce + 0 vals) (count vals))]
    (add-style elem :fill (linear-rgb val domain colour-scheme))))

(defn regions-map-slow
  "Takes an svg file representing a map of Ireland divided into regions
   and generates a choropleth map where the colours represent the value
   of the given variable"
  [req]
  (try
    (let [{:keys [year months model scenario variable fill region]} req
          models (if (= model "ensemble")
                   (get ensembles scenario)
                   (vector (vector model scenario)))
          regions-svg (case (:regions req)
                        "Counties" counties-svg
                        "Provinces" provinces-svg)
          regions (case (:regions req)
                    "Counties" counties
                    "Provinces" provinces)
          delta (= (:abs req) "Change")
          diff-fn (if delta diff-by-county abs-data)
          colour-scheme (if (temp-var? variable) (reverse color-brewer/OrRd-7) (reverse color-brewer/RdBu-11))
          min (get-in mins [(:abs req) variable])
          max (get-in maxs [(:abs req) variable])
          offset {"JJA" 4, "DJF" 0, "SON" 1 "MAM" 2}]
      (log/info "Min: " min " Max: " max)
      {:status 200
       :headers {"Content-Type" "image/svg+xml"}
       :body
       (let [choropleth
             (reduce
              #(transform-xml
                %1
                [{:id %2}]
                (fn [elem]
                  (let [link (make-url "climate-information/projections"
                                       (assoc-in req [:region] %2))
                        vals (filter (fn [x] (not (nil? x))) (diff-fn %2 year months models variable))
                        val (/ (reduce + 0 vals) (count vals))]
                    (log/info "Value: " val " From: " req)
                    [:a {:xlink:href link :target "_top"}
                     (-> (add-attrs elem :onmouseover
                                    (if (nil? val)
                                      "Unknown"
                                      (str "value(evt,'"
                                           (->
                                            val
                                            (* 100)
                                            round
                                            (/ 100)
                                            float)
                                           (if (temp-var? variable) "°C " (if delta "% " "mm/hr"))
                                           %2
                                           "')")))
                         (colour-on-linear %2 year months models variable region min max diff-fn colour-scheme)
                         (add-style :stroke (if (= region (:id (second elem))) "red" "grey")))])))
              regions-svg			
              regions)
             legend (reduce #(transform-xml %1
                                            [{:id (str "col-" %2)}]
                                            (fn [node] (add-style node :fill (nth colour-scheme
                                                                                  (+ %2 (offset months))))))
                            choropleth
                            (range 0 (count colour-scheme)))
             values (reduce #(transform-xml %1
                                            [{:id (str "val-" %2)}]
                                            (fn [node] (set-content node (str (->
                                                                               ((scale/linear :domain [0 (count colour-scheme)]
                                                                                              :range [max min])
                                                                                (+ %2 (offset months)))
                                                                               (* 100)
                                                                               round
                                                                               (/ 100)
                                                                               float)))))
                            legend
                            (range 0 (inc (count colour-scheme))))
             units (transform-xml values [{:id "units"}]
                                  (fn [node] (set-content node
                                                          (if (temp-var? variable)
                                                            (str "°Celsius" (when delta " change"))
                                                            (if delta "% change" "mm/hr")))))
             selected (transform-xml units [{:id "selected"}]
                                     (fn [node] (set-content node (str "Selected: " (:region req)))))]
         (emit selected))})
    (catch Exception ex
      (log/info ex)
      (log/info req)
      "We do apologise. There are no data available for the selection you have chosen.
Please select another combination of decade/variable/projection")))
  
  
(def regions-map (memoize regions-map-slow))
