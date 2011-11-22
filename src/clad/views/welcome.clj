(ns clad.views.welcome
  (:require [clad.views.common :as common]
            [noir.content.getting-started])
  (:use [noir.core :only [defpage]]
        [hiccup.core :only [html]]
	[net.cgrand.enlive-html])
  (:import (java.net URLEncoder
                     URLDecoder)))
  
(def essential (html-resource "clad/views/more.html"))

(deftemplate clad "clad/views/CLAD_1.html"
  [file]

  [:#essential]
  (clone-for [snip (select essential [:.more])]
   [:li]
   (content {:tag :a
             :attrs {:href (URLEncoder/encode (str "/clad/" (apply str (emit* (:id (:attrs snip))))))}
             :content (apply str (emit* (:id (:attrs snip))))}))

  [:.CF2]
  (content (select essential [(keyword (str "#" file))])))

(defpage "/clad" [] (clad "whatis"))
(defpage [:get ["/clad/:more" :more #".+"]] {:keys [more]} (clad more))
