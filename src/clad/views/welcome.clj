(ns clad.views.welcome
  (:require [clojure.contrib.string :as str])
  (:use [clad.views.site]
        [clad.views.charts]
        [clad.views.svg]
        [clad.models.gdal]
        [clad.models.couch]
        [noir.core :only [defpage]]
        [hiccup.core :only [html]]
	[net.cgrand.enlive-html]
        [incanter.core])
  (:import (java.net URLEncoder
                     URLDecoder)
           (java.io ByteArrayOutputStream
                    ByteArrayInputStream)))

(defn site []
  ;;a little recursion might help here
  (reduce (fn [new-map page]
            (assoc new-map
              (keyword (str/replace-re #"[^a-zA-Z0-9]" "-" (:title page)))
              (assoc-in page [:sections]
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
                                            (assoc-in topic [:subtopics]
                                                      (reduce
                                                       (fn [topic-map subtopic]
                                                         (assoc topic-map
                                                           (keyword
                                                            (str/replace-re #"[^a-zA-Z0-9]" "-" (:title subtopic)))
                                                           subtopic))
                                                       (array-map)
                                                       (reverse (:subtopics topic))))))
                                        (array-map)
                                        (reverse (:topics section))))))
                         (array-map)
                         (reverse (:sections page))))))
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
                        :content (map
                                  #(hash-map
                                    :tag :li :content
                                    (vector
                                     (hash-map
                                      :tag :a :content (:title (val %))
                                      :attrs (hash-map
                                              :href (str "/clad/" page "/section/" (name section)
                                                         "/topic/" (name (key topic)) "/subtopic/"
                                                         (name (key %)))))))
                                  (:subtopics (val topic)))}])))

(defn by-county [folder run]
  {:status 200
   :headers {"Content-Type" "text/csv"
             "Content-Disposition" "attachment;filename=counties.csv"}
   :body (apply str (map
                     (fn [run] (apply str "," (reduce #(str %1 "," %2) counties) "\n" run "," (allcounties-memo run)))
                     (map #(str folder "/" run %) icarus-runs)))})

(deftemplate clad "clad/views/CLAD_1.html"
  [{page :page section :section topic :topic glossary :glossary subtopic :subtopic}]
  
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
  (content (select (format-text topic page section) [[:.Glossary (keyword (str "#" (URLDecoder/decode glossary)))]]))
              
  [:#Content_frame]
  (content (select (format-text topic page section)
                   [(let [sections (get-in (site) [(keyword page) :sections])
                          topics (get (:topics ((keyword section) sections))
                                      (keyword topic)
                                      ((keyword section) sections))
                          target (get-in topics [:subtopics (keyword subtopic)] topics)]
                      (:from target))])))

(defpage "/clad" []
  (clad {:topic "What is Climate Change?" :glossary "climate" :page "Climate Change" :section "Essentials"}))
(defpage "/clad/:page"
  {:keys [page section]}
  (clad {:topic "What is Climate Change?" :glossary "climate" :page page :section section}))
(defpage "/clad/:page/section/:section"
  {:keys [page section]}
  (clad {:topic section :glossary "climate" :page page :section section}))
(defpage [:get ["/clad/:page/section/:section/topic/:more/glossary/:glossary"]]
  {:keys [more glossary page section]}
  (clad {:topic more :glossary glossary :page page :section section}))
(defpage [:get ["/clad/:page/section/:section/topic/:topic/subtopic/:subtopic"]]
  {:keys [topic page section subtopic]}
  (clad {:topic topic :subtopic subtopic :page page :section section :glossary "climate"}))
(defpage [:get ["/clad/:page/section/:section/topic/:more"]]
  {:keys [more page section]} (clad {:topic more :glossary "Climate" :page page :section section}))
(defpage "/csv/:run" {:keys [folder run]} (by-county folder run))
(defpage "/svg/:run/:fill" {:keys [run fill]} (counties-map run fill))
(defpage "/html/:year/:month" {:keys [year month] } (get-run-data year month))

