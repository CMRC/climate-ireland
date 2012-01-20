(ns clad.models.gdal
  (:import (org.gdal.gdal gdal
                          Dataset)
           (org.gdal.gdalconst gdalconstConstants)
           (java.io ByteArrayOutputStream
                    ByteArrayInputStream)))

(. gdal (AllRegister))

(defn fb-as-floats [floatbuffer]
    (loop [i 1 floats []]
      (if (< i (.limit floatbuffer))
        (recur (inc i)
               (conj floats (.get floatbuffer i)))
        floats)))

(defn adf [filename]
  (let [ds (. gdal Open filename gdalconstConstants/GA_ReadOnly)
        md (.GetMetadata_List ds)
        desc (.GetDescription ds)
        proj (.GetProjection ds)
        band (.GetRasterBand ds 1)
        bb (.ReadRaster_Direct band 0 0 (.GetXSize band) (.GetYSize band))
        fb (.asFloatBuffer bb)
        ba (byte-array (* (.GetXSize band) (.GetYSize band)))]
    (apply str (fb-as-floats fb))))
             
