library(maptools)
library(maps)
library(rgdal)
library(RCurl)
library(RJSONIO)

Sys.setenv("http_proxy" = "")

counties <- readOGR(dsn="/home/anthony/County/ING/LandAreaAdmin_ROIandUKNI", layer="LandAreaAdmin_ROIandUKNI")
print(summary (counties))

makeurl <- function(run,county,model,scenario) {
  strip <- gsub("(\\s)","", county)
  paste("http://localhost:5984/climate_dev6/",run, strip, model, scenario, sep="")
}
clip <- function(county, run, var, countydata,sgdf,model,scenario) {
  ckk=!is.na(overlay(sgdf, countydata))
  kkclipped= sgdf[ckk,]
  val <- mean(as(kkclipped, "data.frame")$band1)
  print(county)
  print(run)
  intyear <- as.integer(gsub("^.*([0-9]{2}[0-9])[0-9]\\w+","\\11",run))
  print(intyear)
  year <- paste(intyear,"-",intyear+29L,sep="")
  print(year)
  months <- toupper(gsub("^.*[0-9]{4}(\\w+)","\\1",run))
  print(months)
  rev <- fromJSON(getURL(makeurl(run,county,model,scenario)))["_rev"]
  if(is.na(rev)){
    getURL(makeurl(run,county,model,scenario),
           customrequest="PUT",
           httpheader=c('Content-Type'='application/json'),
           postfields=toJSON(list(region=county, year=year, months=months,
             model=model, scenario=scenario,
             datum.value=val, datum.variable=var)))
  } else {
    getURL(makeurl(run,county,model,scenario),
           customrequest="PUT",
           httpheader=c('Content-Type'='application/json'),
           postfields=toJSON(list(region=county, year=year, months=months,
             model=model, scenario=scenario,
             datum.value=val, datum.variable=var,
             '_rev'=toString(rev))))
  }
}
bycounty <- function(sgdf, region, var, run, model, scenario) {
  countydata <- counties[counties@data$COUNTY==region,] 
  clip(region,run,var,countydata,sgdf,model,scenario)
}
byprovince <- function(sgdf, region, var, run, model, scenario) {
  countydata <- counties[counties@data$Province==region,] 
  clip(region,run,var,countydata,sgdf,model,scenario)
}
NI <- function(sgdf, var, run, model, scenario) {
  countydata <- counties[counties@data$Country=="UK",] 
  clip("NI",run,var,countydata,sgdf,model,scenario)
}

