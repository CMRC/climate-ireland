(ns clad.views.welcome
  (:require [clad.views.common :as common]
            [noir.content.getting-started]
            [clad.models.site :as sitemap])
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
                                 (str "/clad/" page "/glossary/" (:id (:attrs a-selected-node))))))]
  post)) 

(defn make-links [page topic]
  (clone-for [snip (select (format-text "") [(keyword (str "." topic))])]
             [:li]
             (content {:tag :a
                       :attrs {:href (str "/clad/" page "/"
                                          (URLEncoder/encode (apply str (emit* (:id (:attrs snip))))))}
		      :content (apply str (emit* (:alt (:attrs snip))))})))

(deftemplate clad "clad/views/CLAD_1.html"
  [{link :link glossary :glossary page :page}]

  [:#buttons]
  (clone-for [context sitemap/site]
             [:li :a]
             (fn [a-selected-node] 
               (assoc-in a-selected-node [:content]
                         (:title (val context))))
             [:li :a]
             (fn [a-selected-node] 
               (assoc-in a-selected-node [:attrs :href]
                         (str "/clad/" (name (key context))))))

  [:.left_links]
  (clone-for [header (:sections ((keyword page) sitemap/site))]
             [:h3]
             (content (:title header))
             [:li]
             (make-links page (:id header)))

  [:#Glossary]
  (content (select (format-text link) [[:.Glossary (keyword (str "#" (URLDecoder/decode glossary)))]]))
  
  [:#Main_Text]
  (content (select (format-text link) [(keyword (str "#" (URLDecoder/decode link))) :.Main_Text]))
  
  [:#Key_Text]
  (content (select (format-text link) [(keyword (str "#" (URLDecoder/decode link))) :.Key_Text]))
  
  [:#Picture]
  (content (select (format-text link) [(keyword (str "#" (URLDecoder/decode link))) :.Picture])))

(defpage "/clad" [] (clad {:link "whatis" :glossary "climate" :page "climate_change"}))
(defpage "/clad/:page" {:keys [page]} (clad {:link "whatis" :glossary "climate" :page page}))
(defpage [:get ["/clad/:page/:more/glossary/:glossary"]] {:keys [more glossary page]} (clad {:link more :glossary glossary :page page}))
(defpage [:get ["/clad/:page/:more"]] {:keys [more page]} (clad {:link more :glossary "Climate" :page page}))

