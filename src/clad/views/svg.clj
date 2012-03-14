(ns clad.views.svg
  (:use [analemma.xml :only [emit add-content add-attrs
			     parse-xml transform-xml filter-xml]]
	analemma.svg
	analemma.xml
        clad.models.couch
        incanter.stats
        clojure.contrib.math
	[clojure.java.io :only [file]]))

(def counties-svg (parse-xml (slurp "src/clad/views/counties.svg")))

(defn counties-data [year months model scenario variable] (map #(bycounty-memo % year months model scenario variable) counties))

(defn quartiles [year months variable] (map #(/ (round (* % 100)) 100)
                           (quantile (counties-data year months variable))))
(defn colour-on-quartiles [elem county year months variable]
  (add-style elem :fill (cond (< (bycounty-memo county year months variable) (nth (quartiles year months variable) 1)) "#56b"
                              (< (bycounty-memo county year months variable) (nth (quartiles year months variable) 2)) "#769"
                              (< (bycounty-memo county year months variable) (nth (quartiles year months variable) 3)) "#967"
                              :else "#b65")))

(defn colour-on-linear [elem county year months model scenario variable]
  (let [cd (counties-data year months model scenario variable)
        min (apply min cd)
        max (apply max cd)
        step (/ 100 (+ 1 (- max min)))
        val (bycounty-memo county year months model scenario variable)
        red (+ 100 (round (* step (- val min))))
        green 96
        blue (- 200 (round (* step (- val min))))]
    (add-style elem :fill (str "#" (format "%x" red) (format "%x" green) (format "%x" blue)))))

(defn counties-map [year months model scenario variable fill]
  (let [fill-fns {"linear" colour-on-linear,
                  "quartiles" colour-on-quartiles}
        q1 (if (= fill "quartiles") (str (float (nth (quartiles year months variable) 1))) "")
        q2 (if (= fill "quartiles") (str (float (nth (quartiles year months variable) 2))) "")
        q3 (if (= fill "quartiles") (str (float (nth (quartiles year months variable) 3))) "")]
    {:status 200
     :headers {"Content-Type" "image/svg+xml"}
     :body
     (emit (-> (reduce #(transform-xml %1
                                       [{:id %2}]
                                       (fn [elem]
                                         (-> (add-attrs elem :onmouseover
                                                        (str "value(evt,'"
                                                             (float (/ (round (* 100 (bycounty-memo %2 year months model scenario variable))) 100))
                                                             " : "
                                                             %2
                                                             "')"))
                                             ((fill-fns fill) %2 year months model scenario variable))))
                       counties-svg
                       counties)
               (transform-xml
                [{:id "q0"}]
                #(set-content % (->>
                                 (counties-data year months model scenario variable)
                                 (apply min)
                                 (* 10)
                                 round
                                 (/ 10)
                                 float
                                 str)))
               (transform-xml
                [{:id "q1"}]
                #(set-content % q1))
               (transform-xml
                [{:id "q2"}]
                #(set-content % q2))
               (transform-xml
                [{:id "q4"}]
                #(set-content % (str (float (/ (round (* 10 (apply max (counties-data year months model scenario variable)))) 10)))))
               (transform-xml
                [{:id "q3"}]
                #(set-content % q3))))}))
