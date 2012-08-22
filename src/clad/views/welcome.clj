(ns clad.views.welcome
  (:require [clojure.contrib.string :as str]
            [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds]))
  (:use [clad.views.site]
        [clad.views.charts]
        [clad.views.svg]
        [clad.models.couch]
        [noir.core :only [defpage pre-route]]
        [noir.response :only [redirect]]
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

(deftemplate table-output
  "clad/views/table.html"
  [year month]
  [:thead :tr]
  (content (map #(hash-map :tag :td :content (str %)) (keys (first (get-run-data year month)))))
  [:tbody :tr]
  (clone-for [data (get-run-data year month)]
             [:tr]
             (content (map #(hash-map :tag :td :content (str %)) (vals data)))))

(defn by-county [year months model scenario variable]
  {:status 200
   :headers {"Content-Type" "text/csv"
             "Content-Disposition" "attachment;filename=counties.csv"}
   :body (str (apply str (interpose "," counties)) "\n"
              (apply str (all-counties year months model scenario variable)))})

(defsnippet content-nodes "clad/views/CLAD_1.html" [:html]
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
  (content (select (format-text topic page section)
                   [[:.Glossary (keyword (str "#" (URLDecoder/decode glossary)))]]))

  [:#References :ul]
  (clone-for [ref
              (let [sections (get-in (site) [(keyword page) :sections])
                    topics (get (:topics ((keyword section) sections))
                                (keyword topic)
                                ((keyword section) sections))
                    target (get-in topics [:subtopics (keyword subtopic)])]
                (:refs target))]
             [:li :a]
             (fn [a-selected-node]
               (->
                (assoc-in a-selected-node [:content]
                          (:title (ref references)))
                (assoc-in [:attrs :href]
                          (str "/clad/Resources/section/References/" (name ref))))))
  
  [:#Content_frame]
  (content (select (format-text topic page section)
                   [(let [sections (get-in (site) [(keyword page) :sections])
                          topics (get (:topics ((keyword section) sections))
                                      (keyword topic)
                                      ((keyword section) sections))
                          target (get-in topics [:subtopics (keyword subtopic)] topics)]
                      (:from target))])))
(defn clad
  [& args]
  (emit* (content-nodes args)))
  
(defn make-refs [ref]
   (->
    (content-nodes {:topic "References" :glossary "climate" :page "Resources" :section "References"})
    (transform
     [:#refs :.Title]
     #(assoc-in % [:content] (:title ((keyword ref) references))))
    (transform
     [:#refs :.Authors]
     #(assoc-in % [:content] (apply str (interpose \, (:authors ((keyword ref) references))))))
    (transform
     [:#refs :.Published :a]
     #(assoc-in % [:content] (:published ((keyword ref) references))))
    (transform
     [:#refs :.Published :a]
     (set-attr :href (:link ((keyword ref) references))))
    emit*))

(defn all-refs []
   (->
    (content-nodes {:topic "References" :glossary "climate" :page "Resources" :section "References"})
    (transform
     [:.Cite]
     (clone-for
      [cite (vals references)]
      [:.Title]
      (content (:title cite))
      [:.Authors]
      (content (apply str (interpose \, (:authors cite))))
      [:.Published :a]
      (content (:published cite))
      [:.Published :a]
      (set-attr :href (:link cite))))
    emit*))

(deftemplate one-pane "clad/views/View_3.html"
  [text tab]
  [:#content]
  (content (select (html-resource text)[(keyword (str "#" tab))]))
  [:#tabs]
  (content (select (html-resource text)[:.buttons])))

(deftemplate two-pane "clad/views/welcome.html"
  [text img]
  [:#blurb]
  (content (html-resource text))
  [:#map]
  (content {:tag :img :attrs {:src img}})
  [:#banner]
  (substitute (select (html-resource "clad/views/View_3.html") [:#banner]))
  [:#footer]
  (substitute (select (html-resource "clad/views/View_3.html") [:#footer])))

(deftemplate welcome "clad/views/welcome.html"
  [map]
  [:#map]
  (content map)
  [:#banner]
  (substitute (select (html-resource "clad/views/View_3.html") [:#banner]))
  [:#footer]
  (substitute (select (html-resource "clad/views/View_3.html") [:#footer])))

(deftemplate svgmap "clad/views/View_2.html"
  [map blurb]
  [:#view-2-map]
  (content map)
  [:#view-2-2-chart]
  (content blurb)
  [:#banner]
  (substitute (select (html-resource "clad/views/View_3.html") [:#banner]))
  [:#footer]
  (substitute (select (html-resource "clad/views/View_3.html") [:#footer])))

(deftemplate login "clad/views/Login.html" [])

(defpage "/ci/welcome/compare/:year1/:year2/:months/:variable"
  {:keys [year1 year2 months variable]}
  (welcome {:tag :img
            :attrs {:src (str "/ci/svg/compare/" year1 "/" year2 "/" months "/" variable)
                    :height "100%"}}))
(defpage "/ci/welcome/plot/:region/:months/:variable/decadal"
  {:keys [region months variable]}
  (welcome {:tag :img
            :attrs {:src (str "/ci/plot/" region "/" months "/" variable "/decadal")
                    :height "100%"}}))
(defpage "/ci/welcome/plot/:region/:months/:variable/decadal-box"
  {:keys [region months variable]}
  (welcome {:tag :img
            :attrs {:src (str "/ci/plot/" region "/" months "/" variable "/decadal-box")}}))

(defpage "/ci/welcome/plot/:region/:months/:variable"
  {:keys [region months variable]}
  (welcome {:tag :img
            :attrs {:src (str "/ci/plot/" region "/" months "/" variable)
                    :height "100%"}}))

(defpage "/ci/welcome/svg/:year/:months/:model/:scenario/:variable/:shading"
  {:keys [year months model scenario variable shading]}
  (welcome {:tag :object
            :attrs {:data (str "/ci/svg/" year "/" months "/" model "/"
                              scenario "/" variable "/" shading)
                    :type "image/svg+xml"
                    :height "550px"
                    :width "440px"}}))

(defpage "/ci/welcome/svgbar/:county/:year/:months/:model/:scenario/:variable/:shading"
  {:keys [county year months model scenario variable shading]}
  (svgmap {:tag :embed
           :attrs {:src (str "/ci/svg/" year "/" months "/" model "/"
                             scenario "/" variable "/" shading)
                   :type "image/svg+xml"}}
          {:tag :img
           :attrs {:src (str "/ci/bar/" county "/" year
                             "/" months "/" variable)
                   :max-width "100%"}}))

(defpage "/ci/welcome/png/:year/:months/:model/:scenario/:variable/:shading"
  {:keys [year months model scenario variable shading]}
  (welcome {:tag :img
            :attrs {:src (str "/ci/png/" year "/" months "/" model "/"
	    scenario "/" variable "/" shading)
                    :height "100%"}}))

(defpage "/ci/welcome/bar/:year/:county/:months/:variable"
  {:keys [year county months variable]}
  (welcome {:tag :img
            :attrs {:src (str "/ci/bar/" year "/" county
                              "/" months "/" variable)
                    :height "100%"}}))

(defpage "/" []
  (redirect "/ci/about"))

(defpage "/ci" []
  (redirect "/ci/about"))

(defpage "/ci/about" []
  (two-pane "clad/views/CI_About.html" "/img/Provinces_2.png"))

(defpage "/ci/climate-change/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_ClimateChange.html" tab))

(defpage "/ci/adaptation/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_adaptation.html" tab))

(defpage "/ci/sectors/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_sectors.html" tab))

(defpage "/ci/resources/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_Resources.html" tab))

(defpage "/clad/Resources/section/References/:ref"
  {:keys [ref]}
  (make-refs ref))
(defpage "/clad" []
  (clad :topic "What is Climate Change?" :glossary "climate" :page "Climate Change" :section "Essentials"))
(defpage "/clad/:page"
  {:keys [page section]}
  (clad {:topic "What is Climate Change?" :glossary "climate" :page page :section section}))
(defpage "/clad/:page/section/:section"
  {:keys [page section]}
  (if (= section "References")
    (all-refs)
    (clad :topic section :glossary "climate" :page page :section section)))
(defpage [:get ["/clad/:page/section/:section/topic/:more/glossary/:glossary"]]
  {:keys [more glossary page section]}
  (clad {:topic more :glossary glossary :page page :section section}))
(defpage [:get ["/clad/:page/section/:section/topic/:topic/subtopic/:subtopic"]]
  {:keys [topic page section subtopic]}
  (clad :topic topic :subtopic subtopic :page page :section section :glossary "climate"))
(defpage [:get ["/clad/:page/section/:section/topic/:more"]]
  {:keys [more page section]}
  (clad :topic more :glossary "Climate" :page page :section section))
(defpage "/ci/csv/:year/:months/:model/:scenario/:variable"
  {:keys [year months model scenario variable]}
  (by-county (Integer/parseInt year) months  model scenario variable))
(defpage "/ci/svg/:year/:months/:model/:scenario/:variable/:fill"
  {:keys [year months model scenario variable fill]}
  (provinces-map (Integer/parseInt year) months model scenario variable fill))
(defpage "/ci/png/:year/:months/:model/:scenario/:variable/:fill"
  {:keys [year months model scenario variable fill]}
  (counties-map-png (Integer/parseInt year) months model scenario variable fill))
(defpage "/ci/html/:year/:months" {:keys [year months] } (table-output year months))
(defpage "/ci/plot/:county/:months/:variable" {:keys [county months variable]} 
	 (plot-models county months variable))
(defpage "/ci/plot/:county/:months/:variable/decadal" {:keys [county months variable]} 
  (plot-models-decadal county months variable))
(defpage "/ci/plot/:county/:months/:variable/decadal-bar" {:keys [county months variable]} 
  (decadal-bar county months variable))
(defpage "/ci/plot/:county/:months/:variable/decadal-box" {:keys [county months variable]} 
  (decadal-box county months variable))
(defpage "/ci/bar/:county/:year/:months/:variable" {:keys [county year months variable]}
  (barchart county (Integer/parseInt year) months variable))

(defpage "/login" []
  (two-pane "clad/views/Login.html" ""))

(pre-route "/ci/*" {:as req}
           (friend/authenticated 
                                        ; We don't need to do anything, we just want to make sure we're 
                                        ; authenticated. 
            nil))
