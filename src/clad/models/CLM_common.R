library(maptools)
library(maps)
library(rgdal)
library(RCurl)
library(RJSONIO)

Sys.setenv("http_proxy" = "")

counties <- readOGR(dsn="/home/anthony/County/LandAreaAdmin_ROIandUKNI", layer="provinces")
print(summary(counties))

openyear <- function(run, base.path) {
  system(paste("cd " ,base.path, ";cdo yearmean ", run, " ym.nc;cdo splityear ym.nc year", sep=""))
  system(paste("cd " ,base.path, ";cdo seasmean ", run, " sm.nc;cdo splitseas sm.nc seas", sep=""))
}
seas <- function(run, seas, base.path) {
  system(paste("cd " ,base.path, ";cdo splityear seas", seas, ".nc ",seas,sep=""))
}
yearly <- function(run, yr, var, base.path) {
  system(paste("cd ",base.path, ";gdal_translate -a_ullr -13.3893 56.3125 -3.39428 50.4016 \"NETCDF:year", yr, ".nc:", var, "\" year.tif",sep=""))
  as(GDAL.open(paste(base.path,"year.tif",sep="")),"SpatialGridDataFrame")
}
seasonal <- function(run, season, year, variable, base.path) {
  system(paste("cd ",base.path, ";gdal_translate -a_ullr -13.3893 56.3125 -3.39428 50.4016 \"NETCDF:", season, year, ".nc:", variable, "\" seas.tif",sep=""))
  as(GDAL.open(paste(base.path,"seas.tif",sep="")),"SpatialGridDataFrame")
}

makeurl <- function(run,county,year,season,variable) {
  strip <- gsub("(\\s)","", county)
  paste("http://localhost:5984/climate_dev2/",run, strip, year, season, variable, sep="")
}

byprovince <- function(sgdf, region, run, year, season, variable) {
  countydata <- counties[counties@data$Province==region,]
  clip(countydata, sgdf, region, run, year, season, variable)
}
NI <- function(sgdf, run, year, season, variable) {
  countydata <- counties[counties@data$Country=="UK",]
  clip(countydata, sgdf, "NI", run, year, season, variable)
}

flipHorizontal <- function(x) {
  if (!inherits(x, "SpatialGridDataFrame")) stop("x must be a SpatialGridDataFrame")
  grd <- getGridTopology(x)
  idx = 1:prod(grd@cells.dim[1:2])
  m = matrix(idx, grd@cells.dim[2], grd@cells.dim[1], byrow = TRUE)[,grd@cells.dim[1]:1]
  idx = as.vector(t(m))
  x@data <- x@data[idx, TRUE, drop = FALSE]
  x
}

flipVertical <- function(x) {
  if (!inherits(x, "SpatialGridDataFrame")) stop("x must be a SpatialGridDataFrame")
  grd <- getGridTopology(x)
  idx = 1:prod(grd@cells.dim[1:2])
  m = matrix(idx, grd@cells.dim[2], grd@cells.dim[1], byrow = TRUE)[grd@cells.dim[2]:1, ]
  idx = as.vector(t(m))
  x@data <- x@data[idx, TRUE, drop = FALSE]
  x
}

clip <- function(countydata, sgdf, region, run, year, season, variable) {
  sgdf <- flipVertical(sgdf)
  ckk=!is.na(over(sgdf, countydata, returnList=TRUE))
  ##print(summary(ckk))
  kkclipped= sgdf[ckk,]
  val <- mean(as(kkclipped, "data.frame")$band1)
  scenario <- gsub(".*CLM4_(.*)_4km.*","\\1",run)
  model <- if(any (grep ("MM_ha", run)))
    "HadGEM"
  else if(any (grep ("MM_ca",run)))
    "CGCM31"
  
  rev <- fromJSON(getURL(makeurl(run,region,year,season,variable)))["_rev"]
  if(is.na(rev)){
    getURL(makeurl(run,region,year,season,variable),
           customrequest="PUT",
           httpheader=c('Content-Type'='application/json'),
           postfields=toJSON(list(region=region, year=year, months=season,
             model=model, scenario=scenario,
             datum.value=val,datum.variable=variable)))
  } else {
    getURL(makeurl(run,region,year,season,variable),
           customrequest="PUT",
           httpheader=c('Content-Type'='application/json'),
           postfields=toJSON(list(region=region, year=year, months=season,
             model=model,scenario=scenario,
             datum.value=val,datum.variable=variable,
             '_rev'=toString(rev))))
  }
}

bycounty <- function(sgdf, region, run, year, season, variable) {
  countydata <- counties[counties@data$COUNTY==region,] 
  clip(countydata, sgdf, region, run, year, season, variable)
}
countynames <- c("Carlow", "Cavan", "Clare", "Cork", "Donegal", "Dublin", "Galway", "Kerry", "Kildare",
                 "Kilkenny", "Laois", "Leitrim", "Limerick", "Longford", "Louth", "Mayo", "Meath", "Monaghan",
                 "North Tipperary", "Offaly", "Roscommon", "Sligo", "South Tipperary", "Waterford", "Westmeath",
                 "Wexford", "Wicklow")

