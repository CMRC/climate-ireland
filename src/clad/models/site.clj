(ns clad.models.site)

(def
  ^{:doc
    "Sitemap: Structure
     Page Title
        Sections
           Section id
           Section Title"}
  
  site (array-map
        :home           {:title "Home"
                         :sections [{:id "main"  :title "Home"}]}
        :climate_change {:title "Climate Change"
                         :sections [{:id "essentials"  :title "Essentials"}
                                    {:id "projections" :title "Global Projections"}
                                    {:id "Impacts"     :title "Irish Coasts"}]}
        :adaptation     {:title "Adaptation"
                         :sections [{:id "whyadapt"    :title "Why Climate Adaptation?"}
                                    {:id "adaptcom"    :title "Adaptive Co-Management"}]}
        :tools_methods  {:title "Tools & Methods"
                         :sections [{:id "tm"          :title "Tools & Methods"}]}
        :policy_law     {:title "Policy & Law"
                         :sections [{:id "howag"       :title "How Adaptation Governance Works"}
                                    {:id "eu"          :title "European Union"}
                                    {:id "ireland"     :title "Ireland"}
                                    {:id "regions"     :title "Regions & Communities"}]}
        :case_studies   {:title "Case Studies"
                         :sections [{:id "howdotheydo" :title "How do they manage?"}
                                    {:id "cs_ireland"  :title "Ireland"}
                                    {:id "cs_int"      :title "International"}
                                    {:id "issues"      :title "Look for your specific issue"}
                                    {:id "experience"  :title "Tell us about your experience!"}]}
        :resources      {:title "Resources"
                         :sections [{:id "whyadapt"    :title "Why Climate Adaptation?"}]}
        :icrn           {:title "ICRN"
                         :sections [{:id "whyadapt"    :title "Why Climate Adaptation?"}]}))