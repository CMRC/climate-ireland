library(gstat)



vals <- read.csv("/home/anthony/icarus/ensemble_scenarios/precip_ensemble_GCMs_1961_2099_a2.csv")

ref <- vals[as.integer(vals$new_year) %in% (1961:1990),]
normal <- (2041:2070)
comp <- vals[as.integer(vals$new_year) %in% normal,]

stations <- c("rain1004ena2","rain1034ena2","rain2437ena2","rain2615ena2",
              "rain2727ena2","rain305ena2","rain3613ena2",
              "rain3723ena2","rain3904ena2","rain4919ena2","rain518ena2",
              "rain532ena2","rain545ena2")

df <- data.frame(stations)
df$x <- c(183183.7805,	
          69165.25357	,
          250103.6228	,
          313779.7728	,
          134587.3127	,
          45702.50913	,
          304116.7206	,
          249476.9821	,
          166549.9322	,
          207400.7442	,
          137972.0533	,
          316983.0846	,
          241963.292 )

df$y <- c(60049.64795,	
          332842.3452	,
          326307.4506	,
          112173.7605	,
          273929.6406	,
          78721.44888	,
          229472.7334	,
          157329.4897	,
          66160.10405	,
          204381.832	,
          160256.2024	,
          243380.7743	,
          458585.7608)


getstationmean <- function(station) {
  stationtotal <- 0
  stationcount <- 0
  stationreftotal <- 0
  stationrefcount <- 0
  for (month in (1:3)) {
    compmonth <- comp[comp$MONTH == month,]
    refmonth <- ref[ref$MONTH == month,]
    stationmean <- mean(compmonth[[station]])
    stationcount <- stationcount + 1
    stationtotal <- stationtotal + stationmean
    
    stationrefmean <- mean(refmonth[[station]])
    stationrefcount <- stationrefcount + 1
    stationreftotal <- stationreftotal + stationrefmean
  }
  
  stationavg <- stationtotal / stationcount
  stationrefavg <- stationreftotal / stationrefcount
  return (100 * (stationavg - stationrefavg) / stationrefavg)
}

df$avg <- apply(df,1,function(row) getstationmean(row[1]))



grd <- expand.grid(x=seq(from=-26995.9, to=423004.095, by=1000), y=seq(from=-2237.46, to=480762.54, by=1000) )
coordinates(grd) <- ~ x+y
gridded(grd) <- TRUE


z <- krige(formula = avg ~ 1, locations = ~ x + y, data = df, newdata = grd) 

sgdf <- as(z,"SpatialGridDataFrame")
sgdf$band1 <- sgdf$var1.pred
print(summary(sgdf))

source("couch.R")
run <- "precip2030djf"
var <- "TOT_PREC"

countiesarray[[run]] <- sgdf

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
NI(var,run)
