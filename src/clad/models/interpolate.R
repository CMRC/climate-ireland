library(gstat)
source("couch.R")


a2vals <- read.csv("/home/anthony/icarus/ensemble_scenarios/precip_ensemble_GCMs_1961_2099_a2.csv")
a2maxs <- read.csv("/home/anthony/icarus/ensemble_scenarios/tmax_ensemble_GCMs_1961_2099_a2.csv")
a2mins <- read.csv("/home/anthony/icarus/ensemble_scenarios/tmin_ensemble_GCMs_1961_2099_a2.csv")
b2vals <- read.csv("/home/anthony/icarus/ensemble_scenarios/precip_ensemble_GCMs_1961_2099_b2.csv")
b2maxs <- read.csv("/home/anthony/icarus/ensemble_scenarios/tmax_ensemble_GCMs_1961_2099_b2.csv")
b2mins <- read.csv("/home/anthony/icarus/ensemble_scenarios/tmin_ensemble_GCMs_1961_2099_b2.csv")

##reference period 1961-90
a2ref <- a2vals[as.integer(a2vals$new_year) %in% (1961:1990),]
a2refmaxs <- a2maxs[as.integer(a2maxs$new_year) %in% (1961:1990),]
a2refmins <- a2mins[as.integer(a2mins$new_year) %in% (1961:1990),]

b2ref <- b2vals[as.integer(b2vals$new_year) %in% (1961:1990),]
b2refmaxs <- b2maxs[as.integer(b2maxs$new_year) %in% (1961:1990),]
b2refmins <- b2mins[as.integer(b2mins$new_year) %in% (1961:1990),]

##weather stations
stations <- c("1004","1034","2437","2615",
              "2727","305","3613",
              "3723","3904","4919","518",
              "532","545")

seasons = c("djf","mam","jja","son")

countynames <- c("Carlow", "Cavan", "Clare", "Cork", "Donegal", "Dublin", "Galway", "Kerry", "Kildare",
                 "Kilkenny", "Laois", "Leitrim", "Limerick", "Longford", "Louth", "Mayo", "Meath", "Monaghan",
                 "North Tipperary", "Offaly", "Roscommon", "Sligo", "South Tipperary", "Waterford", "Westmeath",
                 "Wexford", "Wicklow")

##lat longs for stations
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


getstationmean <- function(station, season,variable,scenario,comp, ref, diff) {
  station <- paste(variable,station,"en",scenario,sep="")
  stationtotal <- 0
  stationcount <- 0
  stationreftotal <- 0
  stationrefcount <- 0
  for (month in (season:(season+2))) {
    compmonth <- comp[comp$MONTH == month,]
    refmonth <- ref[ref$MONTH == month,]
    stationmean <- mean(compmonth[[station]])
    print(stationmean)
    stationcount <- stationcount + 1
    stationtotal <- stationtotal + stationmean
    
    stationrefmean <- mean(refmonth[[station]])
    stationrefcount <- stationrefcount + 1
    stationreftotal <- stationreftotal + stationrefmean
  }
  
  
  stationavg <- stationtotal / stationcount
  stationrefavg <- stationreftotal / stationrefcount
  return (diff (stationavg, stationrefavg))
}

populate <- function(df,prefix,varname,scenario="ICARUS") {

  grd <- expand.grid(x=seq(from=-26995.9, to=423004.095, by=1000), y=seq(from=-2237.46, to=480762.54, by=1000) )
  coordinates(grd) <- ~ x+y
  gridded(grd) <- TRUE
  

  z <- krige(formula = avg ~ 1, locations = ~ x + y, data = df, newdata = grd) 
  
  sgdf <- as(z,"SpatialGridDataFrame")
  sgdf$band1 <- sgdf$var1.pred
  print(summary(sgdf))
  
  print(seasons[season+1])
  run <- paste(prefix,toString(normal),seasons[season+1],sep="")
  var <- varname

  countiesarray[[run]] <- sgdf

  for(county in countynames) {
    bycounty(county, var, run, scenario)
  }
  for(province in c("Leinster", "Munster", "Connaught", "Ulster")) {
    byprovince(province, var, run, scenario)
  }
  NI(var,run, scenario)
}

for (normal in c(2010,2020,2030,2040,2050,2060,2070)) {
  for (season in 0:3) {
    ##precipitation
    ##a2
    a2comp <- a2vals[as.integer(a2vals$new_year) %in% normal:(normal + 29),]

    df$a2avg <- apply(df,1,function(row) getstationmean(row[1],season*3 +1,"rain","a2",a2comp,a2ref,
                                                        function (comp,ref) {100 * (comp - ref) / ref}))
    df$avg <- df$a2avg
    populate(df,"precip","TOT_PREC","a2")

    ##b2
    b2comp <- b2vals[as.integer(b2vals$new_year) %in% normal:(normal + 29),]

    df$b2avg <- apply(df,1,function(row) getstationmean(row[1],season*3 +1,"rain","b2",b2comp,b2ref,
                                                        function (comp,ref) {100 * (comp - ref) / ref}))
    df$avg <- df$b2avg
    populate(df,"precip","TOT_PREC","b2")

    ##a2 + b2 ensemble
    df$avg <- (df$a2avg + df$b2avg) / 2
    populate(df,"precip","TOT_PREC")

    ##mean temperature
    #a2
    a2compmins <- a2mins[as.integer(a2mins$new_year) %in% normal:(normal + 29),]
    a2compmaxs <- a2maxs[as.integer(a2mins$new_year) %in% normal:(normal + 29),]
    df$a2min <- apply(df,1,function(row) getstationmean(row[1],season*3 +1,"min","a2",a2compmins,a2refmins,
                                                      function (comp,ref) {comp - ref}))
    df$a2max <- apply(df,1,function(row) getstationmean(row[1],season*3 +1,"max","a2",a2compmaxs,a2refmaxs,
                                                      function (comp,ref) {comp - ref}))
    df$a2avg <- (df$a2min + df$a2max) / 2
    df$avg <- df$a2avg
    populate(df,"temp","T_2M","a2")

    #b2
    b2compmins <- b2mins[as.integer(b2mins$new_year) %in% normal:(normal + 29),]
    b2compmaxs <- b2maxs[as.integer(b2mins$new_year) %in% normal:(normal + 29),]
    df$b2min <- apply(df,1,function(row) getstationmean(row[1],season*3 +1,"min","b2",b2compmins,b2refmins,
                                                      function (comp,ref) {comp - ref}))
    df$b2max <- apply(df,1,function(row) getstationmean(row[1],season*3 +1,"max","b2",b2compmaxs,b2refmaxs,
                                                      function (comp,ref) {comp - ref}))
    df$b2avg <- (df$b2min + df$b2max) / 2
    df$avg <- df$b2avg
    populate(df,"temp","T_2M","b2")

    #ensemble
    df$avg <- (df$a2avg + df$b2avg) / 2
    populate(df,"temp","T_2M")
  }
}

    
