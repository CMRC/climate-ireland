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

makeurl <- function(run,county) {
  strip <- gsub("(\\s)","", county)
  paste("http://localhost:5984/climate_dev/",run, strip, sep="")
}
clip <- function(county, run, var, countydata,sgdf) {
  ckk=!is.na(overlay(sgdf, countydata))
  kkclipped= sgdf[ckk,]
  val <- mean(as(kkclipped, "data.frame")$band1) / 10

  intyear <- as.integer(gsub(".*(\\d{2})(\\d)\\d\\w+","\\21",run))
  year <- paste("20",intyear,"-",intyear+9L,sep="")
  months <- toupper(gsub(".*\\d{4}(\\w+)","\\1",run))
  rev <- fromJSON(getURL(makeurl(run,county)))["_rev"]
  if(is.na(rev)){
    getURL(makeurl(run,county),
           customrequest="PUT",
           httpheader=c('Content-Type'='application/json'),
           postfields=toJSON(list(region=county, year=year, months=months,
             model="ICARUS", scenario="ICARUS",
             datum.value=val, datum.variable=var)))
  } else {
    getURL(makeurl(run,county),
           customrequest="PUT",
           httpheader=c('Content-Type'='application/json'),
           postfields=toJSON(list(region=county, year=year, months=months,
             model="ICARUS", scenario="ICARUS",
             datum.value=val, datum.variable=var,
             '_rev'=toString(rev))))
  }
}
bycounty <- function(region, var, run) {
  sgdf <- countiesarray[[run]]
  countydata <- counties[counties@data$COUNTY==region,] 
  clip(region,run,var,countydata,sgdf)
}
byprovince <- function(region, var, run) {
  sgdf <- countiesarray[[run]]
  countydata <- counties[counties@data$Province==region,] 
  clip(region,run,var,countydata,sgdf)
}

byrun <-function(run, var, base.path) { 
  populatecounties(run, base.path)

  countynames <- c("Carlow", "Cavan", "Clare", "Cork", "Donegal", "Dublin", "Galway", "Kerry", "Kildare",
                   "Kilkenny", "Laois", "Leitrim", "Limerick", "Longford", "Louth", "Mayo", "Meath", "Monaghan",
                   "North Tipperary", "Offaly", "Roscommon", "Sligo", "South Tipperary", "Waterford", "Westmeath",
                   "Wexford", "Wicklow")
  for(county in countynames) {
    bycounty(county, var, run)
  }
  for(province in c("Leinster", "Munster", "Connaught", "Ulster")) {
    byprovince(province, var, run)
  }
}

runs <- c("temp2020jja", "temp2020son", "temp2020djf", "temp2020mam", "temp2050jja", "temp2050son",
          "temp2050djf", "temp2050mam", "temp2080jja", "temp2080son", "temp2080djf", "temp2080mam")
precruns <- c("precip2020jja", "precip2020son", "precip2020djf", "precip2020mam", "precip2050jja",
              "precip2050son", "precip2050djf", "precip2050mam", "precip2080jja", "precip2080son",
              "precip2080djf", "precip2080mam")

lapply(runs, byrun, var <- "T_2M", base.path <- "/var/data/coverages/Temperature/")
lapply(precruns, byrun, var <- "TOT_PREC", base.path <- "/var/data/coverages/Precipitation/")
