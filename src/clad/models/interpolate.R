library(gstat)
library(gdata)
source("couch.R")

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
sdf <- data.frame(stations)
sdf$x <- c(183183.7805,	
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

sdf$y <- c(60049.64795,	
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

getstationmean <- function(station, season,variable,scenario,projected) {
  stationtotal <- 0
  stationcount <- 0
  
  for (month in (season:(season+2))) {
    compmonth <- projected[projected$MONTH == month,]
    stationmean <- mean(compmonth[[station]])
    stationcount <- stationcount + 1
    stationtotal <- stationtotal + stationmean
  }
  
  
  stationavg <- stationtotal / stationcount
  return (stationavg)
}

populate <- function(sdf,varname,model,scenario,normal) {
  
  x=seq(from=-26995.9, to=423004.095, by=1000)
  y=seq(from=-2237.46, to=480762.54, by=1000)
  ireland = expand.grid(x=x, y=y)
  print(summary(ireland))
  coordinates(ireland) = ~x+y
  gridded(ireland) = TRUE
  
  
  z <- krige(formula = avg ~ 1, locations = ~ x + y, data = sdf, newdata = ireland) 
  
  sgdf <- as(z,"SpatialGridDataFrame")
  sgdf@proj4string = counties@proj4string
  sgdf$band1 <- sgdf$var1.pred
  print(summary(sgdf))
  
  print(seasons[season+1])
  run <- paste(varname,toString(normal),seasons[season+1],sep="")

  for(county in countynames) {
    bycounty(sgdf, county, varname, run, model, scenario)
  }
  for(province in c("Leinster", "Munster", "Connaught", "Ulster")) {
    byprovince(sgdf, province, varname, run, model, scenario)
  }
  NI(sgdf, varname, run, model, scenario)
}

for(model in c("CCCM","CSIRO","HadCM3")) {
  for(scenario in c("A2","B2")) {
    for(variable in c("TOT_PREC","TMAX_2M","TMIN_2M")) {
      head = "/var/data/icarus/GCMs/GCM_"
      tail = paste("_",variable,".xls",sep="")
      vals = read.xls(paste(head, model, tail, sep=""),sheet=1)
      for (normal in c(1960,2010,2020,2030,2040,2050,2060,2070)) {
        for (season in 0:3) {
          projected = vals[as.integer(vals$YEAR) %in% normal:(normal + 29),]
          names(projected) = gsub("^[^0-9]*([0-9]{3,4}).*$","\\1",names(projected))
          sdf$avg =  apply(sdf,1,function(rw) getstationmean(rw[1],season*3 +1, variable, scenario, projected)) 
          if (variable == "TMAX_2M") {
            sdf$avg = sdf$avg + 273.15
            sdf$max = sdf$avg
          } else if (variable == "TMIN_2M") {
            sdf$avg = sdf$avg + 273.15
            sdf$min = sdf$avg
            sdf$avg = (sdf$min + sdf$max) / 2
            populate(sdf,"T_2M",model,scenario,normal)
            sdf$avg = sdf$min
          }
          populate(sdf,variable,model,scenario,normal)
        }
      }
    }
  }
}

