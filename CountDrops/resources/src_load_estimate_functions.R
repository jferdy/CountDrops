## Estimates load per volume unit for one sample using a quasipoisson model
## ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
## Arguments are:
## -- CFU counts for each possible type of bacteria (total will be computed)
## -- Volume
## -- Concentration (with values between zero and one, one being a non diluted
## sample and zero an infinitely diluted sample)
## Estimates are provided as log10 load per volume unit

estimateLoadOneSample <- function(counts,volume,concentration,max.cfu.per.drop=30) {
  if(any(concentration<=0) || any(concentration>1)) {
      cat("ERROR -- Concentration must be a number greater than zero and less or equal to one!\n")
      cat("Non diluted samples avec a concentration of one.\n")
    return(NULL)
  }
  if(any(volume<=0)){
    cat("ERROR -- Volume must be greater than zero!\n")
    return(NULL)
  }
  if(any(!is.na(counts) & counts<0)) {
    cat("ERROR -- CFU counts must be greater than zero!\n")
    return(NULL)
  }
  if(is.numeric(counts)) {
    counts <- data.frame(CFU=counts)
  }
  if(nrow(counts)!=length(concentration) || nrow(counts)!=length(volume) || length(concentration)!=length(volume)) {
    cat("ERROR -- size of counts, volume and concentration must be the same!\n")
    cat("counts :",nrow(counts),"\n")
    cat("concentration :",length(concentration),"\n")
    cat("volume :",length(volume),"\n")
    return(NULL)
  }
  # total count
  if(ncol(counts)>1) {
    counts$total <- apply(counts,1,sum)
  }
  fit <- function(y) {
    # ignore counts above threshold max.cfu.per.drop
    y[y>=max.cfu.per.drop] <- NA
    if(sum(!is.na(y))<=0) return(rep(NA,2))
    if(sum(y,na.rm=T)<=0) return(c(-Inf,NA))

    ###############################################################################
    #       fit quasi poisson model -- glmnb fails in too many cases...           #
    m <- try(glm(y~1+offset((log(volume)+log(concentration))),family=quasipoisson))
    ###############################################################################
    
    if(class(m)!="try-error" && m$converged) {
      # glm has converged
      z <- summary(m)$coefficients[1:2]/log(10)
    } else {
      # glm has failed!
      z <- rep(NA,2)
    }
    return(z)
  }
  
  res <- apply(counts,2,fit)
  rownames(res) <- c("estimate","SE")
  return(res)
}

## Returns the most informative CFU count
## ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
## Arguments are:
## -- CFU counts for each possible type of bacteria (total will be computed)
## -- Volume
## -- Concentration (with values between zero and one)

mostInformativeCountOneSample <- function(counts,volume,concentration,max.cfu.per.drop=30) {
  counts$total <- apply(counts,1,sum)
  get <- function(y) {
    # ignore counts aboce threshold max.cfu.per.drop
    y[y>=max.cfu.per.drop] <- NA
    if(any(!is.na(y) && y>0)) {
      # if positive counts are present
      # returns the position of the greatest count
      # below threshold
      pos <- which.max(y)
    } else {
      # otherwise returns the position of 
      # the least diluted sample with zero count
      pos <- which.max(ifelse(is.na(y),0,concentration))
    }
    return(c(y[pos],volume[pos],concentration[pos]))
  }
  
  res <- apply(counts,2,get)
  rownames(res) <- c("CFU","volume","concentration")
  return(res)
}

## Call estimate and most informative on a data.frame with several samples

estimateLoad <- function(d,max.cfu.per.drop=30) {
    if(! ("sample_ID" %in% colnames(d))) return(NULL)
    if(! ("Volume" %in% colnames(d))) return(NULL)
    if(! ("Dilution" %in% colnames(d))) return(NULL)

    l <- colnames(d)=="NA."
    if(any(l)) colnames(d)[l] <- "na"
    
    posFields <- 1:(grep("Volume",colnames(d))-1)
    posCFU <- (grep("Dilution",colnames(d))+1):ncol(d)
            
    ld <- split(d,d$sample_ID)

    result <- data.frame(matrix(NA,ncol=length(posFields),nrow=length(ld)))
    colnames(result) <- colnames(d)[posFields]
    rownames(result) <- names(ld)
    
    for(i in posFields) result[,i] <- sapply(ld,function(dtmp) {
        z <- dtmp[,i]
        ## field must have only one value for each sample !
        ## NA is returned otherwise
        if(length(unique(z))>1) return(NA)
        return(z[1])
    })
    ## columns with only NA are removed
    result <- result[,apply(result,2,function(x) any(!is.na(x)))]

    res <- NULL
    res <- t(sapply(ld,function(dtmp) {
       estimateLoadOneSample(dtmp[,posCFU],dtmp$Volume,1/dtmp$Dilution,max.cfu.per.drop) 
   }))
    res <- data.frame(res)
    colnames(res) <- c(paste(colnames(d)[sort(rep(posCFU,2))],c("","_se"),sep=""),
                       "TOTAL","TOTAL_se")        
    result <- cbind(result,res)

    res <- NULL
    res <- t(sapply(ld,function(dtmp) {
        mostInformativeCountOneSample(dtmp[,posCFU],dtmp$Volume,1/dtmp$Dilution,max.cfu.per.drop) 
    }))
    colnames(res) <- c(paste("BEST_",colnames(d)[sort(rep(posCFU,3))],c("","_Volume","_Dilution"),sep=""),
                       "BEST_TOTAL","BEST_TOTAL_Volume","BEST_TOTAL_Dilution")        
    result <- cbind(result,res)
        
    return(result)
}
