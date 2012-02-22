(ns clad.views.svg
  (:use [analemma.xml :only [emit add-content add-attrs
			     parse-xml transform-xml filter-xml]]
	analemma.svg
	analemma.xml
        clad.models.gdal
        incanter.stats
        clojure.contrib.math
	[clojure.java.io :only [file]]))

(def counties-svg (parse-xml (slurp "src/clad/views/counties.svg")))

(defn counties-data [run] (map #(bycounty-memo % run) counties))

(defn quartiles [run] (map #(/ (round (* % 100)) 100)
                           (quantile (counties-data run))))

(defn colour-on-quartiles [elem county run]
  (add-style elem :fill (cond (< (bycounty-memo county run) (nth (quartiles run) 1)) "#56b"
                              (< (bycounty-memo county run) (nth (quartiles run) 2)) "#769"
                              (< (bycounty-memo county run) (nth (quartiles run) 3)) "#967"
                              :else "#b65")))

(defn colour-on-linear [elem county run]
  (let [cd (counties-data run)
        min (apply min cd)
        max (apply max cd)
        step (/ 100 (- max min))
        val (bycounty-memo county run)
        red (+ 100 (round (* step (- val min))))
        green 96
        blue (- 200 (round (* step (- val min))))]
    (add-style elem :fill (str "#" (format "%x" red) (format "%x" green) (format "%x" blue)))))

(defn counties-map [run fill]
  (let [fill-fns {"linear" colour-on-linear,
                  "quartiles" colour-on-quartiles}
        q1 (if (= fill "quartiles") (str (float (nth (quartiles run) 1))) "")
        q2 (if (= fill "quartiles") (str (float (nth (quartiles run) 2))) "")
        q3 (if (= fill "quartiles") (str (float (nth (quartiles run) 3))) "")]
    {:status 200
     :headers {"Content-Type" "image/svg+xml"}
     :body
     (emit (-> (reduce #(transform-xml %1
                                       [{:id %2}]
                                       (fn [elem] ((fill-fns fill) elem %2 run)))
                       counties-svg
                       counties)
               (transform-xml
                [{:id "q0"}]
                #(set-content % (str (float (/ (round (* 10 (apply min (counties-data run)))) 10)))))
               (transform-xml
                [{:id "q1"}]
                #(set-content % q1))
               (transform-xml
                [{:id "q2"}]
                #(set-content % q2))
               (transform-xml
                [{:id "q4"}]
                #(set-content % (str (float (/ (round (* 10 (apply max (counties-data run)))) 10)))))
               (transform-xml
                [{:id "q3"}]
                #(set-content % q3))))}))
