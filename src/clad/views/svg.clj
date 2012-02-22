(ns clad.views.svg
  (:use [analemma.xml :only [emit add-content add-attrs
			     parse-xml transform-xml filter-xml]]
	analemma.svg
        clad.models.gdal
        incanter.stats
        clojure.contrib.math
	[clojure.java.io :only [file]]))

(def run "temp2020djf")

(def counties-svg (parse-xml (slurp "src/clad/views/counties.svg")))

(def quartiles (map #(/ (round (* % 1000)) 1000)
                    (quantile (map #(bycounty-memo % run) counties))))

(defn counties-map []
  {:status 200
   :headers {"Content-Type" "image/svg+xml"}
   :body
   (emit (reduce #(transform-xml %1
                                 [{:id %2}]
                                 (fn [elem] (add-style elem :fill (cond (< (bycounty-memo %2 run) (nth quartiles 1)) "#56b"
                                                                        (< (bycounty-memo %2 run) (nth quartiles 2)) "#769"
                                                                        (< (bycounty-memo %2 run) (nth quartiles 3)) "#967"
                                                                        :else "#b65"))))
                 counties-svg
                 counties))})
                  