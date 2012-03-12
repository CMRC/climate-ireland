library(maptools)
library(maps)
library(rgdal)
library(RCurl)
library(RJSONIO)

#Sys.setenv("http_proxy" = "")

counties <- readOGR(dsn="/home/anthony/CLAD/resources/County/LandAreaAdmin_ROIandUKNI", layer="counties")

base.path <- "./"

year <- function(run, year) {
  system(paste("cd " ,base.path, ";cdo -s seasmean ", run, " seas.nc;cdo -s selyear,", year, " seas.nc year.nc"))
}
seas <- function(run, season, variable) {
  system(paste("cd " ,base.path, ";cdo -s selseas,", season, " year.nc sy.nc;gdal_translate -a_ullr -13.3893 56.3125 -3.39428 50.4016 \"NETCDF:sy.nc:", variable, "\" temp.tif",sep=""))
  as(GDAL.open(paste(base.path,"temp.tif",sep="")),"SpatialGridDataFrame")
}


makeurl <- function(run,county,year,season,variable) {
  strip <- gsub("(\\s)","", county)
  paste("http://localhost:5984/icip/",run, strip, year, season, variable, sep="")
}

bycounty <- function(sgdf, county, run, year, season, variable, description, units) {
  countydata <- counties[counties@data$COUNTY==county,] 
  ckk=!is.na(overlay(sgdf, countydata))
  kkclipped= sgdf[ckk,]
  val <- mean(as(kkclipped, "data.frame")$band1)
  scenario <- gsub(".*CLM4_(.*)_4km.*","\\1",run)
  model <- if(any (grep ("MM_ha", run)))
    "HadGEM"
  else if(any (grep ("MM_ca",run)))
    "CGCM3.1"
  
  rev <- fromJSON(getURL(makeurl(run,county,year,season,variable)))["_rev"]
  try(
      if(is.na(rev)){
        getURL(makeurl(run,county,year,season,variable),
               customrequest="PUT",
               httpheader=c('Content-Type'='application/json'),
               postfields=toJSON(list(county=county, year=year, months=season,
                 model=model, scenario=scenario,
                 datum.value=val, datum.units=units, datum.description=description)))
      } else {
        getURL(makeurl(run,county,year,season,variable),
               customrequest="PUT",
               httpheader=c('Content-Type'='application/json'),
               postfields=toJSON(list(county=county, year=year, months=season,
                 model=model,scenario=scenario,
                 datum.value=val, datum.units=units, datum.description=description,
                 '_rev'=toString(rev))))
      },silent=T)
}

byrun <-function(run) { 
  countynames <- c("Carlow", "Cavan", "Clare", "Cork", "Donegal", "Dublin", "Galway", "Kerry", "Kildare",
                   "Kilkenny", "Laois", "Leitrim", "Limerick", "Longford", "Louth", "Mayo", "Meath", "Monaghan",
                   "North Tipperary", "Offaly", "Roscommon", "Sligo", "South Tipperary", "Waterford", "Westmeath",
                   "Wexford", "Wicklow")
  for(year in 2021:2060) {
    year(run,year)
    for(season in c("djf","mama","jja","son")) {
      sgdf <- seas(run,toupper(season), "PS")
      for(county in countynames) {
        bycounty(sgdf, county, run, year, season, "PS", "surface pressure","Pa")
      }
    }
  }
}
    
runs <- c("MM_caCLM4_A1B_4km.nc")

lapply(runs, byrun)

