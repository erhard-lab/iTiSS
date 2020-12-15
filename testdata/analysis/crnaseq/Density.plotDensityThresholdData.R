dataFile <- 'analysis/crnaseq/DensitydensityThresholdData.tsv'
pdfFile <- 'analysis/crnaseq/Density.densityThresholds.pdf'
customThresh <- 6.206889305859775E-7
threshAutoParam <- 6.206889305859775E-7
manualSelectionFile <- 'analysis/crnaseq/DensitydensityManualSelection.tsv'
manualSelectionPdf <- 'analysis/crnaseq/DensitydensityManuelThresh.pdf'
library(ggplot2)

multimapThresh <- 99999;

# this will clean the data of values that occure more than multimapThresh times.
# on default settings none will be romved
cleanData <- function(df) {
    tab = table(df[,3])
    tab = tab[which(tab>=multimapThresh)]
    todelete = which(df[,3] %in% names(tab))
    if (length(todelete) > 0) {
        df = df[-(todelete),]
    }
    return(df)
}

df = read.delim(dataFile)
df <- cleanData(df)
df$x = 1:length(df[,1])
df$y = sort(df[,3])
newfile = df[df[,3] > customThresh,]
write.table(newfile, file=paste(manualSelectionFile, sep=""), row.names=FALSE, col.names=TRUE, quote=FALSE, sep="\t")
pdf(manualSelectionPdf)
print(ggplot(df, aes(x=x, y=y)) + geom_point() + geom_hline(aes(yintercept=customThresh, color="red")))
dev.off()
pdf(pdfFile)
print(ggplot(df, aes(x=x, y=y)) + geom_point() + geom_hline(aes(yintercept=threshAutoParam, color="red")))
dev.off()
