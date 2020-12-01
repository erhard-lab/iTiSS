iTiSS
=================
iTiSS (integrated Transcriptional start site caller) is a method to identify transcriptional start sites (TiSS) from various TiSS-profiling experiments with an additional integrative module to combine and remove artefactual TiSS called in single data sets.

## Table of contents

* [Prerequisites](#prerequisites)
* [Installation using the binaries (recommended)](#installation-binary)
* [Usage](#usage)
* [Results](#results)
   * [SPARSE_PEAK](#sparse_peak)
   * [DENSE_PEAK](#dense_peak)
   * [DENSITY](#density)
   * [KINETIC](#kinetic)
* [Merging TiSS into TSRs](#merging-tiss)
* [Citation](#citation)
* [Testdata](#testdata)
* [Installation from source (for advanced users only)](#installation-from-source)

## Prerequisites:

- Java ≥ 1.8
- R with RServe (optional, if plotting of thresholds is needed)

## Installation using the binaries (recommended)

- Download the .jar-file of the newest [iTiSS release](https://github.com/erhard-lab/iTiSS/releases)
- That's it. If a Java runtime environment is already installed, you shoud be presented with the help-desk when executing `java -cp iTiSS.jar -hh` on the command line.

## Usage

iTiSS was designed to work on the internal data format of the `gedi`-tookit called `CIT` as well as an indexed genome in `.oml`-format. However, it contains a conversion module, exporting `BAM`-files into the `CIT`-format and creating the indexed genome on the fly.

`BAM`-files are specified with the `-bams` option. Multiple BAM-files can be supplied, which will subsequentially be totalized and converted into a `CIT`-file.

Reference genomes need to be provided in a simple two column tab-separated file containing the chromosome names in the first column (which need to correspond to the actual chromosome names in the `BAM`-file) and their respective lengths in the second. This file is then provided to iTiSS with the `-chromSizes` option.

Such a reference genome sample file could look like this for the first 3 chromosomes as well as the X and Y chromosome of the human genome:

```
1	248956422
2	242193529
3	198295559
X	156040895
Y	57227415
```

An example for running iTiSS' SPARSE_PEAK detection on two bam files:
```
java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -bams bam1.bam bam2.bam -chromSizes chromSizes.tsv -rep XX -modType SPARSE_PEAK -autoParam
```

The `-rep` option determines which files or conditions to test. If `BAM`-files are provided, simply type as many `X`s as there are `BAM`-files. If you are using the `CIT`-format, the index of each `X` indicates which conditions of the `CIT`-file to use, whereas the `_`-character determines the conditions to skip.

The `-modType` option determines the algorithm to be used on the data (see our [paper below](#citation) for in-depth description of each algorithm). The following modes are supported: `SPARSE_PEAK `, `DENSE_PEAK`, `DENSITY` and `KINETIC`.

`SPARSE_PEAK` mode should be used on data, where the signal-to-noise ratio is very high (such is the case for dRNA-seq, PROcap-seq and RAMPAGE data).

`DENSE_PEAK` mode should be used on data, where reads not only map at the respecitve 5'-end of their transcripts, but are only enriched at it with additional reads mapping throughout the whole transcript's body (such is the case for CAGE, cRNA-seq and STRIPE-seq data).

The `DENSITY` and `KINETIC` mode shoud only be used in conjunction (using the [TiSSMerger](#merging-tiss) sub-program) to the `DENSE_PEAK` mode to leverage additional information of such data sets, if the signal-to-noise ratios are very low.

The `DENSITY` mode compares read density changes upstream compared to downstream along the reference genome. Consequently, if, for example the `DENSE_PEAK` mode identified a peak, which did not meet the threshold, but the read density cleary increases downstream of it, the chances of it beeing a true-positive are high.

The `KINETIC` mode compares the local fold-change changes across multiple timepoints. Consequently, this mode is only usable in timecourse experiments. Like the `DENSITY` mode it should ony be used in conjunction to the `DENSE_PEAK` mode using [TiSSMerger](#merging-tiss). The `KINETIC` mode detects if a peak changes in expression strengths throughout the course of infection. If that is the case, the chances of it beeing a true-positive are high.

If the `-autoParam` option is set, iTiSS will determin thresholds on its own based on the data. The set thresholds can afterwards be seen in `[prefix].plot[modType]ThresholdData.R`. Here, the threshold can further be lowered or increased by running the priveded Rscript in the results-folder. An additional file will be created with TiSS beeing filtered with the user-defined threshold. This is useful in integrative studies, where more TiSS should be called on a per-sample bases, as false-positives are filtered out later by merging multiple samples. If in that case iTiSS sets the threshold too high, TiSS can quickly be re-filtered with a user-defined lower threshold.

Run the command `java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSS -hhh` to see the following help message explaining each of the other parameters that can be set.

```
gedi -e TiSS <Options>

General:
 -prefix <prefix>               The prefix used for all output files
[ -bams <bams>                  Whitespace separated list of bam-files]

GenomicCreate:
[ -chromSizes <chromSizes>      tsv file containing the sizes of each chromosome]

TiSSController:
 -modType <modType>             The type of analyzis [DENSITY, KINETIC, DENSE_PEAK, SPARSE_PEAK] (default: KINETIC) Choices are : KINETIC,DENSITY,SPARSE_PEAK,DENSE_PEAK
[ -reads <reads>                Read data in CIT-format.]
[ -genomic <genomic>            The indexed GEDI genome.]
[ -windowSize <windowSize>      The size of the moving window (default: 100)]
[ -zscore <zscore>              z-score threshold to call a TiSS (not used if -autoparam is set) (default: 5.0)]
[ -minReadDens <minReadDens>    The minimum read density to look for (default: 0.0)]
[ -rep <rep>                    A string to identify samples to combine (for ex.: XX_X -> combines read counts from sample 0, 1 and 3 with 2 being ignored) underscore character (_) for skip]
[ -pseudo <pseudo>              A pseudo count be added to each position (default: 1.0)]
[ -peakFC <peakFC>              The fold-change threshold for sparse peak algorithm (not used if -autoparam is set) (default: 4.0)]
[ -timepoints <timepoints>      A string to identify conditions (for ex.: 12_3 -> 3 conditions at index 0, 1 and 3, with 2 being ignored) underscore character (_) for skip (default: )]
[ -strandness <strandness>      Which strandness. (default: Sense) Choices are : AutoDetect,Sense,Antisense,Unspecific]
[ -autoparam                    automatically set thresholds based on the data]
[ -plotParams                   plot thresholds]
[ -pVal <pVal>                  p-Value threshold to call a TiSS (not used if -autoparam is set) (default: 5.0)]
```

## Results
Running iTiSS will create a folder at the following location `[working directory][prefix]`, where prefix is supplied to iTiSS with the `-prefix` option. For example `-prefix test/out` will create a folder named `test` in the current working directory and add the prefix `out` to all files generated in this run.

In the following, the most interesting files of each module will be explained:

### SPARSE_PEAK
- `[prefix].sparsePeak.tsv`: The final called TiSS
- `[prefix].sparsePeakThresholds.pdf`: A visualization of all colected points and the set threshold
- `[prefix]BAM2CIT_convertedReads.cit`: If BAMs were provided, this is the CIT files they were converted into. If further analysis is necessary, using this file is recomended
- `[prefix].plotSparsePeakThresholdData.R`: The Rscript file used to create the plot. Here, custom thresholds can be set, to re-filter selected positions if needed.
- `[prefix]sparsePeakThresholdData.tsv`: All selected positions with their respective z-scores (see paper)

### DENSE_PEAK
- `[prefix].sparsePeak.tsv`: The final called TiSS
- `[prefix].sparsePeakThresholds.pdf`: A visualization of all colected points and the set threshold
- `[prefix]BAM2CIT_convertedReads.cit`: If BAMs were provided, this is the CIT files they were converted into. If further analysis is necessary, using this file is recomended
- `[prefix].plotSparsePeakThresholdData.R`: The Rscript file used to create the plot. Here, custom thresholds can be set, to re-filter selected positions if needed.
- `[prefix]sparsePeakThresholdData.tsv`: All selected positions with their respective fold-changes (see paper)

### DENSITY
- `[prefix].density.tsv`: The final called TiSS
- `[prefix].densityThresholds.pdf`: A visualization of all colected points and the set threshold
- `[prefix]BAM2CIT_convertedReads.cit`: If BAMs were provided, this is the CIT files they were converted into. If further analysis is necessary, using this file is recomended
- `[prefix].plotDensityThresholdData.R`: The Rscript file used to create the plot. Here, custom thresholds can be set, to re-filter selected positions if needed.
- `[prefix]densityThresholdData.tsv`: All selected positions with their respective p-values (see paper)

### KINETIC
- `[prefix].kinetic.tsv`: The final called TiSS
- `[prefix].kineticThresholds.pdf`: A visualization of all colected points and the set threshold
- `[prefix]BAM2CIT_convertedReads.cit`: If BAMs were provided, this is the CIT files they were converted into. If further analysis is necessary, using this file is recomended
- `[prefix].kineticThresholdData.R`: The Rscript file used to create the plot. Here, custom thresholds can be set, to re-filter selected positions if needed.
- `[prefix]kineticThresholdData.tsv`: All selected positions with their respective p-values (see paper)

## Merging TiSS into TSRs
Merging of multiple TiSS files is done by the `TiSSMerger` sub-program of iTiSS. Here, a user-specified gap-range is used to merge TiSS in close proximity into transcription start site regions (TSRs). An example command to run iTiSS on three separate TiSS files obtained by running iTiSS on three different TiSS-profiling datasets could look like this: `java -cp iTiSS.jar -Xmx12g -Xms4g executables/TiSSMerger -in path/to/TiSSdata1.tsv path/to/TiSSdata2.tsv path/to/TiSSdata3.tsv -dep "0:1:2>3x2" -prefix out/path`

- `-in` option needs the paths to all TiSS-files
- `-gap` option defines the range in which two TiSS are combined into a single TSR. 
- `-dep` option determines how the files should be merged. The numbers indicate the indices of the provided file. 0 is the first, 1 the second, etc. With the `:` character, multiple files can be combined, as it is the case here for all three files. The `>` character determines the created file id, in which the files left of it are combined into. Here, the files at indices 0, 1 and 2 are merged into a new file with ID 3. The number after the `x` determines the number of files a TiSS needs to be confirmed by. For example, if the TiSS at position 10 was identified in file 0 and 2, and the TiSS at position 20 was identified only in file 2, the final file would only contain the TiSS at position 10 in this case.

The `-dep` option offers even more complexity in combining multiple datasets. Here are more examples of combining various datasets, where `f1`, `f2`, ... are the final TiSS-files from iTiSS produced by any mode:

- `-in f1 f2 f3 -dep "0:1>3x2,2:3>4x2"` All TiSS found in either file `f1` or `f2` and `f3`. Boolean form: `(f1 || f2) && f3`
- `-in f1 f2 f3 f4 -dep "0:1>4x2,2:3>5x2,4:5>6x1"` All TiSS found in either file `f1` and `f2` or in file `f3` and `f4`. Boolean form: `(f1 && f2) || (f3 && f4)`

## Citation

If you use iTiSS, please cite the following paper:

## Testdata

Testdata including example commands on how to run iTiSS on different TiSS-profiling datasets can be found on [Zenodo](https://doi.org/10.5281/zenodo.3860525). This testdata includes mapped reads from six different TSS-profiling data sets (dRNA-seq, cRNA-seq, PROcap-seq, CAGE, RAMPAGE, STRIPE-seq), which can be found in the `mapping` folder. iTiSS can be found in the `programs/iTiSS` folder. The `start.bash` file in the same folder contains suggested executions for calling iTiSS to predict TiSS in the respective data sets.

## Installation from source (for advanced users only)
iTiSS is a submodule of the gedi toolkit (full gedi source: https://github.com/erhard-lab/gedi)

- Clone or download the gedi toolkit and this repository
- Add this repository as a module to the gedi toolkit
- If you only want to use iTiSS, the following modules and dependencies are all you need:
![needed modules](https://github.com/Chrizey91/GediTiSS/blob/master/imgs/modulesNeeded.png "Modules needed from gedi")
![needed modules](https://github.com/Chrizey91/GediTiSS/blob/master/imgs/Dependencies.png "Modules needed from gedi")
- Alter the Test and Output paths so that the executables are generated in the bin folder of each module. Example for cglib:
![needed modules](https://github.com/Chrizey91/GediTiSS/blob/master/imgs/OutputPathsNew.png "Modules needed from gedi")
- Build the whole toolkit
- add the `gedi`-bash file to you PATH

Now you can run iTiSS from the commandline by using `gedi -e TiSS [options]`
