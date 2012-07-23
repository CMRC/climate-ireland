(ns clad.server
  (:require [noir.server :as server]
            [clj-logging-config.log4j :as log-config]
            [clojure.tools.logging :as log])
  (:use ring.middleware.params))

(server/load-views "src/clad/views/")

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode
                        :ns 'clad})))

(log-config/set-logger! :level :debug
                        :out (org.apache.log4j.FileAppender.
                              (org.apache.log4j.EnhancedPatternLayout. org.apache.log4j.EnhancedPatternLayout/TTCC_CONVERSION_PATTERN)
                              "logs/foo.log"
                              true))

(log/info "Server started")

