(ns clad.views.svg
  (:use [analemma.xml :only [emit add-content add-attrs
			     parse-xml transform-xml filter-xml]]
	analemma.svg
	analemma.xml
        clad.models.couch
        incanter.stats
        clojure.contrib.math
	[clojure.java.io :only [file]])
  (:import (org.apache.batik.transcoder.image PNGTranscoder)
           (org.apache.batik.transcoder TranscoderInput
                                        TranscoderOutput)
           (java.io ByteArrayOutputStream
                    ByteArrayInputStream)))

(def counties-svg (parse-xml (slurp "src/clad/views/counties.svg")))
(def provinces-svg (parse-xml (slurp "src/clad/views/provinces.svg")))

(defn counties-data [year months model scenario variable]
  (map #(data-by-county % year months model scenario variable) counties))

(defn quartiles-slow [year months model scenario variable]
  (map #(/ (round (* % 100)) 100)
       (quantile (map (fn [county] (temp-diff-data county year months model scenario variable))
                      counties))))

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
  (let [step (/ 100 (- max min))
        red (+ 50 (round (* step (float (- val min)))))
        green 96
        blue (- 200 (round (* step (float (- val min)))))]
    (str "#" (format "%x" red) (format "%x" green) (format "%x" blue))))
  
(defn colour-on-linear [elem county year months model scenario variable]
  (let [val (temp-diff-data county year months model scenario variable)
        min (nth (quartiles year months model scenario variable) 0)
        max (nth (quartiles year months model scenario variable) 4)]
    (add-style elem :fill (linear-rgb val min max))))

(defn regions-map 
  ([year months variable]
     (regions-map year months "ensemble" "ensemble" variable "linear"))
  ([year months model scenario variable fill region]
     (let [regions-svg (case region
                         :county counties-svg
                         :province provinces-svg)
           regions (case region
                     :county counties
                     :province provinces)
           fill-fns {"linear" colour-on-linear,
                     "quartiles" colour-on-quartiles}
           min (nth (quartiles year months model scenario variable) 0)
           max (nth (quartiles year months model scenario variable) 4)
           mid (/ (+ max min) 2)]
       {:status 200
        :headers {"Content-Type" "image/svg+xml"}
        :body
        (emit (->
               (reduce #(transform-xml
                         %1
                         [{:id %2}]
                         (fn [elem]
                           (let [link (str "/ci/welcome/svgbar/"
                                           (apply str
                                                  (interpose "/" [%2 year months model scenario
                                                                  variable fill])))]
                             [:a {:xlink:href link :target "_top"}
                              (-> (add-attrs elem :onmouseover
                                             (str "value(evt,'"
                                                  (->
                                                   (temp-diff-data %2 year months model scenario variable)
                                                   (* 100)
                                                   round
                                                   (/ 100)
                                                   float)
                                                  "K : "
                                                  %2
                                                 "')"))
                                  ((fill-fns fill) %2 year months model scenario variable))])))
                       regions-svg			
                       regions)
               (transform-xml
                [{:id "min-text"}]
                #(set-content % (str (float min))))
               (transform-xml
                [{:id "max-text"}]
                #(set-content % (str (float max))))
               (transform-xml
                [{:id "min"}]
                #(add-style % :fill (linear-rgb min min max)))
               (transform-xml
                [{:id "mid"}]
                #(add-style % :fill (linear-rgb mid min max)))
               (transform-xml
                [{:id "max"}]
                #(add-style % :fill (linear-rgb max min max)))))})))

  
(defn counties-map 
  ([year months variable]
     (regions-map year months "ensemble" "ensemble" variable "linear" :county))
  ([year months model scenario variable fill]
     (regions-map year months model scenario variable fill :county)))

(defn provinces-map 
  ([year months variable]
     (regions-map year months "ensemble" "ensemble" variable "linear" :province))
  ([year months model scenario variable fill]
     (regions-map year months model scenario variable fill :province)))

(defn counties-map-png 
  ([year months model scenario variable fill]
    (let [input (TranscoderInput. (str "http://www.climateireland.ie:8080/ci/svg/" year "/" months "/" model
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

(defn compare-map 
  [year1 year2 months variable]
  {:status 200
   :headers {"Content-Type" "image/svg+xml"}
      :title (str variable " " year1 " v " year2)
   :body
   (emit (reduce #(transform-xml
                   %1
                   [{:id %2}]
                   (fn [prov]
                     (let [y1 (ensemble-data %2 year1 months variable)
                           y2 (ensemble-data %2 year2 months variable)
                           g (-> (transform-xml
                                  prov
                                  [{:class "val"}]
                                  (fn [elem]
                                    (set-content elem (-> (- y2 y1)
                                                          (/ y1)
                                                          (* 10000)
                                                          round
                                                          (/ 100)
                                                          float
                                                          (str "%")))))
                                 (transform-xml
                                  [{:class "shape"}]
                                  (fn [elem]
                                    (let [diff (- y2 y1)
                                          base 0x40
                                          mult 100
                                          r (if (neg? diff) (+ base (round (abs (* diff mult)))) base)
                                          g (if (pos? diff) (+ base (round (* diff mult))) base)
                                          b base
                                          fill (str "#" (format "%02x" r) (format "%02x" g) (format "%02x" b))]
                                      (add-style elem :fill fill)))))
                           link "/ci/welcome"]
                       [:a {:xlink:href link} g])))
                 provinces-svg
                 provinces))})