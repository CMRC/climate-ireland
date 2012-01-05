(ns clad.server
  (:require [noir.server :as server])
  (:use ring.middleware.params))

(server/load-views "src/clad/views/")

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode
                        :ns 'clad})))

