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
                                   {:title "How can I use this site?"}]}]}
           {:title "Climate Change"
            :sections [{:title "Essentials"
                        :headings [{:title "What is Climate Change?",        :from :#whatis,    :to :#Main_Text}
                                   {:title "Evidence for Climate Change",    :from :#evidence,  :to :#Main_Text}
                                   {:title "Why is Climate Change Serious?", :from :#whyis,     :to :#Main_Text}
                                   {:title "Climate Modelling",              :from :#modelling, :to :#Main_Text}
                                   {:title "Adaptation and Mitigation",      :from :#adapt,     :to :#Main_Text}]}
                       {:title "Global Projections"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Irish Coasts"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}]}
           {:title "Adaptation"
            :sections [{:title "Why Climate Adaptation?"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Adaptive Co-Management"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}]}
           {:title "Tools & Methods"
            :sections [{:title "Tools & Methods"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}]}
           {:title "Policy & Law"
            :sections [{:title "How Adaptation Governance Works"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "European Union"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Ireland"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Regions & Communities"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}]}
           {:title "Case Studies"
            :sections [{:title "How do they manage?"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Ireland"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "International"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Look for your specific issue"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Tell us about your experience!"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}]}
           {:title "Resources"
            :sections [{:title "How I can build capacities for climate adaptation?"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Data and Information"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Guidelines"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Legal and Policy Support"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Financial Support"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Practical Measures"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Communication and Presentations"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Working with Communities"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}]}
           {:title "ICRN"
            :sections [{:title "About ICRN"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "National Advisory Panel"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Regional Units"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Vulnerability assessment"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Local Scenarios"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "GIS Coastal Adaptation"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}
                       {:title "Get Involved!"
                        :headings [{:title "Irish Coast and CC"}
                                   {:title "How can I use this site?"}]}]}])

(defn site []
  (reduce (fn [new-map v]
            (assoc new-map
              (keyword (:title v))
              (assoc-in v [:sections]
                        (reduce
                         (fn [inner-map section]
                           (assoc inner-map
                             (keyword
                              (:title section))
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
