rm -rf analysis

# Example run of the SPARSE_PEAK module on dRNA-seq with bam-files and automatic threshold calculation
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -bams data/dRNAseq/dRNA_repA.8hpi.bam -chromSizes HSV1.chrom.sizes -modType SPARSE_PEAK -prefix analysis/drnaseq/Peaks_RepA -rep X -autoparam
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -bams data/dRNAseq/dRNA_repB.8hpi.bam -chromSizes HSV1.chrom.sizes -modType SPARSE_PEAK -prefix analysis/drnaseq/Peaks_RepB -rep X -autoparam

# Example run of the DENSE_PEAK module on cRNA-seq with bam-files and automatic threshold calculation
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -bams data/cRNAseq/cRNA_repA.8hpi.bam -chromSizes HSV1.chrom.sizes -modType DENSE_PEAK -prefix analysis/crnaseq/Peaks_RepA -rep X -autoparam
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -bams data/cRNAseq/cRNA_repB.8hpi.bam -chromSizes HSV1.chrom.sizes -modType DENSE_PEAK -prefix analysis/crnaseq/Peaks_RepB -rep X -autoparam

# Example run of the SPARSE_PEAK module on cRNA-seq with CIT-files and a fixed z-Score
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -reads analysis/crnaseq/Peaks_RepABAM2CIT_convertedReads.cit -chromSizes HSV1.chrom.sizes -modType DENSE_PEAK -prefix analysis/crnaseq/Peaks_RepA_adjusted -rep X -zscore 2
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -reads analysis/crnaseq/Peaks_RepBBAM2CIT_convertedReads.cit -chromSizes HSV1.chrom.sizes -modType DENSE_PEAK -prefix analysis/crnaseq/Peaks_RepB_adjusted -rep X -zscore 2

# Example run of the DENSITY module on cRNA-seq with CIT-files and pooling of reads from multiple bam-files
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -reads analysis/crnaseq/Peaks_RepABAM2CIT_convertedReads.cit analysis/crnaseq/Peaks_RepBBAM2CIT_convertedReads.cit -chromSizes HSV1.chrom.sizes -modType DENSITY -prefix analysis/crnaseq/Density -rep XX -autoparam

# Example run of TiSSMerger2 to only call TiSS from multiple predictions that were found in BOTH datasets into a single tsr-file
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSSMerger2 -inTiss analysis/drnaseq/Peaks_RepA.sparsePeak.tsv analysis/drnaseq/Peaks_RepB.sparsePeak.tsv -prefix analysis/merged/drnaseq -minScore 2 -gap 10 -ext 10

# Example run of TiSSMerger2 to pool TiSS from multiple predictions into a single tsr-file
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSSMerger2 -inTiss analysis/crnaseq/Peaks_RepA_adjusted.densePeak.tsv analysis/crnaseq/Peaks_RepB_adjusted.densePeak.tsv -prefix analysis/merged/crnaseq_peaks -minScore 1
# Example run of TiSSMerger2 to pool TiSS from one TiSS-predictions as well as a pooled tsr-file into a single tsr-file with custom TSR extensions
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSSMerger2 -inTiss analysis/crnaseq/Density.density.tsv -inTsr analysis/merged/crnaseq_peaks.tsr -prefix analysis/merged/crnaseq -minScore 2 -gap 1 -ext 1

# Example run of TiSSMerger2 to combine multiple tsr-files into one
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSSMerger2 -inTsr analysis/merged/drnaseq.tsr analysis/merged/crnaseq.tsr -prefix analysis/merged/TiSS -minScore 1 -gap 10 -ext 10
