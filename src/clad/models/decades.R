source("CLM_common.R")

base.path <- "temp/"

cdoseasonaldecadal <- function(run, decade) {
  cmd <- paste("cd " ,base.path, ";cdo splitseas -yseasmean -selyear,", decade, " ", run, ".nc ", run, sep="")
  print (cmd)
  system(cmd)
}
cdoyearlydecadal <- function(run, decade) {
  cmd <- paste("cd " ,base.path, ";cdo yearmean -selyear,", decade, " ", run, ".nc ", run, "J2D", ".nc", sep="")
  print(cmd)
  system(cmd)
}
maketifseasonaldecadal <- function(run, decade, season, variable) {
  com <- paste("cd ",base.path, ";gdal_translate -a_ullr -13.3893 56.3125 -3.39428 50.4016 \"NETCDF:", run,
               season, ".nc:", variable, "\" seas.tif",sep="")
  print(com)
  system(com)
  as(GDAL.open(paste(base.path,"seas.tif",sep="")),"SpatialGridDataFrame")
}

#baseline data
for(run in c("MM_haCLM4_C20_4km", "MM_caCLM4_C20_4km")) {
  cdoyearlydecadal(run, "1961/1990")
  cdoseasonaldecadal(run, "1961/1990")
  for(season in c("DJF","MAM","JJA","SON","J2D")) {
    for(var in c("lat","lon","PS","TOT_PREC","PMSL","QV_2M","T_2M","RUNOFF_G","RUNOFF_S","TMAX_2M","TMIN_2M","VGUST_DYN")) {
      decade <- "1961-1990"
      sd20s <- maketifseasonaldecadal(run, decade, season, var)
      NI(sd20s, run, decade, season, var)
      for(province in c("Leinster","Munster","Connaught","Ulster")) {
        byprovince(sd20s, province, run, decade, season, var)
      }
      for(county in countynames) {
        bycounty(sd20s, county, run, decade, season, var)
      }
    }
  }
}

for(start in c(2021L,2031L)) {
  for(run in c("MM_caCLM4_A1B_4km","MM_caCLM4_A2_4km","MM_haCLM4_RCP45_4km","MM_haCLM4_RCP85_4km")) {
    cdoyearlydecadal(run, paste(toString(start),sep="/",toString(start+29)))
    cdoseasonaldecadal(run, paste(toString(start),sep="/",toString(start+29)))
    for(season in c("DJF","MAM","JJA","SON","J2D")) {
      for(var in c("lat","lon","PS","TOT_PREC","PMSL","QV_2M","T_2M","RUNOFF_G","RUNOFF_S","TMAX_2M","TMIN_2M","VGUST_DYN")) {
        decade <- paste(toString(start),sep="-",toString(start+29))
        sd20s <- maketifseasonaldecadal(run, decade, season, var) 
        NI(sd20s, run, decade, season, var)
        for(province in c("Leinster","Munster","Connaught","Ulster")) {
          byprovince(sd20s, province, run, decade, season, var)
        }
        for(county in countynames) {
          bycounty(sd20s, county, run, decade, season, var)
        }
      }
    }
  }
}
