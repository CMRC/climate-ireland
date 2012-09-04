(ns clad-test.views.svg
  (:use clojure.test
        clad.views.svg))

(deftest test-linear-rgb
  (let [mid (linear-rgb 75 [50 100])]
    ;;check valid colour string
    (is (re-find #"rgb\(\d{3},\d{3},\d{3}\)" mid))))

