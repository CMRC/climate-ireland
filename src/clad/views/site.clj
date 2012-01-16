(ns clad.views.site)
           

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
  
  sitemap [{:title "Home" :file "Home.html"
            :sections [{:title "Irish Coast and CC"              :from :#home_ic}
                       {:title "How can I use this site?"        :from :#home_how}
                       {:title "News and Events"                 :from :#home_news}]}
           {:title "Climate Change" :file "more.html"
            :sections [{:title "Essentials"                                  :from :#essentials
                        :headings [{:title "What is Climate Change?",        :from :#whatis,  }
                                   {:title "Evidence for Climate Change",    :from :#evidence,}
                                   {:title "Why is Climate Change Serious?", :from :#whyis,   }
                                   {:title "Climate Modelling",              :from :#modelling}
                                   {:title "Adaptation and Mitigation",      :from :#adapt,   }]}
                       {:title "Global Projections"                          :from :#project
                        :headings [{:title "Global and Regional Trends"      :from :#project}
                                   {:title "Climate change and Coasts"       :from :#impacts
                                    :subtopics [{:title "Sea level rise"}]}]}
                       {:title "Irish Coasts"                                :from :#ireland
                        :headings [{:title "Climate change in Ireland"       :from :#ireland}
                                   {:title "Impacts on Irish coasts"         :from :#irishcoastsimpacts}
                                   {:title "Regions"                         :from :#regions}
                                   {:title "Sectors"                         :from :#sectors}]}]}
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

