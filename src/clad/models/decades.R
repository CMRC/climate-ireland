source("CLM_common.R")

base.path <- "temp/"

cdoseasonaldecadal <- function(run, decade) {
  system(paste("cd " ,base.path, ";cdo splitseas -yseasmean -selyear,", decade, " ", run, ".nc ", run, sep=""))
}
cdoyearlydecadal <- function(run, decade) {
  system(paste("cd " ,base.path, ";cdo yearmean -selyear,", decade, " ", run, ".nc ", run, "J2D", ".nc", sep=""))
}
maketifseasonaldecadal <- function(run, decade, season, variable) {
  system(paste("cd ",base.path, ";gdal_translate -a_ullr -13.3893 56.3125 -3.39428 50.4016 \"NETCDF:", run,
               season, ".nc:", variable, "\" seas.tif",sep=""))
  as(GDAL.open(paste(base.path,"seas.tif",sep="")),"SpatialGridDataFrame")
}

for(start in c(2021L,2031L,2041L,2051L)) {
  for(run in c("MM_caCLM4_A1B_4km","MM_caCLM4_A2_4km","MM_haCLM4_RCP45_4km","MM_haCLM4_RCP85_4km")) {
    cdoyearlydecadal(run, paste(toString(start),sep="/",toString(start+9)))
    cdoseasonaldecadal(run, paste(toString(start),sep="/",toString(start+9)))
    for(season in c("DJF","MAM","JJA","SON","J2D")) {
      for(var in c("lat","lon","PS","TOT_PREC","PMSL","QV_2M","T_2M","RUNOFF_G","RUNOFF_S","TMAX_2M","TMIN_2M","VGUST_DYN")) {
        seasstr <- paste(toString(start),sep="",toString(start+9-2000))
        sd20s <- maketifseasonaldecadal(run, seasstr, season, var) 
        for(province in c("Leinster","Munster","Connaught","Ulster")) {
          byprovince(sd20s, province, run, as.integer(seasstr), season, var)
        }
        for(county in countynames) {
          bycounty(sd20s, county, run, as.integer(seasstr), season, var)
        }
      }
    }
  }
}
