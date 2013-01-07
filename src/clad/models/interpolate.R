vals <- read.csv("/home/anthony/icarus/ensemble_scenarios/precip_ensemble_GCMs_1961_2099_a2.csv")

ref <- vals[as.integer(vals$new_year) %in% (1961:1990),]
normal <- (2021:2050)
comp <- vals[as.integer(vals$new_year) %in% normal,]

stations <- c("rain1004ena2","rain1034ena2","rain2437ena2","rain2615ena2",
              "rain2727ena2","rain2922ena2","rain305ena2","rain3613ena2",
              "rain3723ena2","rain3904ena2","rain4919ena2","rain518ena2",
              "rain532ena2","rain545ena2")

for (station in stations) {
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
  print (paste (station, 100 * (stationavg - stationrefavg) / stationrefavg))

}
