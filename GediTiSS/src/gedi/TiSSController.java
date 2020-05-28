package gedi;

import gedi.analyzer.AnalyzeCustom;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Strandness;
import gedi.core.region.GenomicRegionStorage;
import gedi.data.Data;
import gedi.data.DataWrapper;
import gedi.modules.*;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.utils.AnalysisModuleType;
import gedi.utils.TiSSUtils;

import java.util.ArrayList;
import java.util.List;

public class TiSSController extends GediProgram {
    private final char SKIP_CHAR = '_';

    public TiSSController(TiSSParameterSet params) {
        addInput(params.reads);
        addInput(params.genomic);
        addInput(params.wSize);
        addInput(params.iqr);
        addInput(params.minReadDens);
        addInput(params.replicates);
        addInput(params.pseudoCount);
        addInput(params.peakFCThreshold);
        addInput(params.timecourses);
        addInput(params.strandness);
        addInput(params.useAutoparam);
        addInput(params.plotMM);
        addInput(params.analyzeModuleType);
        addInput(params.pValThresh);
        addInput(params.cleanupThresh);

        addInput(params.prefix);

        addOutput(params.outIQR);
        addOutput(params.outTA);
        addOutput(params.outKA);
        addOutput(params.outXRN1);
    }

    @Override
    public String execute(GediProgramContext context) throws Exception {
        List<GenomicRegionStorage<AlignedReadsData>> reads = getParameters(0);
        Genomic genomic = getParameter(1);
        int windowSize = getParameter(2);
        double iqr = getParameter(3);
        double minReadDens = getParameter(4);
        String replicates = getParameter(5);
        double pseudoCount = getParameter(6);
        double peakFCThreshold = getParameter(7);
        String timecourses = getParameter(8);
        Strandness strandness = getParameter(9);
        boolean useMM = getParameter(10);
        boolean plotMM = getParameter(11);
        List<AnalysisModuleType> modTypes = getParameters(12);
        double pValThresh = getParameter(13);
        int cleanupThresh = getParameter(14);

        String prefix = getParameter(15);

        final boolean useMultiCourse = timecourses != null && !timecourses.isEmpty();

        int[][] reps = TiSSUtils.extractReplicatesFromString(replicates, SKIP_CHAR);

        DataWrapper dataWrapper = new DataWrapper(reads, strandness);
        List<Data> data = new ArrayList<>();
        List<Data> singleLanes = new ArrayList<>(reps.length);
        List<Data> multiLanes = new ArrayList<>(reps.length);
        for (int[] rep : reps) {
            if (!useMultiCourse) {
                Data d = new Data(rep, false);
                data.add(d);
                singleLanes.add(d);
                context.getLog().info("Using non-multi data handling.");
            }
            else {
                Data d = new Data(rep, true);
                data.add(d);
                multiLanes.add(d);
                context.getLog().info("Using multi data handling.");
            }
        }
        dataWrapper.initData(genomic, data);

        AnalyzeCustom analyzer = new AnalyzeCustom();

        for (AnalysisModuleType modType : modTypes) {
            switch (modType) {
                case DENSITY:
                    context.getLog().info("Adding Density module");
                    singleLanes.forEach(d -> analyzer.addModule(new TranscriptionalActivity(pValThresh, windowSize, minReadDens, cleanupThresh, useMM, prefix, d, "DENSITY")));
                    break;
                case KINETIC:
                    context.getLog().info("Adding Kinetic module");
                    multiLanes.forEach(d -> analyzer.addModule(new KineticActivity(windowSize, pValThresh, pseudoCount, cleanupThresh, useMM, prefix, d, "KINETIC")));
                    break;
                case SPARSE_PEAK:
                    context.getLog().info("Adding sparse peak module");
                    singleLanes.forEach(d -> analyzer.addModule(new DRnaModule(windowSize, pseudoCount, peakFCThreshold, cleanupThresh, d, useMM, "SPARSE_PEAK")));
                    break;
                case DENSE_PEAK:
                    context.getLog().info("Adding dense peak module");
                    singleLanes.forEach(d -> analyzer.addModule(new CRnaModule(iqr, pseudoCount, windowSize, cleanupThresh, d, useMM, "DENSE_PEAK")));
                    break;
                default:
                    throw new IllegalArgumentException("AnalyzeModuleType is unrecognized: " + modType);
            }
        }

        analyzer.startAnalyzing(dataWrapper, genomic);

        context.getLog().info("Analyzation modules finished");
        context.getLog().info("Writing final file(s)");

        for (AnalysisModuleType modType : modTypes) {
            switch (modType) {
                case DENSITY:
                    if (useMM) {
                        analyzer.calculateMLResults(TranscriptionalActivity.class, prefix, plotMM);
                    }
                    analyzer.writeOutResults(new LineOrientedFile(getOutputFile(1).getPath()), TranscriptionalActivity.class);
                    break;
                case KINETIC:
                    if (useMM) {
                        analyzer.calculateMLResults(KineticActivity.class, prefix, plotMM);
                    }
                    analyzer.writeOutResults(new LineOrientedFile(getOutputFile(2).getPath()), KineticActivity.class);
                    break;
                case SPARSE_PEAK:
                    if (useMM) {
                        analyzer.calculateMLResults(DRnaModule.class, prefix, plotMM);
                    }
                    analyzer.writeOutResults(new LineOrientedFile(getOutputFile(3).getPath()), DRnaModule.class);
                    break;
                case DENSE_PEAK:
                    if (useMM) {
                        analyzer.calculateMLResults(CRnaModule.class, prefix, plotMM);
                    }
                    analyzer.writeOutResults(new LineOrientedFile(getOutputFile(0).getPath()), CRnaModule.class);
                    break;
                default:
                    throw new IllegalArgumentException("AnalysisModuleType is unrecognized: " + modType);
            }
        }

        return null;
    }
}
