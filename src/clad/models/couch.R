library(maptools)
library(maps)
library(rgdal)
library(RCurl)
library(RJSONIO)

#Sys.setenv("http_proxy" = "")

counties <- readOGR(dsn="/home/anthony/County/ING/LandAreaAdmin_ROIandUKNI", layer="LandAreaAdmin_ROIandUKNI")
print(summary (counties))
countiesarray = new.env()

populatecounties <- function(run, base.path) {
  countiesarray[[run]] = sgdf <- as(GDAL.open(paste(base.path,run,sep="")),"SpatialGridDataFrame")
}

makeurl <- function(run,county,scenario) {
  strip <- gsub("(\\s)","", county)
  paste("http://localhost:5984/climate_dev2/",run, strip, scenario, sep="")
}
clip <- function(county, run, var, countydata,sgdf,scenario) {
  ckk=!is.na(overlay(sgdf, countydata))
  kkclipped= sgdf[ckk,]
  val <- mean(as(kkclipped, "data.frame")$band1)
  print(county)
  print(run)
  intyear <- as.integer(gsub("^.*([0-9]{2})([0-9])[0-9]\\w+","\\21",run))
  print(intyear)
  year <- paste("20",intyear,"-",intyear+29L,sep="")
  print(year)
  months <- toupper(gsub("^.*[0-9]{4}(\\w+)","\\1",run))
  print(months)
  rev <- fromJSON(getURL(makeurl(run,county,scenario)))["_rev"]
  if(is.na(rev)){
    getURL(makeurl(run,county,scenario),
           customrequest="PUT",
           httpheader=c('Content-Type'='application/json'),
           postfields=toJSON(list(region=county, year=year, months=months,
             model="ICARUS", scenario=scenario,
             datum.value=val, datum.variable=var)))
  } else {
    getURL(makeurl(run,county,scenario),
           customrequest="PUT",
           httpheader=c('Content-Type'='application/json'),
           postfields=toJSON(list(region=county, year=year, months=months,
             model="ICARUS", scenario=scenario,
             datum.value=val, datum.variable=var,
             '_rev'=toString(rev))))
  }
}
bycounty <- function(region, var, run, scenario) {
  sgdf <- countiesarray[[run]]
  countydata <- counties[counties@data$COUNTY==region,] 
  clip(region,run,var,countydata,sgdf,scenario)
}
byprovince <- function(region, var, run, scenario) {
  sgdf <- countiesarray[[run]]
  countydata <- counties[counties@data$Province==region,] 
  clip(region,run,var,countydata,sgdf,scenario)
}
NI <- function(var, run, scenario) {
  sgdf <- countiesarray[[run]]
  countydata <- counties[counties@data$Country=="UK",] 
  clip("NI",run,var,countydata,sgdf,scenario)
}

