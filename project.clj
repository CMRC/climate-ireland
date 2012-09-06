(defproject clad "0.2.0-SNAPSHOT"
            :description "Irish Climate Information Platform"
            :dependencies [[clj-stacktrace "0.2.4"]
                           [org.clojure/clojure "1.4.0"]
                           [enlive "1.0.0"]
                           [noir "1.2.1"]
                           [org.clojars.pallix/analemma "1.0.0-SNAPSHOT"]
                           [incanter "1.2.4"]
                           [clj-time "0.3.7"]
                           [com.ashafa/clutch "0.4.0-SNAPSHOT"]
			   [org.clojars.pallix/batik "1.7.0"]
                           [clj-logging-config "1.9.7"]
                           [com.cemerick/friend "0.0.9"]
                           [org.clojure/clojurescript "0.0-1011"]
                           [com.keminglabs/c2 "0.2.1"]
                           [com.keminglabs/vomnibus "0.3.1"]]
            :plugins [[lein-ring "0.7.1"]]
            :main clad.server)

