(ns clad.models.site)

(def
  ^{:doc
    "Sitemap: Structure
     Page Title
        Sections
           Section id
           Section Title"}
  
  site {:climate_change {:title "Climate Change"
                         :sections [{:id "essentials"  :title "Essentials"}
                                    {:id "projections" :title "Global Projections"}
                                    {:id "Impacts"     :title "Irish Coasts"}]}})