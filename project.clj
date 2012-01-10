(defproject clad "0.1.0-SNAPSHOT"
            :description "CLAD: Coastal Adapatation and Climate Resilence for Ireland"
            :dependencies [[clj-stacktrace "0.2.4"]
                           [org.clojure/clojure "1.3.0"]
                           [enlive "1.0.0"]
                           [noir "1.2.0"]]
            :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]]
            :main clad.server)

