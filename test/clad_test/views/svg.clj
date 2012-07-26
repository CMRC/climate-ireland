(ns clad-test.views.svg
  (:use clojure.test
        clad.views.svg))

(deftest test-linear-rgb
  (let [mid (linear-rgb 0 50 100)]
    ;;check valid colour string
    (println mid)
    (is (re-find #"#(\p{XDigit}{2}){3}" mid))))

