library(maptools)
library(maps)
library(rgdal)
library(RCurl)
library(RJSONIO)

#Sys.setenv("http_proxy" = "")

counties <- readOGR(dsn="/home/anthony/CLAD/resources/County/LandAreaAdmin_ROIandUKNI", layer="provinces")

base.path <- "./temp/"

openyear <- function(run) {
  system(paste("cd " ,base.path, ";cdo -s yearmean ", run, " ym.nc;cdo -s splityear ym.nc year", sep=""))
}
seas <- function(run, year) {
  system(paste("cd " ,base.path, ";cdo -seasmean year", year, ".nc m", year, ".nc;cdo -s splitseas m", year, ".nc seas",sep=""))
}
yearly <- function(run, yr, var) {
  system(paste("cd ",base.path, ";gdal_translate -a_ullr -13.3893 56.3125 -3.39428 50.4016 \"NETCDF:m", yr, ".nc:", var, "\" year.tif",sep=""))
  as(GDAL.open(paste(base.path,"year.tif",sep="")),"SpatialGridDataFrame")
}
seasonal <- function(run, season, variable) {
  system(paste("cd ",base.path, ";gdal_translate -a_ullr -13.3893 56.3125 -3.39428 50.4016 \"NETCDF:seas", season, ".nc:", variable, "\" seas.tif",sep=""))
  as(GDAL.open(paste(base.path,"seas.tif",sep="")),"SpatialGridDataFrame")
}


makeurl <- function(run,county,year,season,variable) {
  strip <- gsub("(\\s)","", county)
  paste("http://localhost:5984/climate/",run, strip, year, season, variable, sep="")
}

bycounty <- function(sgdf, county, run, year, season, variable) {
  countydata <- counties[counties@data$COUNTY==county,] 
  ckk=!is.na(overlay(sgdf, countydata))
  kkclipped= sgdf[ckk,]
  val <- mean(as(kkclipped, "data.frame")$band1)
  scenario <- gsub(".*CLM4_(.*)_4km.*","\\1",run)
  model <- if(any (grep ("MM_ha", run)))
    "HadGEM"
  else if(any (grep ("MM_ca",run)))
    "CGCM31"
  
  rev <- fromJSON(getURL(makeurl(run,county,year,season,variable)))["_rev"]
  try(
      if(is.na(rev)){
        getURL(makeurl(run,county,year,season,variable),
               customrequest="PUT",
               httpheader=c('Content-Type'='application/json'),
               postfields=toJSON(list(county=county, year=year, months=season,
                 model=model, scenario=scenario,
                 datum.value=val,datum.variable=variable)))
      } else {
        getURL(makeurl(run,county,year,season,variable),
               customrequest="PUT",
               httpheader=c('Content-Type'='application/json'),
               postfields=toJSON(list(county=county, year=year, months=season,
                 model=model,scenario=scenario,
                 datum.value=val,datum.variable=variable,
                 '_rev'=toString(rev))))
      },silent=T)
}

byprovince <- function(sgdf, province, run, year, season, variable) {
  countydata <- counties[counties@data$Province==province,] 
  ckk=!is.na(overlay(sgdf, countydata))
  kkclipped= sgdf[ckk,]
  val <- mean(as(kkclipped, "data.frame")$band1)
  scenario <- gsub(".*CLM4_(.*)_4km.*","\\1",run)
  model <- if(any (grep ("MM_ha", run)))
    "HadGEM"
  else if(any (grep ("MM_ca",run)))
    "CGCM31"
  
  rev <- fromJSON(getURL(makeurl(run,province,year,season,variable)))["_rev"]
  try(
      if(is.na(rev)){
        getURL(makeurl(run,province,year,season,variable),
               customrequest="PUT",
               httpheader=c('Content-Type'='application/json'),
               postfields=toJSON(list(province=province, year=year, months=season,
                 model=model, scenario=scenario,
                 datum.value=val,datum.variable=variable)))
      } else {
        getURL(makeurl(run,province,year,season,variable),
               customrequest="PUT",
               httpheader=c('Content-Type'='application/json'),
               postfields=toJSON(list(province=province, year=year, months=season,
                 model=model,scenario=scenario,
                 datum.value=val,datum.variable=variable,
                 '_rev'=toString(rev))))
      },silent=T)
}

byrun <-function(run) { 
  countynames <- c("Carlow", "Cavan", "Clare", "Cork", "Donegal", "Dublin", "Galway", "Kerry", "Kildare",
                   "Kilkenny", "Laois", "Leitrim", "Limerick", "Longford", "Louth", "Mayo", "Meath", "Monaghan",
                   "North Tipperary", "Offaly", "Roscommon", "Sligo", "South Tipperary", "Waterford", "Westmeath",
                   "Wexford", "Wicklow")
  openyear(run)
  for(year in 2021:2060) {
    seas(run,year)
    for(var in c("PS","TOT_PREC","PMSL","QV_2M","T_2M","RUNOFF_G","RUNOFF_S","TMAX_2M","TMIN_2M","VGUST_DYN")) {
      sgdfy <- yearly(run,year,var)
      for(season in c("djf","mam","jja","son")) {
        sgdfs <- seasonal(run,toupper(season),var)
        for(county in countynames) {
          bycounty(sgdfy, county, run, year, "j2d", var)
          bycounty(sgdfs, county, run, year, season, var)
        }
        for(province in c("Leinster","Munster","Connaught","Ulster")) {
          byprovince(sgdfy, province, run, year, "j2d", var)
          byprovince(sgdfs, province, run, year, season, var)
        }
      }
    }
   }
  gc()
}
    
runs <- list.files(base.path, pattern="MM_.*\\.nc")

lapply(runs, byrun)

