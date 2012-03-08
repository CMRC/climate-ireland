library(maptools)
library(maps)
library(rgdal)
library(RCurl)
library(RJSONIO)

Sys.setenv("http_proxy" = "")

counties <- readOGR(dsn="/home/anthony/Desktop/County/LandAreaAdmin_ROIandUKNI", layer="counties")
countiesarray = new.env()

base.path <- "/home/anthony/CLM4-Barry/CLM4_Data/CLM4_A1B_1/"

open <- function(run) {
  system(paste("cd " ,base.path, ";gdal_translate -a_ullr -13.3893 56.3125 -3.39428 50.4016 \"", run, "\" temp.tif",sep=""))
  as(GDAL.open(paste(base.path,"temp.tif",sep="")),"SpatialGridDataFrame")
}

makeurl <- function(run,county) {
  strip <- gsub("(\\s)","", county)
  paste("http://localhost:5984/icip/",run, strip, sep="")
}

bycounty <- function(county, run) {
  sgdf <- open(run)
  countydata <- counties[counties@data$COUNTY==county,] 
  ckk=!is.na(overlay(sgdf, countydata))
  kkclipped= sgdf[ckk,]
  val <- mean(as(kkclipped, "data.frame")$band1)

  year <- "2020"
  months <- "j2d"
  rev <- fromJSON(getURL(makeurl(run,county)))["_rev"]
  if(is.na(rev)){
    getURL(makeurl(run,county),
           customrequest="PUT",
           httpheader=c('Content-Type'='application/json'),
           postfields=toJSON(list(county=county, year=year, months=months,
             datum.value=val, datum.units="K", datum.description="2m temperature")))
  } else {
    getURL(makeurl(run,county),
           customrequest="PUT",
           httpheader=c('Content-Type'='application/json'),
           postfields=toJSON(list(county=county, year=year, months=months,
             datum.value=val, datum.units="K", datum.description="2m temperature",
             '_rev'=toString(rev))))
  }
}

byrun <-function(run) { 
  countynames <- c("Carlow", "Cavan", "Clare", "Cork", "Donegal", "Dublin", "Galway", "Kerry", "Kildare",
                   "Kilkenny", "Laois", "Leitrim", "Limerick", "Longford", "Louth", "Mayo", "Meath", "Monaghan",
                   "North Tipperary", "Offaly", "Roscommon", "Sligo", "South Tipperary", "Waterford", "Westmeath",
                   "Wexford", "Wicklow")
  for(county in countynames) {
    bycounty(county, run)
  }
}

runs <- c("NETCDF:CLM4_A1B_1_max.nc:T_2M")

lapply(runs, byrun)

