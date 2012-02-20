library(maptools)
library(maps)
library(rgdal)

counties <- readOGR(dsn="/home/anthony/Desktop/County/LandAreaAdmin_ROIandUKNI", layer="LandAreaAdmin_ROIandUKNI")

countiesarray = new.env()

populatecounties <- function(run) {
  countiesarray[[run]] = sgdf <- as(GDAL.open(paste ("/home/anthony/CLM4-Barry/CLM4_Data/CLM4_A1B_1",run,sep="")),"SpatialGridDataFrame")
}

bycounty <- function(county, run) {
  sgdf <- countiesarray[[run]]
  countydata <- counties[counties@data$COUNTY==county,] 
  ckk=!is.na(overlay(sgdf, countydata))
  kkclipped= sgdf[ckk,]
  mean(as(kkclipped, "data.frame")$band1)
}


netcdf <- function (county) {
  prec <- readGDAL("NETCDF:/home/anthony/CLM4-Barry/CLM4_Data/CLM4_A1B_1/CLM4_A1B_1_mean.nc:TOT_PREC")
  proj4string(prec) <- CRS("+proj=merc +ellps=WGS84")
  countydata <- counties[counties@data$COUNTY==county,] 
  countiesll <- spTransform(countydata,CRS("+proj=merc ellps=WGS84"))
  ckk=!is.na(overlay(prec, countiesll))
  kkclipped= prec[ckk,]
  mean(as(kkclipped, "data.frame")$band1)
}
