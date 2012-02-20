(ns clad.views.svg
  (:use [analemma.xml :only [emit add-content add-attrs
			     parse-xml transform-xml filter-xml]]
        [examples.svg]
	analemma.svg
	[clojure.java.io :only [file]]))

(defn counties-map []
  {:status 200
   :headers {"Content-Type" "image/svg+xml"}
   :body
   (emit (transform-xml (parse-xml (slurp "src/clad/views/counties.svg"))
                        [{:id "Cork County"}]
                        #(add-style % :fill "#000000")))})