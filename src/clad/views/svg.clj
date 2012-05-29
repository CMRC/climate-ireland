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

(defn quartiles [year months model scenario variable] (map #(/ (round (* % 100)) 100)
                                                           (quantile (counties-data year months model scenario variable))))

(defn colour-on-quartiles [elem county year months model scenario variable]
  (add-style elem :fill (cond (< (data-by-county county year months model scenario variable) (nth (quartiles year months model scenario variable) 1)) "#56b"
                              (< (data-by-county county year months model scenario variable) (nth (quartiles year months model scenario variable) 2)) "#769"
                              (< (data-by-county county year months model scenario variable) (nth (quartiles year months model scenario variable) 3)) "#967"
                              :else "#b65")))

(defn ensemble-data [county year months variable]
  (/ (reduce #(+ %1 (data-by-county county year months (first %2) (second %2) variable))
             0
             ensemble) (count ensemble)))

(defn diff-data [county year months model scenario variable]
  (if (= model "ensemble")
    (- (ensemble-data county year months variable) 273.15)
    (let [ref (ref-data county months model variable)
          comp (data-by-county county year months model scenario variable)
          res (->
               (- comp ref)
               (/ ref)
               (* 10000)
               round
               (/ 100)
               float)]
      res)))

(defn colour-on-linear [elem county year months model scenario variable]
  (let [cd (counties-data year months model scenario variable)
        min 0
        max 1.0
        step (/ 200 (- max min))
        val (diff-data county year months model scenario variable)
        red (+ 100 (round (* step (- val min))))
        green 96
        blue (- 200 (round (* step (- val min))))]
    (add-style elem :fill (str "#" (format "%x" red) (format "%x" green) (format "%x" blue))
               :fill-opacity 1)))
                                     
(defn counties-map 
  ([year months variable]
     (counties-map year months "ensemble" "ensemble" variable "linear"))
  ([year months model scenario variable fill]
     (let [fill-fns {"linear" colour-on-linear,
                     "quartiles" colour-on-quartiles}]
       {:status 200
        :headers {"Content-Type" "image/svg+xml"}
        :body
        (emit (reduce #(transform-xml
                        %1
                        [{:id %2}]
                        (fn [elem]
                          (-> (add-attrs elem :onmouseover
                                         (str "value(evt,'"
                                              (diff-data %2 year months model scenario variable)
                                              "% : "
                                              %2
                                              "')"))
                              ((fill-fns fill) %2 year months model scenario variable))))
                      counties-svg			
                      counties))})))    

(defn counties-map-png 
  ([year months model scenario variable fill]
    (let [input (TranscoderInput. (str "http://www.climateireland.ie:8080/svg/" year "/" months "/" model
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
                           y2 (ensemble-data %2 year2 months variable)]
                       (-> (transform-xml
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
                                (add-style elem :fill fill))))))))
                 provinces-svg
                 provinces))})