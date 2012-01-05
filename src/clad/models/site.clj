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
                         :sections [{:id "res_how"     :title "How I can build capacities for climate adaptation?"}
                                    {:id "res_data"    :title "Data and Information"}
                                    {:id "res_guide"   :title "Guidelines"}
                                    {:id "res_legal"   :title "Legal and Policy Support"}
                                    {:id "res_fin"     :title "Financial Support"}
                                    {:id "res_prac"    :title "Practical Measures"}
                                    {:id "res_comm"    :title "Communication and Presentations"}
                                    {:id "res_work"    :title "Working with Communities"}]}
        :icrn           {:title "ICRN"
                         :sections [{:id "icrn_about"  :title "About ICRN"}
                                    {:id "icrn_panel"  :title "National Advisory Panel"}
                                    {:id "icrn_reg"    :title "Regional Units"}
                                    {:id "icrn_vuln"   :title "Vulnerability assessment"}
                                    {:id "icrn_local"  :title "Local Scenarios"}
                                    {:id "icrn_gis"    :title "GIS Coastal Adaptation"}
                                    {:id "icrn_get"    :title "Get Involved!"}]}))