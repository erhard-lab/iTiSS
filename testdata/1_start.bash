# dRNA-seq
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -bams data/dRNAseq/dRNA_repA.8hpi.bam -chromSizes HSV1.chrom.sizes -modType SPARSE_PEAK -prefix analysis/drnaseq/Peaks_RepA -rep X -autoparam -plotParams
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -bams data/dRNAseq/dRNA_repB.8hpi.bam -chromSizes HSV1.chrom.sizes -modType SPARSE_PEAK -prefix analysis/drnaseq/Peaks_RepB -rep X -autoparam -plotParams

# crnaseq
# peaks
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -bams data/cRNAseq/cRNA_repA.8hpi.bam -chromSizes HSV1.chrom.sizes -modType DENSE_PEAK -prefix analysis/crnaseq/Peaks_RepA -rep X -autoparam -plotParams
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -bams data/cRNAseq/cRNA_repB.8hpi.bam -chromSizes HSV1.chrom.sizes -modType DENSE_PEAK -prefix analysis/crnaseq/Peaks_RepB -rep X -autoparam -plotParams
# adjust zscore
# use CIT instead of bam
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -reads analysis/crnaseq/Peaks_RepABAM2CIT_convertedReads.cit -chromSizes HSV1.chrom.sizes -modType DENSE_PEAK -prefix analysis/crnaseq/Peaks_RepA_adjusted -rep X -zscore 2
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -reads analysis/crnaseq/Peaks_RepBBAM2CIT_convertedReads.cit -chromSizes HSV1.chrom.sizes -modType DENSE_PEAK -prefix analysis/crnaseq/Peaks_RepB_adjusted -rep X -zscore 2

# density
# use cit instead of bam
# totalize reads
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -reads analysis/crnaseq/Peaks_RepABAM2CIT_convertedReads.cit analysis/crnaseq/Peaks_RepBBAM2CIT_convertedReads.cit -chromSizes HSV1.chrom.sizes -modType DENSITY -prefix analysis/crnaseq/Density -rep XX -autoparam -plotParams

# merge
# combine dRNAseq
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSSMerger2 -inTiss analysis/drnaseq/Peaks_RepA.sparsePeak.tsv analysis/drnaseq/Peaks_RepB.sparsePeak.tsv -prefix analysis/merged/drnaseq -minScore 2 -gap 10 -ext 10

java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSSMerger2 -inTiss analysis/crnaseq/Peaks_RepA_adjusted.densePeak.tsv analysis/crnaseq/Peaks_RepB_adjusted.densePeak.tsv -prefix analysis/merged/crnaseq_peaks -minScore 1
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSSMerger2 -inTiss analysis/crnaseq/Density.density.tsv -inTsr analysis/merged/crnaseq_peaks.tsr -prefix analysis/merged/crnaseq -minScore 2 -gap 1 -ext 1

# combine All
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSSMerger2 -inTsr analysis/merged/drnaseq.tsr analysis/merged/crnaseq.tsr -prefix analysis/merged/TiSS -minScore 1 -gap 10 -ext 10
