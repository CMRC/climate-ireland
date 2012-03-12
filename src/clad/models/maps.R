library(maptools)
library(maps)
library(rgdal)

counties <- readOGR(dsn="resources/County/LandAreaAdmin_ROIandUKNI", layer="LandAreaAdmin_ROIandUKNI")

countiesarray = new.env()

populatecounties <- function(run) {
  countiesarray[[run]] = sgdf <- as(GDAL.open(run),"SpatialGridDataFrame")
}

bycounty <- function(county, run) {
  sgdf <- countiesarray[[run]]
  countydata <- counties[counties@data$COUNTY==county,] 
  ckk=!is.na(overlay(sgdf, countydata))
  kkclipped= sgdf[ckk,]
  mean(as(kkclipped, "data.frame")$band1)}
