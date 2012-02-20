(ns clad.models.gdal
  (:use incanter.core)
  (:import(java.io ByteArrayOutputStream
                   ByteArrayInputStream)))

(use '(com.evocomputing rincanter))

(def icarus-runs
  ["2020djf"
   "2020mam"
   "2020jja"
   "2020son"
   "2050djf"
   "2050mam"
   "2050jja"
   "2050son"
   "2080djf"
   "2080mam"
   "2080jja"
   "2080son"])

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

(def bycounty-memo (memoize data-by-county))

(defn all-counties [run]
  (r-eval (str "populatecounties('" run "')"))
  (println (r-eval "ls(countiesarray)"))
  (map #(str (bycounty-memo % run) ",") counties))

(def allcounties-memo (memoize all-counties))
