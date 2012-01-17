(ns clad.views.welcome
  (:require [clojure.contrib.string :as str])
  (:use [clad.views.site]
        [noir.core :only [defpage]]
        [hiccup.core :only [html]]
	[net.cgrand.enlive-html])
  (:import (java.net URLEncoder
                     URLDecoder)))

(defn site []
  ;;a little recursion might help here
  (reduce (fn [new-map v]
            (assoc new-map
              (keyword (str/replace-re #"[^a-zA-Z0-9]" "-" (:title v)))
              (assoc-in v [:sections]
                        (reduce
                         (fn [inner-map section]
                           (assoc inner-map
                             (keyword
                              (str/replace-re #"[^a-zA-Z0-9]" "-" (:title section)))
                             (assoc-in section [:topics]
                                       (reduce
                                        (fn [section-map topic]
                                          (assoc section-map
                                            (keyword
                                             (str/replace-re #"[^a-zA-Z0-9]" "-" (:title topic)))
                                            topic))
                                        (array-map)
                                        (reverse (:topics section))))))
                         (array-map)
                         (reverse (:sections v))))))
          (array-map)
          (reverse sitemap)))

(defn format-text [topic page section]
  (let [file (str "clad/views/" (:file ((keyword page) (site))))
        pre (html-resource file)
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
             (content [{:tag :a
                       :attrs {:href (str "/clad/" page "/section/" (name section) "/topic/"
                                          (name (key topic)))}
                        :content (:title (val topic))}
                       {:tag :ul
                        :content (map #(hash-map :tag :li :content
                                                 (vector (hash-map :tag :a :content (:title %)
                                                                   :attrs (hash-map :href (:title %)))))
                                      (:subtopics (val topic)))}])))

(deftemplate clad "clad/views/CLAD_1.html"
  [{link :link glossary :glossary page :page section :section}]
  
  [:#buttons :li]
  (clone-for [context (site)]
             [:a]
             (fn [a-selected-node] 
               (assoc-in a-selected-node [:content]
                         (:title (val context))))
             [:a]
             (fn [a-selected-node] 
               (assoc-in a-selected-node [:attrs :href]
                         (let [level1 (key context)
                               level2 (first (:sections (level1 (site))))
                               level3 (get (val level2) :topics nil)]
                           (str "/clad/" (name level1)
                                "/section/"
                                (name (key level2))
                                (if (seq level3)
                                  (str "/topic/" (name (key (first level3))))))))))
  
  [:.left_links]
  (clone-for [section (:sections ((keyword page) (site)))]
             [:a]
             (fn [a-selected-node] 
               (assoc-in
                (assoc-in a-selected-node
                          [:attrs :href]
                          (str "/clad/" page "/section/" (name (key section))))
                [:content]
                (:title (val section))))
             [:li]
             (make-links page (key section) (:topics (val section))))
  
  [:#Glossary]
  (content (select (format-text link page section) [[:.Glossary (keyword (str "#" (URLDecoder/decode glossary)))]]))
              
  [:#Content_frame]
  (content (select (format-text link page section)
                   [(let [sections (get-in (site) [(keyword page) :sections])
                          headings (get (:topics ((keyword section) sections))
                                      (keyword link)
                                      ((keyword section) sections))
                          target (get-in headings [:subtopics] headings)]
                      (:from target))])))

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

