(ns clad.views.welcome
  (:require [clad.views.common :as common]
            [noir.content.getting-started])
  (:use [noir.core :only [defpage]]
        [hiccup.core :only [html]]
	[net.cgrand.enlive-html])
  (:import (java.net URLEncoder
                     URLDecoder)))
  
(defn format-text [page]
  (let [pre (html-resource "clad/views/more.html")
       post (transform pre [:a.Glossary] 
                           (fn [a-selected-node]
                               (assoc-in a-selected-node [:attrs :href] 
                                 (str page "glossary/" (:id (:attrs a-selected-node))))))]
  post)) 

(defn make-links [topic]
  (clone-for [snip (select (format-text "") [(keyword (str "." topic))])]
             [:li]
             (content {:tag :a
		      :attrs {:href (str "/clad/" (URLEncoder/encode (apply str (emit* (:id (:attrs snip))))))}
		      :content (apply str (emit* (:alt (:attrs snip))))})))

(deftemplate clad "clad/views/CLAD_1.html"
  [{link :link glossary :glossary}]

  (clone-for [header ["one" "two" "three"]]
             [:.left_links :h3]
             header)
             
  [(keyword (str "#" "essentials"))]
  (make-links "essentials")

  [(keyword (str "#" "projections"))]
  (make-links "projections")
  
  [(keyword (str "#" "Impacts"))]
  (make-links "Impacts")

  [(keyword (str "#" "howto"))]
  (make-links "howto")
  
  [:#Glossary]
  (content (select (format-text link) [[:.Glossary (keyword (str "#" (URLDecoder/decode glossary)))]]))
  
  [:#Main_Text]
  (content (select (format-text link) [(keyword (str "#" (URLDecoder/decode link))) :.Main_Text]))
  
  [:#Key_Text]
  (content (select (format-text link) [(keyword (str "#" (URLDecoder/decode link))) :.Key_Text]))
  
  [:#Picture]
  (content (select (format-text link) [(keyword (str "#" (URLDecoder/decode link))) :.Picture])))

(defpage "/clad" [] (clad {:link "What is Climate?" :glossary "climate"}))
(defpage [:get ["/clad/:more/glossary/:glossary"]] {:keys [more glossary]} (clad {:link more :glossary glossary}))
(defpage [:get ["/clad/:more"]] {:keys [more]} (clad {:link more :glossary "Climate"}))

