(ns clad.views.welcome
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [noir.session :as session]
            [noir.response :as resp]
            [clj-time.core :as time]
            [clj-time.format :as format]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds]))
  (:use [clad.views.site]
        [clad.views.charts]
        [clad.views.svg]
        [clad.views.questions]
        [clad.models.couch]
        [noir.core :only [defpage pre-route]]
        [noir.request :only [ring-request]]
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


(defsnippet question "clad/views/Questionnaire.html"
  [:.question]
  [[title {:keys [question responses freetext]}]]
  [:h4] (content question)
  [:.form] (set-attr :id title)
  [:a] (set-attr :href (str "javascript: document.getElementById('" title "').style.display='none';"))
  [:li.select] (clone-for [response responses]
                   (content {:tag :input
                             :content response
                             :attrs
                             {:value response
                              :name title
                              :type "radio"}}))
  [:li.free] (when freetext
               (content [freetext {:tag :input :attrs {:type "text" :name (str title freetext)}}])))

(defsnippet qform "clad/views/Questionnaire.html"
  [:#survey-info]
  []
  [:#questions]
  (content (map #(question %) qs)))

(deftemplate questionnaire "clad/views/View_3.html"
  [params]
  [:body] (set-attr :onload "qinit();")
  [:#content]
  (content (qform)))

(deftemplate submit "clad/views/View_3.html"
  [req]
  [:#content]
  (content (html [:div
                  [:p "Thank you"]
                  [:p [:a {:href "/"} "Home.."]]])))

(deftemplate one-pane "clad/views/View_3.html"
  [text page tab]
  [:#content]
  (content (select (html-resource text)[(keyword (str "#" tab))]))
  [:#tabs]
  (content (select (transform (html-resource text)
                              [:.buttons :a]
                              (fn [a-node]
                                (if (->>
                                     (str/split (get-in a-node [:attrs :href]) #"/")
                                     (some #{tab}))
                                  (assoc-in a-node [:attrs :id]
                                            "current")
                                  a-node)))
                   [:.buttons]))

  [:#menu]
  (content (select (transform (html-resource "clad/views/View_3.html")
                              [:.buttons :a]
                              (fn [a-node]
                                (if (->>
                                     (str/split (get-in a-node [:attrs :href]) #"/")
                                     (some #{page}))
                                  (assoc-in a-node [:attrs :id]
                                            "current")
                                  a-node)))
                   [:.buttons :ul])))

(deftemplate two-pane "clad/views/welcome.html"
  [text page rhs]
  [:#blurb]
  (content (html-resource text))
  [:#map]
  (content rhs)
  [:#banner]
  (substitute (select (html-resource "clad/views/View_3.html") [:#banner]))
  [:#footer]
  (substitute (select (html-resource "clad/views/View_3.html") [:#footer]))
  [:#menu]
  (content (select (transform (html-resource "clad/views/View_3.html")
                              [:.buttons :a]
                              (fn [a-node]
                                (if (->>
                                     (str/split (get-in a-node [:attrs :href]) #"/")
                                     (some #{page}))
                                  (assoc-in a-node [:attrs :id]
                                            "current")
                                  a-node)))
                   [:.buttons :ul])))


(def seasons {"DJF" "Winter", "MAM" "Spring", "JJA" "Summer", "SON" "Autumn" "J2D" "Annual"})

(def scenarios (array-map
                ["ensemble" "high"] "High"
                ["ensemble" "medium"] "Medium"
                ["ensemble" "low"] "Low"
                ["ensemble" "ensemble"] "ensemble"))
                
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
  [req map]
  
  [:#decades :option]
  (clone-for [decade {"2030s" "2021-2050"
                      "2040s" "2031-2060"
                      "2050s" "2041-2070"
                      "2060s" "2051-2080"
                      "2070s" "2061-2090"
                      "2080s" "2071-2100"}]
             [:option]
             (fn [a-node] (->
                           (assoc-in (if (= (:years req) (second decade))
                                       (assoc-in a-node [:attrs :selected] nil)
                                       a-node) [:content] (first decade))
                           (assoc-in [:attrs :value] (second decade)))))
  
  [:#regions :option]
  (clone-for [region ["Counties" "Provinces"]]
             [:option]
             (fn [a-node] (->
                           (assoc-in (if (= region (:regions req))
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
  (clone-for [run (keys scenarios)]
             [:option]
             (fn [a-node] (->
                           (assoc-in (if (and (= (:model req) (first run))
                                              (= (:scenario req) (second run)))
                                       (assoc-in a-node [:attrs :selected] nil)
                                       a-node) [:content] (scenarios run))
                           (assoc-in [:attrs :value] (str (first run)
                                                          "/" (second run))))))
               
  [:#abs :option]
  (clone-for [abs ["Change" "Value"]]
             [:option]
             (fn [a-node] (->
                           (assoc-in (if (= (:abs req) abs)
                                       (assoc-in a-node [:attrs :selected] nil)
                                       a-node) [:content] abs)
                           (assoc-in [:attrs :value] abs)))))

(deftemplate current-climate "clad/views/View_3.html"
  [req]
  [:#main]
  (content (html-resource "clad/views/CI_Information.html")))

(deftemplate svgmap "clad/views/View_2.html"
  [req map blurb]
  [:#banner]
  (substitute (select (html-resource "clad/views/View_3.html") [:#banner]))
  [:#tabs]
  (content (select (transform (html-resource "clad/views/CI_Information.html")
                              [:li :a]
                              (fn [a-node]
                                (if (->>
                                     (str/split (get-in a-node [:attrs :href]) #"/")
                                     (some #{"projections"}))
                                  (assoc-in a-node [:attrs :id]
                                            "current")
                                  a-node)))
                   [:.buttons]))
  [:.buttons :a]
  (fn [a-node]
    (if (->>
         (str/split (get-in a-node [:attrs :href]) #"/")
         (some #{"projections"}))
      (assoc-in a-node [:attrs :id]
                "current")
      a-node))
  [:#info-header]
  (content (select (html-resource "clad/views/CI_Information.html") [:#climate-projections]))
  [:#view-2-map]
  (content map)
  [:#view-2-map-tool]
  (content (maptools req map))
  [:#view-2-2-chart]
  (content blurb)
  [:#view-2-2-expl]
  (content (chart-help req))
  [:#map-help]
  (content (map-help))
  [:#footer]
  (substitute (select (html-resource "clad/views/View_3.html") [:#footer])))

(deftemplate login "clad/views/Login.html" [])

(defpage "/ci/climate-information/projections/:region/:years/:months/:model/:scenario/:variable/:abs/:regions"
  {:as req}
  (svgmap req
          {:tag :object
           :attrs {(if (good-browser?) :data :src) (make-url "svg" req)
                   :type "image/svg+xml"}}
          {:tag :img
           :attrs {:src (make-url "box" req)
                   :max-width "100%"}}))

(defpage "/" []
  (resp/redirect "/ci/about"))

(defpage "/ci" []
  (resp/redirect "/ci/about"))

(defpage "/ci/about" []
  (two-pane "clad/views/CI_About.html" "about" {:tag :object :attrs {:data "/img/impact-tool.svg"}}))
  
(defpage "/ci/about/impacts" []
  (two-pane "clad/views/CI_Impacts.html" "about" {:tag :object :attrs {:data "/img/impact-tool.svg"}}))
  
  (defpage "/ci/about/adaptation-tool" []
  (two-pane "clad/views/CI_adaptation-tool.html" "adaptation-tool" {:tag :object :attrs {:data "/img/impact-tool.svg"}}))

(defpage "/ci/climate-change/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_ClimateChange.html" "climate-change" tab))
  
(defpage "/ci/adaptation/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_adaptation.html" "adaptation" tab))

(defpage "/ci/sectors/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_sectors.html" "sectors" tab))

(defpage "/ci/resources/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_Resources.html" "resources" tab))

(defpage "/ci/tools/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_impacts.html" "tools" tab))

(defpage "/ci/climate-information/:tab" {:keys [tab]}
  (one-pane "clad/views/CI_Information.html" "climate-information" tab))

(defpage "/ci/csv/:year/:months/:model/:scenario/:variable"
  {:keys [year months model scenario variable]}
  (by-county year months  model scenario variable))
(defpage "/ci/svg/:region/:year/:months/:model/:scenario/:variable/:abs/:regions"
  {:as req}
  (regions-map req))
(defpage "/ci/html/:year/:months" {:keys [year months] } (table-output year months))
(defpage "/ci/box/:county/:years/:months/:model/:scenario/:variable/:abs/:regions" {:keys [county months variable abs model scenario]} 
  (decadal-box county months variable abs model scenario))
(defpage "/ci/questionnaire" {:as req}
  (questionnaire req))
(defpage [:post "/ci/submit"] {:as req}
  (do (put-submit (assoc req :time (format/unparse (format/formatters :basic-date-time) (time/now))))
      (submit req)))
(defpage "/login" []
  (two-pane "clad/views/Login.html" "login" (html-resource "clad/views/terms.html")))
(defpage "/ci/maptools" {:as req}
  (resp/redirect (str "/ci/climate-information/projections/" (:region req)
                 "/" (:years req)
                 "/" (:months req)
                 "/" (:runs req)
                 "/" (:variable req)
                 "/" (:abs req)
                 "/" (:regions req))))

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

