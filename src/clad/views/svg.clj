(ns clad.views.svg
  (:use [analemma.xml :only [emit add-content add-attrs
			     parse-xml transform-xml filter-xml]]
	analemma.svg
        clad.models.gdal
	[clojure.java.io :only [file]]))

(def counties-svg (parse-xml (slurp "src/clad/views/counties.svg")))

(defn counties-map []
  (let [run "temp2020djf"]
    {:status 200
     :headers {"Content-Type" "image/svg+xml"}
     :body
     (emit (reduce #(transform-xml %1
                                   [{:id %2}]
                                   (fn [elem] (add-style elem :fill (if (> (bycounty-memo %2 run) 8) "#5555ff" "#ff5555"))))
                   counties-svg
                   counties))}))
                   