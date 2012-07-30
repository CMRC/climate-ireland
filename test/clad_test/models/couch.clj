(ns clad-test.models.couch
  (:use clojure.test))

(defn test-temp-diff []
  (let [ref 273.15
        proj 290.12]
    (is (temp-diff ref proj) (- proj ref))))
