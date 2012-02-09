library(maptools)
library(maps)
library(rgdal)

counties <- readOGR(dsn="/home/anthony/Desktop/County/LandAreaAdmin_ROIandUKNI", layer="LandAreaAdmin_ROIandUKNI")

bycounty <- function(county, run) {
  icarus <- GDAL.open(paste ("/home/anthony/Desktop/ICARUS DATA_1/Coverages/Precipitation/",run,sep=""))
  sgdf <- as(icarus,"SpatialGridDataFrame")
  countydata <- counties[counties@data$COUNTY==county,] 
  ckk=!is.na(overlay(sgdf, countydata))
  kkclipped= sgdf[ckk,]
  mean(as(kkclipped, "data.frame")$band1)}
