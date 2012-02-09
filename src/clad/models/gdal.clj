(ns clad.models.gdal
  (:use incanter.core)
  (:import(java.io ByteArrayOutputStream
                   ByteArrayInputStream)))

(use '(com.evocomputing rincanter))

(def icarus-runs
  ["precip2020djf"
  "precip2020mam"
  "precip2020jja"
  "precip2020son"
  "precip2050djf"
  "precip2050mam"
  "precip2050jja"
  "precip2050son"
  "precip2080djf"
  "precip2080mam"
  "precip2080jja"
  "precip2080son"])

(def counties
       ["Carlow"
        "Cavan"
        "Clare"
        "Cork"
        "Donegal"
        "Dublin"
        "Galway"
        "Kerry"
        "Kildare"
        "Kilkenny"
        "Laois"
        "Leitrim"
        "Limerick"
        "Longford"
        "Louth"
        "Mayo"
        "Meath"
        "Monaghan"
        "North Tipperary"
        "Offaly"
        "Roscommon"
        "Sligo"
        "South Tipperary"
        "Waterford"
        "Westmeath"
        "Wexford"
        "Wicklow"])

(defn fb-as-floats [floatbuffer]
    (loop [i 1 floats []]
      (if (< i (.limit floatbuffer))
        (recur (inc i)
               (conj floats (.get floatbuffer i)))
        floats)))

(r-eval "source(\"src/clad/models/maps.R\")")

(defn data-by-county [county run]
  (first (r-eval
          (str "bycounty('" county "','" run "')"))))

(defn all-counties [run]
  (r-eval (str "populatecounties('" run "')"))
  (println (r-eval "ls(countiesarray)"))
  (map #(str (data-by-county % run) ",")
       counties))
