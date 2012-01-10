(ns clad.models.site
  (:require [clojure.contrib.string :as str] ))
           

(def
  ^{:doc
    "Sitemap: Structure
     Page Title
        Sections
           Section Title
              Headings
                 Heading Title
                 From selector
                 To selector"}
  
  sitemap [{:title "Home"
            :sections [{:title "Home"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}
                                   {:title "News and Events"}]}]}
           {:title "Climate Change"
            :sections [{:title "Essentials"
                        :headings [{:title "What is Climate Change?",        :from :#whatis,    :to :#Main_Text}
                                   {:title "Evidence for Climate Change",    :from :#evidence,  :to :#Main_Text}
                                   {:title "Why is Climate Change Serious?", :from :#whyis,     :to :#Main_Text}
                                   {:title "Climate Modelling",              :from :#modelling, :to :#Main_Text}
                                   {:title "Adaptation and Mitigation",      :from :#adapt,     :to :#Main_Text}]}
                       {:title "Global Projections"
                        :headings [{:title "Global and Regional Trends"      :from :#project}
                                   {:title "Climate change and Coasts"       :from :#impacts}]}
                       {:title "Irish Coasts"
                        :headings [{:title "Climate change in Ireland"       :from :#ireland}
                                   {:title "Impacts on Irish coasts"         :from :#irishcoastsimpacts}
                                   {:title "Regions"}
                                   {:title "Sectors"}]}]}
           {:title "Adaptation"
            :sections [{:title "Why Climate Adaptation?"
                        :headings [{:title "What is climate adaptation?"}
                                   {:title "Why adapt?"}
                                   {:title "How to approach it?"}]}
                       {:title "Adaptive Co-Management"
                        :headings [{:title "How can I do it?"}
                                   {:title "What do I need?"}]}]}
           {:title "Tools & Methods"
            :sections [{:title "Tools & Methods"
                        :headings [{:title "Which Method Works?"}
                                   {:title "Vulnerability Assessment"}
                                   {:title "Scenario Development"}
                                   {:title "Knowledge Integration"}
                                   {:title "Implementation"}
                                   {:title "Resources"}]}]}
           {:title "Policy & Law"
            :sections [{:title "How Adaptation Governance Works"
                        :headings [{:title "Challenges"}
                                   {:title "Policy and legislation"}
                                   {:title "Implementation"}]}
                       {:title "European Union"}
                       {:title "Ireland"}
                       {:title "Regions & Communities"}]}
           {:title "Case Studies"
            :sections [{:title "How do they manage?"
                        :headings [{:title "FIXME!"}]}
                       {:title "Ireland"
                        :headings [{:title "Tralee Bay Co.Kerry"}
                                   {:title "Bantry Bay Co. Cork"}
                                   {:title "Fingal Co. Dublin"}
                                   {:title "Cork Harbour Co. Cork"}
                                   {:title "Lough Swilly Co. Donegal"}]}
                       {:title "International"
                        :headings [{:title "CS 1"}
                                   {:title "CS 2"}
                                   {:title "CS 3"}]}
                       {:title "Look for your specific issue"}
                       {:title "Tell us about your experience!"}]}
           {:title "Resources"
            :sections [{:title "How I can build capacities for climate adaptation?"
                        :headings [{:title "FIXME!"}]}
                       {:title "Data and Information"
                        :headings [{:title "Climate Change"}
                                   {:title "Sustainable Development"}
                                   {:title "Irish Climate"}
                                   {:title "Irish Coasts and Seas"}]}
                       {:title "Guidelines"}
                       {:title "Legal and Policy Support"}
                       {:title "Financial Support"}
                       {:title "Practical Measures"}
                       {:title "Communication and Presentations"}
                       {:title "Working with Communities"}]}
           {:title "ICRN"
            :sections [{:title "About ICRN"
                        :headings [{:title "FIXME!"}]}
                       {:title "National Advisory Panel"}
                       {:title "Regional Units"
                        :headings [{:title "Tralee"}
                                   {:title "Bantry"}
                                   {:title "Fingal"}]}
                       {:title "Vulnerability assessment"
                        :headings [{:title "Tralee"}
                                   {:title "Bantry"}
                                   {:title "Fingal"}]}
                       {:title "Local Scenarios"
                        :headings [{:title "Tralee"}
                                   {:title "Bantry"}
                                   {:title "Fingal"}]}
                       {:title "GIS Coastal Adaptation"}
                       {:title "Get Involved!"}]}])

(defn site []
  (reduce (fn [new-map v]
            (assoc new-map
              (keyword (str/replace-re #"[^a-zA-Z0-9]" "-" (:title v)))
              (assoc-in v [:sections]
                        (reduce
                         (fn [inner-map section]
                           (assoc inner-map
                             (keyword
                              (str/replace-re #"[^a-zA-Z0-9]" "-" (:title section)))
                             (assoc-in section [:headings]
                                       (reduce
                                        (fn [section-map topic]
                                          (assoc section-map
                                            (keyword
                                             (str/replace-re #"[^a-zA-Z0-9]" "-" (:title topic)))
                                            topic))
                                        (array-map)
                                        (reverse (:headings section))))))
                         (array-map)
                         (reverse (:sections v))))))
          (array-map)
          (reverse sitemap)))

(site)
