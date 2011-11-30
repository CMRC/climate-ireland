(ns clad.views.welcome
  (:require [clad.views.common :as common]
            [noir.content.getting-started])
  (:use [noir.core :only [defpage]]
        [hiccup.core :only [html]]
	[net.cgrand.enlive-html])
  (:import (java.net URLEncoder
                     URLDecoder)))
  
(def more (html-resource "clad/views/more.html"))

(deftemplate clad "clad/views/CLAD_1.html"
  [link]

  [(keyword (str "#" "essentials"))]
  (clone-for [snip (select more [(keyword (str "." "essentials"))])]
             [:li]
             (content {:tag :a
                       :attrs {:href (str "/clad/" (URLEncoder/encode (apply str (emit* (:id (:attrs snip))))))}
                       :content (apply str (emit* (:id (:attrs snip))))}))
  
  [:.CF2]
  (content (select more [(keyword (str "#" (URLDecoder/decode link)))])))

(defpage "/clad" [] (clad "What is Climate?"))
(defpage [:get ["/clad/:more" :more #".+"]] {:keys [more]} (clad more))


["essentials" "projections" "Impacts" "How to deal with it"]
