(defproject clad "0.1.0-SNAPSHOT"
            :description "CLAD: Coastal Adapatation and Climate Resilence for Ireland"
            :dependencies [[clj-stacktrace "0.2.4"]
                           [org.clojure/clojure "1.3.0"]
                           [enlive "1.0.0"]
                           [noir "1.2.0"]
                           [org.clojars.sritchie09/gdal-java "1.8.0"]
                           [incanter "1.2.3"]]
            :native-dependencies [[jriengine "0.8.4"]]
            :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]
                               [native-deps "1.0.5"]]
            :main clad.server)

