(ns clad.views.welcome
  (:require [clad.views.common :as common]
            [noir.content.getting-started])
  (:use [noir.core :only [defpage]]
        [hiccup.core :only [html]]
	[net.cgrand.enlive-html])
  (:import (java.net URLEncoder
                     URLDecoder)))
  
(defn format-text []
  (let [pre (html-resource "clad/views/more.html")
       post (select pre [:div])]
  post))

(def more (format-text))

(defn make-links [topic]
  (clone-for [snip (select more [(keyword (str "." topic))])]
             [:li]
             (content {:tag :a
		      :attrs {:href (str "/clad/" (URLEncoder/encode (apply str (emit* (:id (:attrs snip))))))}
		      :content (apply str (emit* (:id (:attrs snip))))})))

(deftemplate clad "clad/views/CLAD_1.html"
  [{link :link glossary :glossary}]

  [(keyword (str "#" "essentials"))]
  (make-links "essentials")

  [(keyword (str "#" "projections"))]
  (make-links "projections")
  
  [(keyword (str "#" "Impacts"))]
  (make-links "Impacts")

  [(keyword (str "#" "howto"))]
  (make-links "howto")
  
  [:#Glossary]
  (content (select more [:.Glossary (keyword (str "#" (URLDecoder/decode glossary)))]))
  
  [:#Main_Text]
  (content (select more [(keyword (str "#" (URLDecoder/decode link))) :.Main_Text]))
  
  [:#Key_Text]
  (content (select more [(keyword (str "#" (URLDecoder/decode link))) :.Key_Text]))
  
  [:#Picture]
  (content (select more [(keyword (str "#" (URLDecoder/decode link))) :.Picture])))

(defpage "/clad" [] (clad {:link "What is Climate?" :glossary "Climate"}))
(defpage [:get ["/clad/:more/glossary/:glossary" :more #".+"]] {:keys [more glossary]} (clad {:link more :glossary glossary}))
(defpage [:get ["/clad/:more" :more #".+"]] {:keys [more]} (clad {:link more :glossary "Climate"}))

