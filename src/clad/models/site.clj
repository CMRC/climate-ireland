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
                        :headings [{:title "What is Climate Change?",
                                    :from  :#whatis,
                                    :to    :#Main_Text}]}
                       {:title "Global Projections"}
                       {:title "Irish Coasts"}]}
            {:title "Adaptation"
             :sections [{:title "Why Climate Adaptation?"}
                        {:title "Adaptive Co-Management"}]}
            {:title "Tools & Methods"
             :sections [{:title "Tools & Methods"}]}
            {:title "Policy & Law"
             :sections [{:title "How Adaptation Governance Works"}
                        {:title "European Union"}
                        {:title "Ireland"}
                        {:title "Regions & Communities"}]}
            {:title "Case Studies"
             :sections [{:title "How do they manage?"}
                        {:title "Ireland"}
                        {:title "International"}
                        {:title "Look for your specific issue"}
                        {:title "Tell us about your experience!"}]}
            {:title "Resources"
             :sections [{:title "How I can build capacities for climate adaptation?"}
                        {:title "Data and Information"}
                        {:title "Guidelines"}
                        {:title "Legal and Policy Support"}
                        {:title "Financial Support"}
                        {:title "Practical Measures"}
                        {:title "Communication and Presentations"}
                        {:title "Working with Communities"}]}
            {:title "ICRN"
             :sections [{:title "About ICRN"}
                        {:title "National Advisory Panel"}
                        {:title "Regional Units"}
                        {:title "Vulnerability assessment"}
                        {:title "Local Scenarios"}
                        {:title "GIS Coastal Adaptation"}
                        {:title "Get Involved!"}]}])

(defn site []
  (reduce (fn [new-map v]
            (assoc new-map
              (keyword (:title v))
              (assoc-in v [:sections]
                        (reduce
                         (fn [inner-map section]
                           (assoc inner-map
                             (keyword
                              (:title section)) section))
                         {}
                         (:sections v)))))
          (array-map)
          (reverse sitemap)))

(site)
