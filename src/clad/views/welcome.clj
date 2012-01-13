(ns clad.views.welcome
  (:require [clad.models.site :as sitemap])
  (:use [noir.core :only [defpage]]
        [hiccup.core :only [html]]
	[net.cgrand.enlive-html])
  (:import (java.net URLEncoder
                     URLDecoder)))

(defn format-text [topic page section]
  (let [pre (html-resource "clad/views/more.html")
        post (transform pre [:a.Glossary] 
                        (fn [a-selected-node]
                          (assoc-in a-selected-node [:attrs :href] 
                                    (str "/clad/" page "/section/" (name section)
                                         "/topic/" topic "/glossary/"
                                         (:id (:attrs a-selected-node))))))]
    post)) 

(defn make-links [page section topics]
  (clone-for [topic topics]
             [:li]
             (content {:tag :a
                       :attrs {:href (str "/clad/" page "/section/" (name section) "/topic/"
                                          (apply str (emit* (name (key topic)))))}
                       :content (apply str (emit* (:title (val topic))))})))

(deftemplate clad "clad/views/CLAD_1.html"
  [{link :link glossary :glossary page :page section :section}]
  
  [:#buttons :li]
  (clone-for [context (sitemap/site)]
             [:a]
             (fn [a-selected-node] 
               (assoc-in a-selected-node [:content]
                         (:title (val context))))
             [:a]
             (fn [a-selected-node] 
               (assoc-in a-selected-node [:attrs :href]
                         (let [level1 (key context)
                               level2 (first (:sections (level1 (sitemap/site))))]
                           (str "/clad/" (name level1)
                                "/section/"
                                (name (key level2))
                                "/topic/"
                                (name (key (first (:headings (val level2))))))))))
  
  [:.left_links]
  (clone-for [section (:sections ((keyword page) (sitemap/site)))]
             [:a]
             (fn [a-selected-node] 
               (assoc-in
                (assoc-in a-selected-node
                          [:attrs :href]
                          (str "/clad/" page "/section/" (name (key section))))
                [:content]
                (:title (val section))))
             [:li]
             (make-links page (key section) (:headings (val section))))

  [:#Glossary]
  (content (select (format-text link page section) [[:.Glossary (keyword (str "#" (URLDecoder/decode glossary)))]]))
  
  [:#Main_Text]
  (content (select (format-text link page section)
                   [(let [sections (:sections ((keyword page) (sitemap/site)))
                          target (get (:headings ((keyword section) sections))
                                      (keyword link)
                                      ((keyword section) sections))]
                      (:from target))
                    :.Main_Text]))
  
  [:#Key_Text]
  (content (select (format-text link page section) [(keyword (str "#" (URLDecoder/decode link))) :.Key_Text]))
  
  [:#Picture]
  (content (select (format-text link page section) [(keyword (str "#" (URLDecoder/decode link))) :.Picture])))

(defpage "/clad" []
  (clad {:link "What is Climate Change?" :glossary "climate" :page "Climate Change" :section "Essentials"}))
(defpage "/clad/:page"
  {:keys [page section]}
  (clad {:link "What is Climate Change?" :glossary "climate" :page page :section section}))
(defpage "/clad/:page/section/:section"
  {:keys [page section]}
  (clad {:link section :glossary "climate" :page page :section section}))
(defpage [:get ["/clad/:page/section/:section/topic/:more/glossary/:glossary"]]
  {:keys [more glossary page section]}
  (clad {:link more :glossary glossary :page page :section section}))
(defpage [:get ["/clad/:page/section/:section/topic/:more"]]
  {:keys [more page section]} (clad {:link more :glossary "Climate" :page page :section section}))
