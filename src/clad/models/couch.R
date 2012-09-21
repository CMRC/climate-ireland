library(maptools)
library(maps)
library(rgdal)
library(RCurl)
library(RJSONIO)

#Sys.setenv("http_proxy" = "")

counties <- readOGR(dsn="/home/anthony/County/LandAreaAdmin_ROIandUKNI", layer="LandAreaAdmin_ROIandUKNI")
countiesarray = new.env()

populatecounties <- function(run, base.path) {
  countiesarray[[run]] = sgdf <- as(GDAL.open(paste(base.path,run,sep="")),"SpatialGridDataFrame")
}

makeurl <- function(run,county) {
  strip <- gsub("(\\s)","", county)
  paste("http://localhost:5984/climate_dev/",run, strip, sep="")
}

bycounty <- function(county, run) {
  sgdf <- countiesarray[[run]]
  countydata <- counties[counties@data$COUNTY==county,] 
  ckk=!is.na(overlay(sgdf, countydata))
  kkclipped= sgdf[ckk,]
  val <- mean(as(kkclipped, "data.frame")$band1) / 10

  intyear <- as.integer(gsub("temp(\\d{2})(\\d)\\d\\w+","\\21",run))
  year <- paste("20",intyear,"-",intyear+9L,sep="")
  months <- toupper(gsub("temp\\d{4}(\\w+)","\\1",run))
  rev <- fromJSON(getURL(makeurl(run,county)))["_rev"]
  if(is.na(rev)){
    getURL(makeurl(run,county),
           customrequest="PUT",
           httpheader=c('Content-Type'='application/json'),
           postfields=toJSON(list(region=county, year=year, months=months,
             model="ICARUS", scenario="ICARUS",
             datum.value=val, datum.variable="T_2M",datum.units="K")))
  } else {
    getURL(makeurl(run,county),
           customrequest="PUT",
           httpheader=c('Content-Type'='application/json'),
           postfields=toJSON(list(region=county, year=year, months=months,
             model="ICARUS", scenario="ICARUS",
             datum.value=val, datum.variable="T_2M",datum.units="K",
             '_rev'=toString(rev))))
  }
}

byrun <-function(run, base.path) { 
  populatecounties(run, base.path)

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

lapply(runs, byrun, base.path <- "/var/data/coverages/Temperature/")
