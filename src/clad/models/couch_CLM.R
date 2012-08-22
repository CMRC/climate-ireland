source("CLM_common.R)

byrun("MM_caCLM4_A1B_4km.nc",2021:2060,"temp/")    
byrun("MM_caCLM4_A2_4km.nc",2021:2060,"temp/")    
byrun("MM_haCLM4_RCP45_4km.nc",2021:2060,"temp/")    
byrun("MM_haCLM4_RCP85_4km.nc",2021:2060,"temp/")    
byrun("MM_caCLM4_C20_4km.nc",1961:1990,"temp/")    
byrun("MM_haCLM4_C20_4km.nc",1961:1990,"temp/")    
