(ns clad.models.couch
  (:require [com.ashafa.clutch :as clutch]
            [cemerick.friend [credentials :as creds]]
            [clojure.tools.logging :as log])
  (:use [com.ashafa.clutch.view-server]
        [clojure.math.numeric-tower]))

(def users-db "climate_dev2")
(def db "climate_dev4")

#_(clutch/with-db users-db
    (clutch/save-view "users"
                      (clutch/view-server-fns
                       :clojure
                       {:users
                        {:map (fn [doc] (if (:username doc) [[(:username doc) doc]]))}})))

#_(clutch/with-db users-db
  (clutch/put-document {:username "sea"
                        :password (creds/hash-bcrypt "longford")
                        :roles #{::user}}))

#_(clutch/configure-view-server db (view-server-exec-string))

(defn get-users []
  (try
    (clutch/with-db users-db
      (reduce #(assoc %1 (:key %2) (:value %2)) {} (clutch/get-view "users" :users)))
    ;;for testing locally without the database we supply a default password
    (catch java.io.IOException e {"local" {:username "local"
                                           :password (creds/hash-bcrypt "local")
                                           :roles #{::user}}})))
#_(get-users)

(def provinces ["Leinster" "Munster" "Connaught" "Ulster"])


(defn save-views []
  (clutch/with-db db
    (clutch/save-view "vals"
                      (clutch/view-server-fns
                       :clojure
                       {:by-ym
                        {:map (fn [doc] [[[(:year doc) (:months doc)]
                                          doc]])}}))
    (clutch/save-view "counties"
                      (clutch/view-server-fns
                       :clojure
                       {:by-county
                        {:map (fn [doc] [[[(:region doc)
                                           (:months doc) (:model doc) 
                                           (:scenario doc) (:datum.variable doc)]
                                          doc]])}}))
    (clutch/save-view "counties-year"
                      (clutch/view-server-fns
                       :clojure
                       {:by-county-year
                        {:map (fn [doc] [[[(:region doc)
                                           (:year doc) (:months doc)
                                           (:model doc) (:scenario doc) (:datum.variable doc)]
                                          doc]])}}))
    (clutch/save-view "models"
                      (clutch/view-server-fns
                       :clojure
                       {:by-model
                        {:map (fn [doc] [[(:year doc),doc]])}}))))

#_(save-views)

(defn get-run-data [year months]
  (try
    (clutch/with-db db
      (map #(:value %) (clutch/get-view "vals" :by-ym {:key [year months]})))
    (catch java.net.ConnectException e [{:region "Kilkenny"}])))

(defn get-county-data [county months model scenario variable]
  (try
    (clutch/with-db db
      (clutch/get-view "counties" :by-county {:key [county months model scenario variable]}))
    (catch java.net.ConnectException e [{:region "Kilkenny"}])))

(defn get-county-by-year [county year months model scenario variable]
  (try
    (clutch/with-db db
      (clutch/get-view "counties-year" :by-county-year {:key [county year months model scenario variable]})
      (catch java.net.ConnectException e [{:region county :year year :months months :model model
                                           :scenario scenario :variable variable :datum.value 1}]))))
(defn get-models []
  (try
    (clutch/with-db db
      (clutch/get-view "models" :by-model))
    (catch java.net.ConnectException e [{:region "Kilkenny"}])))

(def ensemble {"ensemble"
               [["ICARUS" "a2"]
                ["ICARUS" "b2"]
                ["CGCM31" "A2"]
                ["CGCM31" "A1B"]
                ["HadGEM" "RCP85"]
                ["HadGEM" "RCP45"]]
               "high"
               [["HadGEM" "RCP85"]
                ["CGCM31" "A2"]
                ["ICARUS" "a2"]]
               "medium"
               [["ICARUS" "b2"]
                ["CGCM31" "A2"]
                ["ICARUS" "a2"]]
               "low"
               [["HadGEM" "RCP45"]
                ["CGCM31" "A1B"]]})

(def temp-vars ["T_2M" "TMAX_2M" "TMIN_2M"])

(defn temp-var? [variable] (some #(= variable %) temp-vars))

(defn percent [comp ref]
  (->
   (- comp ref)
   (/ ref)
   (* 10000)
   round
   (/ 100)
   float))

(defn data-by-county [county year months model scenario variable]
  (letfn [(get-data [county year months model scenario variable]
            (->> (get-county-by-year county year months model scenario variable)
                 first 
                 :value
                 :datum.value))]
    (if (= model "ensemble")
      (let [d (map #(get-data county year months (first %) (second %) variable)
                   (get ensemble scenario))]
        (/ (reduce + 0 d) (count d)))
      (get-data county year months model scenario variable))))

(defn abs-data [county year months model scenario variable]
  (if (temp-var? variable)
    (- (data-by-county county year months model scenario variable) 273.15)
    (data-by-county county year months model scenario variable)))

(defn ref-data-slow [county months model scenario variable]
  (try
    (clutch/with-db db
      (data-by-county county "1961-1990" months model
                      (if (= model "ICARUS") scenario "C20") variable))
    (catch java.net.ConnectException e [{:region "Kilkenny"}])))

(def ref-data (memoize ref-data-slow))

(defn diff-by-county [county year months model scenario variable]
  (let [d (->> (get-county-by-year county year months model scenario variable)
                  first 
                  :value
                  :datum.value)
        ref (ref-data county months model scenario variable)]
    (when d
      (if (temp-var? variable)
        (- d ref)
        (percent d ref)))))

(defn diff-data
  "calculate the display value based on a difference function between
computed data and reference data. Difference function is subtraction for
temperature data and percentage difference for everything else"
  [county year months model scenario variable]
  (if (= model "ensemble")
    (let [rawcomp (filter #(not (nil? %))
                          (map #(diff-by-county county year months (first %) (second %) variable)
                               (get ensemble scenario)))
          comp (when (> (count rawcomp) 0)
                 (/ (reduce + 0 rawcomp)
                    (count rawcomp)))]
      comp)
    (diff-by-county county year months model scenario variable)))

(def bycounty-memo (memoize data-by-county))

(def counties-by-province
  {"Carlow" "Leinster"
   "Cavan" "Ulster"
   "Clare" "Connaught"
   "Cork" "Munster"
   "Donegal" "Ulster"
   "Dublin" "Leinster"
   "Galway" "Connaught"
   "Kerry" "Munster"
   "Kildare" "Leinster"
   "Kilkenny" "Leinster"
   "Laois" "Leinster"
   "Leitrim" "Connaught"
   "Limerick" "Munster"
   "Longford" "Leinster"
   "Louth" "Leinster"
   "Mayo" "Connaught"
   "Meath" "Leinster"
   "Monaghan" "Ulster"
   "North Tipperary" "Munster"
   "Offaly" "Leinster"
   "Roscommon" "Connaught"
   "Sligo" "Connaught"
   "South Tipperary" "Munster"
   "Waterford" "Munster"
   "Westmeath" "Leinster"
   "Wexford" "Leinster"
   "Wicklow" "Leinster"
   "NI" "Ulster"})

(def counties (keys counties-by-province))

(defn all-counties [year months model scenario variable]
  (map #(str (data-by-county % year months model scenario variable) ",") counties))

(defn make-url [view req & {:keys [counties?] :or {counties? false}}]
  (str "/ci/"
       view "/"
       (apply str (interpose "/" (vals req)))
       (when counties? "/counties")))

(defn put-submit [req]
  (do
    (log/info req)
    (clutch/with-db db
      (clutch/put-document req))))
