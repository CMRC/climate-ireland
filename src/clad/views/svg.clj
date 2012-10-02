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
            [vomnibus.color-brewer :as color-brewer]
            [clojure.tools.logging :as log]
            [clojure.stacktrace :as trace])
  (:import (org.apache.batik.transcoder.image PNGTranscoder)
           (org.apache.batik.transcoder TranscoderInput
                                        TranscoderOutput)
           (java.io ByteArrayOutputStream
                    ByteArrayInputStream)))

(def counties-svg (parse-xml (slurp "src/clad/views/counties.svg")))
(def provinces-svg (parse-xml (slurp "src/clad/views/provinces.svg")))

(defn counties-data [year months model scenario variable]
  (map #(data-by-county % year months model scenario variable) counties))

(defn quartiles-slow [year months model scenario variable cp diff-fn]
  (map #(/ (round (* % 100)) 100)
       (quantile (map (fn [county] (diff-fn county year months model scenario variable))
                      (case cp :county counties :province provinces)))))

(def quartiles (memoize quartiles-slow))
  
(defn colour-on-quartiles [elem county year months model scenario variable]
  )

(def colour-scheme color-brewer/RdYlBu-11)

(defn linear-rgb [val domain]
  "Returns a colour string based on the value"
  (if (nil? val)
    "black"
    (let [colour-scale (let [s (scale/linear :domain domain
                                             :range [0 (dec (count colour-scheme))])]
                         (fn [d] (nth colour-scheme (floor (s d)))))]
      (colour-scale val))))

(defn colour-on-linear
  "Calls CouchDB and returns a colour string based on the query parameters"
  [elem county year months model scenario variable region lmin lmax diff-fn]
  (let [domain [lmax lmin]
        val (diff-fn county year months model scenario variable)]
    (add-style elem :fill (linear-rgb val domain))))

(defn regions-map-slow
  "Takes an svg file representing a map of Ireland divdided into regions
   and generates a choropleth map where the colours represent the value
   of the given variable"
  [cp req]
  (try
    (let [{:keys [year months model scenario variable fill region]} req
          regions-svg (case cp
                        :county counties-svg
                        :province provinces-svg)
          regions (case cp
                    :county counties
                    :province provinces)
          diff-fn (if (= model "ICARUS")
                    data-by-county ;; ICARUS data is already expressed as a delta
                    (if (temp-var? variable) temp-diff-data diff-data))
          min (decadal-min months model scenario variable regions diff-fn)
          max (decadal-max months model scenario variable regions diff-fn)]
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
                                       (assoc-in req [:region] %2)
                                       :counties? (= cp :county))
                        val (diff-fn %2 year months model scenario variable)]
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
                                           (if (temp-var? variable) "°C " "% ")
                                           %2
                                           "')")))
                         (colour-on-linear %2 year months model scenario variable region min max diff-fn)
                         (add-style :stroke (if (= region (:id (second elem))) "white" "black")))])))
              regions-svg			
              regions)
             legend (reduce #(transform-xml %1
                                            [{:id (str "col-" %2)}]
                                            (fn [node] (add-style node :fill (nth colour-scheme %2))))
                            choropleth
                            (range 0 11))
             values (reduce #(transform-xml %1
                                            [{:id (str "val-" %2)}]
                                            (fn [node] (set-content node (str (->
                                                                               ((scale/linear :domain [0 11]
                                                                                              :range [max min])
                                                                                %2)
                                                                               (* 100)
                                                                               round
                                                                               (/ 100)
                                                                               float)))))
                            legend
                            (range 11 -1 -1))
             units (transform-xml values [{:id "units"}]
                                  (fn [node] (set-content node (if (temp-var? variable) "°Celsius change" "% change"))))
             pushpin (transform-xml units [{:id "Ireland"}]
                                    (fn [node] 
                                      (add-content [:circle {:cx "200" :cy "200" :r "100" :fill "black" :stroke-width "5"}])))
             selected (transform-xml values [{:id "selected"}]
                                     (fn [node] (set-content node (str "Selected: " (:region req)))))]
         (emit selected))})
    #_(catch NullPointerException ex
      (log/info req)
      "We do apologise. There is no data available for the selection you have chosen.
Please select another combination of decade/variable/projection")))
  
  
(def regions-map (memoize regions-map-slow))

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

