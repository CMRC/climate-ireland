library(maptools)
library(maps)
library(rgdal)
library(RCurl)
library(RJSONIO)

Sys.setenv("http_proxy" = "")

counties <- readOGR(dsn="/home/anthony/Desktop/County/LandAreaAdmin_ROIandUKNI", layer="LandAreaAdmin_ROIandUKNI")
countiesarray = new.env()

base.path <- "/home/anthony/CLAD/resources/Temperature/"

populatecounties <- function(run) {
  countiesarray[[run]] = sgdf <- as(GDAL.open(paste(base.path,run,sep="")),"SpatialGridDataFrame")
}

makeurl <- function(run,county) {
  strip <- gsub("(\\s)","", county)
  paste("http://localhost:5984/icip/",run, strip, sep="")
}

bycounty <- function(county, run) {
  sgdf <- countiesarray[[run]]
  countydata <- counties[counties@data$COUNTY==county,] 
  ckk=!is.na(overlay(sgdf, countydata))
  kkclipped= sgdf[ckk,]
  val <- mean(as(kkclipped, "data.frame")$band1)

  year <- gsub("temp(\\d{4})\\w+","\\1",run)
  months <- gsub("temp\\d{4}(\\w+)","\\1",run)
  rev <- fromJSON(getURL(makeurl(run,county)))["_rev"]
  if(is.na(rev)){
    getURL(makeurl(run,county),
           customrequest="PUT",
           httpheader=c('Content-Type'='application/json'),
           postfields=toJSON(list(county=county, year=year, months=months,
             datum.value=val, datum.units="%", datum.description="temperature")))
  } else {
    getURL(makeurl(run,county),
           customrequest="PUT",
           httpheader=c('Content-Type'='application/json'),
           postfields=toJSON(list(county=county, year=year, months=months,
             datum.value=val, datum.units="%", datum.description="temperature",
             '_rev'=toString(rev))))
  }
}

byrun <-function(run) { 
  populatecounties(run)

  countynames <- c("Carlow", "Cavan", "Clare", "Cork", "Donegal", "Dublin", "Galway", "Kerry", "Kildare",
                   "Kilkenny", "Laois", "Leitrim", "Limerick", "Longford", "Louth", "Mayo", "Meath", "Monaghan",
                   "North Tipperary", "Offaly", "Roscommon", "Sligo", "South Tipperary", "Waterford", "Westmeath",
                   "Wexford", "Wicklow")
  for(county in countynames) {
    bycounty(county, run)
  }
}

runs <- c("temp2020jja", "temp2020son", "temp2020djf", "temp2020mam", "temp2050jja", "temp2050son",
          "temp2050djf", "temp2050mam", "temp2080jja", "temp2080son", "temp2080djf", "temp2080mam")

lapply(runs, byrun)
