(ns clad.views.welcome
  (:require [clojure.contrib.string :as str]
            [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [noir.session :as session]
            [noir.response :as resp]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds]))
  (:use [clad.views.site]
        [clad.views.charts]
        [clad.views.svg]
        [clad.models.couch]
        [noir.core :only [defpage pre-route]]
        [noir.request :only [ring-request]]
        [hiccup.core :only [html]]
	[net.cgrand.enlive-html]
        [incanter.core])
  (:import (java.net URLEncoder
                     URLDecoder)
           (java.io ByteArrayOutputStream
                    ByteArrayInputStream)))

(defn good-browser? []
  (->
   (->>
   (get (:headers (ring-request)) "user-agent")
   (re-matches #".*MSIE (\d)\..*")
   second)
   (or "100")
   Integer/parseInt
   (> 8)))

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
  [text page tab]
  [:#content]
  (content (select (html-resource text)[(keyword (str "#" tab))]))
  [:#tabs]
  (content (select (transform (html-resource text)
                              [:.buttons :a]
                              (fn [a-node]
                                (if (->>
                                     (get-in a-node [:attrs :href])
                                     (str/split #"/")
                                     (some #{tab}))
                                  (assoc-in a-node [:attrs :id]
                                            "current")
                                  a-node)))
                   [:.buttons]))

  [:#buttons]
  (content (select (transform (html-resource "clad/views/View_3.html")
                              [:.buttons :a]
                              (fn [a-node]
                                (if (->>
                                     (get-in a-node [:attrs :href])
                                     (str/split #"/")
                                     (some #{page}))
                                  (assoc-in a-node [:attrs :id]
                                            "current")
                                  a-node)))
                   [:.buttons :ul])))

(deftemplate two-pane "clad/views/welcome.html"
  [text page img]
  [:#blurb]
  (content (html-resource text))
  [:#map]
  (content {:tag :object :attrs {:data img}})
  [:#banner]
  (substitute (select (html-resource "clad/views/View_3.html") [:#banner]))
  [:#footer]
  (substitute (select (html-resource "clad/views/View_3.html") [:#footer]))
  [:#buttons]
  (content (select (transform (html-resource "clad/views/View_3.html")
                              [:.buttons :a]
                              (fn [a-node]
                                (if (->>
                                     (get-in a-node [:attrs :href])
                                     (str/split #"/")
                                     (some #{page}))
                                  (assoc-in a-node [:attrs :id]
                                            "current")
                                  a-node)))
                   [:.buttons :ul])))


(def seasons {"DJF" "Winter", "MAM" "Spring", "JJA" "Summer", "SON" "Autumn" "J2D" "All Seasons"})
(def scenarios {["CGCM31" "A1B"] "A1B"
                ["CGCM31" "A2"] "A2"
                ["HadGEM" "RCP45"] "RCP45"
                ["HadGEM" "RCP85"] "RCP85"
                ["ICARUS" "ICARUS"] "A2 + B2 ensemble"
                ["ensemble" "ensemble"] "ensemble"})
                
(defsnippet map-help "clad/views/chart-help.html"
  [:#map-help]
  
  []
  )
  
(defsnippet chart-help
  "clad/views/chart-help.html"
  [:#view-2-2-expl]

  [{:keys [months variable]}]
  
  [:.season]
  (content (seasons months))

  [:.variable]
  (content (variables variable))

  [:.if-temperature]
  (set-attr :class (when (not= variable "T_2M") "do-not-display"))
  
  [:.if-precipitation]
  (set-attr :class (when (not= variable "TOT_PREC") "do-not-display")))

(defsnippet maptools "clad/views/maptools.html" [:#view-2-map-tool]
  [req map & {:keys [counties?] :or {counties? false}}]
  
  [:#decades :option]
  (clone-for [decade ["2021-30" "2031-40" "2041-50" "2051-60"]]
             [:option]
             (fn [a-node] (->
                           (assoc-in (if (= (:years req) decade)
                                       (assoc-in a-node [:attrs :selected] nil)
                                       a-node) [:content] decade)
                           (assoc-in [:attrs :value] decade))))
  
  [:#regions :option]
  (clone-for [region ["Counties" "Provinces"]]
             [:option]
             (fn [a-node] (->
                           (assoc-in (if (or
                                          (and counties?
                                               (= region "Counties"))
                                          (and (not counties?)
                                               (= region "Provinces")))
                                       (assoc-in a-node [:attrs :selected] nil)
                                       a-node) [:content] region)
                           (assoc-in [:attrs :value] region))))

  [:#region]
  (set-attr :value (:region req))
  
  [:#variables :option]
  (clone-for [variable (keys variables)]
             [:option]
             (fn [a-node] (->
                           (assoc-in
                            (if (= variable (:variable req))
                              (assoc-in a-node [:attrs :selected] nil)
                              a-node)
                            [:content] (variables variable))
                           (assoc-in [:attrs :value] variable))))
  
  [:#months :option]
  (clone-for [month (keys seasons)]
             [:option]
             (fn [a-node] (->
                           (assoc-in (if (= month (:months req))
                                       (assoc-in a-node [:attrs :selected] nil)
                                       a-node) [:content] (seasons month))
                           (assoc-in [:attrs :value] month))))

  [:#runs :option]
  (clone-for [run (conj ensemble ["ICARUS" "ICARUS"] ["ensemble" "ensemble"])]
             [:option]
             (fn [a-node] (->
                           (assoc-in (if (and (= (:model req) (first run))
                                              (= (:scenario req) (second run)))
                                       (assoc-in a-node [:attrs :selected] nil)
                                       a-node) [:content] (scenarios run))
                           (assoc-in [:attrs :value] (str (first run)
                                                          "/" (second run)))))))

(deftemplate current-climate "clad/views/View_3.html"
  [req]
  [:#main]
  (content (html-resource "clad/views/CI_Information.html")))

(deftemplate svgmap "clad/views/View_2.html"
  [req map blurb & {:keys [counties?] :or {counties? false}}]
  [:#banner]
  (substitute (select (html-resource "clad/views/View_3.html") [:#banner]))
  [:#tabs]
  (content (select (transform (html-resource "clad/views/CI_Information.html")
                              [:li :a]
                              (fn [a-node]
                                (if (->>
                                     (get-in a-node [:attrs :href])
                                     (str/split #"/")
                                     (some #{"projections"}))
                                  (assoc-in a-node [:attrs :id]
                                            "current")
                                  a-node)))
                   [:.buttons]))
  [:.buttons :a]
  (fn [a-node]
    (if (->>
         (get-in a-node [:attrs :href])
         (str/split #"/")
         (some #{"projections"}))
      (assoc-in a-node [:attrs :id]
                "current")
      a-node))
  [:#info-header]
  (content (select (html-resource "clad/views/CI_Information.html") [:#climate-projections]))
  [:#view-2-map]
  (content map)
  [:#view-2-map-tool]
  (content (maptools req map :counties? counties?))
  [:#view-2-2-chart]
  (content blurb)
  [:#view-2-2-expl]
  (content (chart-help req))
  [:#map-help]
  (content (map-help))
  [:#footer]
  (substitute (select (html-resource "clad/views/View_3.html") [:#footer])))

(deftemplate login "clad/views/Login.html" [])

(defpage "/ci/climate-information/projections/:region/:years/:months/:model/:scenario/:variable/:shading"
 {:as req}
 (svgmap req
         {:tag :object
          :attrs {(if (good-browser?) :data :src) (make-url "svg" req)
                  :type "image/svg+xml"}}
         {:tag :img
          :attrs {:src (make-url "box" req)
                  :max-width "100%"}}))

(defpage "/ci/climate-information/projections/:region/:years/:months/:model/:scenario/:variable/:shading/counties"
 {:as req}
 (svgmap req
         {:tag :object
          :attrs {(if (good-browser?) :data :src) (make-url "svg" req :counties? true)
                  :type "image/svg+xml"}}
         {:tag :img
          :attrs {:src (make-url "box" req)
                  :max-width "100%"}}
         :counties? true))

(defpage "/" []
  (resp/redirect "/ci/about"))

(defpage "/ci" []
  (resp/redirect "/ci/about"))

(defpage "/ci/about" []
  (two-pane "clad/views/CI_About.html" "about" "/img/impact-tool.svg"))

(defpage "/ci/climate-change/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_ClimateChange.html" "climate-change" tab))

(defpage "/ci/adaptation/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_adaptation.html" "adaptation" tab))

(defpage "/ci/sectors/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_sectors.html" "sectors" tab))

(defpage "/ci/resources/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_Resources.html" "resources" tab))

(defpage "/ci/tools/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_tools.html" "tools" tab))

(defpage "/ci/climate-information/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_Information.html" "climate-information" tab))

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
  (by-county year months  model scenario variable))
(defpage "/ci/svg/:region/:year/:months/:model/:scenario/:variable/:fill"
  {:as req}
  (provinces-map req))
(defpage "/ci/svg/:region/:year/:months/:model/:scenario/:variable/:fill/counties"
  {:as req}
  (counties-map req))
(defpage "/ci/png/:year/:months/:model/:scenario/:variable/:fill"
  {:keys [year months model scenario variable fill]}
  (counties-map-png year months model scenario variable fill))
(defpage "/ci/html/:year/:months" {:keys [year months] } (table-output year months))
(defpage "/ci/plot/:county/:months/:variable" {:keys [county months variable]} 
	 (plot-models county months variable))
(defpage "/ci/plot/:county/:months/:variable/decadal" {:keys [county months variable]} 
  (plot-models-decadal county months variable))
(defpage "/ci/box/:county/:years/:months/:model/:scenario/:variable/linear" {:keys [county months variable]} 
  (decadal-box county months variable))
(defpage "/ci/bar/:region/:year/:months/:model/:scenario/:variable/:fill" {:keys [region year months variable]}
  (barchart region year months variable))
(defpage "/login" []
  (two-pane "clad/views/Login.html" "login" ""))
(defpage "/ci/maptools" {:as req}
  (resp/redirect (str "/ci/climate-information/projections/" (:region req)
                 "/" (:years req)
                 "/" (:months req)
                 "/" (:runs req)
                 "/" (:variable req)
                 "/linear"
                 (when (= (:regions req) "Counties") "/counties"))))

(defn clear-identity [response] 
  (update-in response [:session] dissoc ::identity))

(defpage "/logout" []
  (clear-identity (resp/redirect "/ci/about")) )

(pre-route "/ci/*" {:as req}
           (friend/authenticated 
                                        ; We don't need to do anything, we just want to make sure we're 
                                        ; authenticated. 
            (log/info "User: " (get-in req [:session :cemerick.friend/identity
                                            :current])
                      " logged in from: " (req :remote-addr)
                      " to URI: " (req :uri))))