(ns clad.views.welcome
  (:require [clad.views.common :as common]
            [noir.content.getting-started]
            clad.models.site)
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

  [:.left_links]
  (clone-for [header [{:id "essentials" :title "Essentials"}
                      {:id "projections" :title "Global Projections"}
                      {:id "Impacts" :title "Irish Coasts"}]]
             [:h3]
             (content (:title header))
             [:li]
             (make-links (:id header)))

  [:#Glossary]
  (content (select (format-text link) [[:.Glossary (keyword (str "#" (URLDecoder/decode glossary)))]]))
  
  [:#Main_Text]
  (content (select (format-text link) [(keyword (str "#" (URLDecoder/decode link))) :.Main_Text]))
  
  [:#Key_Text]
  (content (select (format-text link) [(keyword (str "#" (URLDecoder/decode link))) :.Key_Text]))
  
  [:#Picture]
  (content (select (format-text link) [(keyword (str "#" (URLDecoder/decode link))) :.Picture])))

(defpage "/clad" [] (clad {:link "whatis" :glossary "climate"}))
(defpage [:get ["/clad/:more/glossary/:glossary"]] {:keys [more glossary]} (clad {:link more :glossary glossary}))
(defpage [:get ["/clad/:more"]] {:keys [more]} (clad {:link more :glossary "Climate"}))

