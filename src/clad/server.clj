(ns clad.server
  (:gen-class)
  (:require [noir.server :as server]
            [clad.views.welcome]
            [clad.views.svg]
            [clad.views.charts]
            [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds]))
  (:use [clad.pw]))

(server/load-views "src/clad/views/")

(server/add-middleware 
 friend/authenticate 
 {:credential-fn (partial creds/bcrypt-credential-fn users) 
  :workflows [(workflows/interactive-form)] 
  :login-uri "/login" 
  :unauthorized-redirect-uri "/login" 
  :default-landing-uri "/ci/about"}) 

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8888"))]
    #_(log-config/set-logger! :level :debug
                            :out (org.apache.log4j.FileAppender.
                                  (org.apache.log4j.EnhancedPatternLayout. org.apache.log4j.EnhancedPatternLayout/TTCC_CONVERSION_PATTERN)
                                  "logs/foo.log"
                                  true))
    (server/start port {:mode mode
                        :ns 'clad}))
  (log/info "Server started"))
