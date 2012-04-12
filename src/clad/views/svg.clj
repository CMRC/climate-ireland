(ns clad.views.svg
  (:use [analemma.xml :only [emit add-content add-attrs
			     parse-xml transform-xml filter-xml]]
	analemma.svg
	analemma.xml
        clad.models.couch
        incanter.stats
        clojure.contrib.math
	[clojure.java.io :only [file]]))

(def counties-svg (parse-xml (slurp "src/clad/views/provinces.svg")))

(defn counties-data [year months model scenario variable] (map #(data-by-county % year months model scenario variable) counties))

(defn quartiles [year months variable] (map #(/ (round (* % 100)) 100)
                           (quantile (counties-data year months variable))))

(defn colour-on-quartiles [elem county year months variable]
  (add-style elem :fill (cond (< (data-by-county county year months variable) (nth (quartiles year months variable) 1)) "#56b"
                              (< (data-by-county county year months variable) (nth (quartiles year months variable) 2)) "#769"
                              (< (data-by-county county year months variable) (nth (quartiles year months variable) 3)) "#967"
                              :else "#b65")))

(defn colour-on-linear [elem county year months model scenario variable]
  (let [cd (counties-data year months model scenario variable)
        min 8.5
        max 12.5
        step (/ 200 (- max min))
        val (if (= model "ensemble")
                (- (/ (reduce #(+ %1
                               (data-by-county county year months (first %2) (second %2) variable))
                           0
                           ensemble) (count ensemble)) 273.15)
                (- (data-by-county county year months model scenario variable)
	       273.15))
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
                  "quartiles" colour-on-quartiles}
        q1 (if (= fill "quartiles") (str (float (nth (quartiles year months variable) 1))) "")
        q2 (if (= fill "quartiles") (str (float (nth (quartiles year months variable) 2))) "")
        q3 (if (= fill "quartiles") (str (float (nth (quartiles year months variable) 3))) "")]
    {:status 200
     :headers {"Content-Type" "image/svg+xml"}
     :body
     (emit (reduce #(transform-xml
                     %1
                     [{:id %2}]
                     (fn [prov]
                       (-> (transform-xml
                            prov
                            [{:class "val"}]
                            (fn [elem]
                              (println "\n val " %2)
                              (set-content elem (str (float (/ (round (* 100 (- (data-by-county %2 year months model scenario variable)
                                                                                273.15))) 100))))))
                           (transform-xml
                            [{:class "shape"}]
                            (fn [elem]
                              (println "\n shape " %2)
                              ((fill-fns fill) elem %2 year months model scenario variable))))))
                   counties-svg
                   provinces))})))
