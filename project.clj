(defproject clad "0.1.0-SNAPSHOT"
            :description "CLAD: Coastal Adapatation and Climate Resilence for Ireland"
            :dependencies [[clj-stacktrace "0.2.4"]
                           [org.clojure/clojure "1.3.0"]
                           [enlive "1.0.0"]
                           [noir "1.2.0"]
                           [org.clojars.sritchie09/gdal-java "1.8.0"]
                           [org.clojars.pallix/analemma "1.0.0-SNAPSHOT"]
                           [incanter "1.2.3"]
                           [com.ashafa/clutch "0.3.1-SNAPSHOT"]]
            :native-dependencies [[jriengine "0.8.4"]]
            :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]
                               [native-deps "1.0.5"]]
            :git-dependencies [["https://github.com/jolby/rincanter.git"]]
            :extra-classpath-dirs [".lein-git-deps/rincanter/src/"]
            :main clad.server)

