(ns clad-test.views.svg
  (:use clojure.test
        clad.views.svg)
  (:require [c2.scale :as scale]
            [vomnibus.color-brewer :as color-brewer]))

(deftest test-linear-rgb
  (let [mid (linear-rgb 75 [50 100] color-brewer/OrRd-7)]
    ;;check valid colour string
    (is (re-find #"rgb\(\d{1,3},\d{1,3},\d{1,3}\)" mid))))

