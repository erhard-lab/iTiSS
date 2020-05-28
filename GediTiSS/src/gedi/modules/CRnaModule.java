package gedi.modules;

import gedi.core.reference.ReferenceSequence;
import gedi.data.Data;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.dynamic.impl.DoubleDynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.mutable.MutablePair;
import gedi.util.r.RRunner;
import gedi.utils.TiSSUtils;
import gedi.utils.machineLearning.PeakAndPos;
import gedi.utils.sortedNodesList.StaticSizeSortedArrayList;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.IOException;
import java.util.*;

public class CRnaModule extends ModuleBase{

    private double zScoreThresh;
    private int windowSize;
    private boolean useMM;
    private double pseudoCount;
    private int cleanupThresh;

    public CRnaModule(double zScoreThresh, double pseudoCount, int windowSize, int cleanupThresh, Data lane,
                      boolean useMM, String name) {
        super(name, lane);
        this.zScoreThresh = zScoreThresh;
        this.windowSize = windowSize;
        this.pseudoCount = pseudoCount;
        this.useMM = useMM;
        this.cleanupThresh = cleanupThresh;
    }

    @Override
    public void findTiSS(NumericArray[] inData, ReferenceSequence ref) {
        // Should only be one lane, the total lane
        NumericArray data = inData[0];

        List<PeakAndPos> mmDataL = useMM ? new ArrayList<>() : null;
        List<PeakAndPos> mmDataR = useMM ? new ArrayList<>() : null;

        double[] upstream = data.toDoubleArray(0, windowSize);
        double[] downstream = data.toDoubleArray(windowSize+1, windowSize*2+1);
        for (int i = 0; i < windowSize; i++) {
            upstream[i] = Math.log(upstream[i] + pseudoCount);
            downstream[i] = Math.log(downstream[i] + pseudoCount);
        }

        StaticSizeSortedArrayList<Double> windowUpstream = new StaticSizeSortedArrayList<>(EI.wrap(upstream).toList(), Double::compareTo);
        StaticSizeSortedArrayList<Double> windowDownstream = new StaticSizeSortedArrayList<>(EI.wrap(downstream).toList(), Double::compareTo);

        if (ref.isMinus()) {
            StaticSizeSortedArrayList<Double> windowTmp = windowUpstream;
            windowUpstream = windowDownstream;
            windowDownstream = windowTmp;
        }

        Map<Integer, Map<String, Double>> foundPeaksNew = this.res.computeIfAbsent(ref, k -> new HashMap<>());
        List<Double> mmDatOut = this.allMmData.computeIfAbsent(ref, k-> new ArrayList<>());

        double sumUpstream = sum(windowUpstream);
        double sumDownstream = sum(windowDownstream);
        double meanUpstream = sumUpstream/(windowUpstream.getSize()-1);
        double meanDownstream = sumDownstream/(windowDownstream.getSize()-1);
        double sdUpsteam = sampleSd(windowUpstream,sumUpstream/windowUpstream.getSize());
        double sdDownsteam = sampleSd(windowDownstream, sumDownstream/windowDownstream.getSize());

        for (int i = windowSize; i < data.length() - (windowSize+1); i++) {
            if (mmDataL != null && i%10000000 == 0) {
                //System.err.print("Progress " + i + "/" + data.length() + ", mmDataL size: " + mmDataL.size() + ", mmDataR size: " + mmDataR.size() + ", upstream: " + sumUpstream + ", downstream: " + sumDownstream + "\r");
                System.err.print(String.format("Progress %d / %d, mmDataL size: %d, mmDataR size: %d, upstream: %.2f, downstream: %.2f \r", i, data.length(), mmDataL.size(), mmDataR.size(), sumUpstream, sumDownstream));
            }
            double valueOfInterest = Math.log(data.getDouble(i) + pseudoCount);
            if (sdUpsteam <= pseudoCount) {
                sdUpsteam = pseudoCount;
            }
            if (sdDownsteam <= pseudoCount) {
                sdDownsteam = pseudoCount;
            }

            NormalDistribution normUpstream = new NormalDistribution(null, meanUpstream, sdUpsteam);
            NormalDistribution normDownstream = new NormalDistribution(null, meanDownstream, sdDownsteam);

            double upstreamZ = (valueOfInterest - meanUpstream) / sdUpsteam;
            double downstreamZ = (valueOfInterest - meanDownstream) / sdDownsteam;
            if (useMM) {
//                if (pUpstream < 0.01 && pDownstream < 0.01) {
                if (upstreamZ > 2.5 && downstreamZ > 2.5) {
                    mmDataL.add(new PeakAndPos(i, upstreamZ));
                    mmDataR.add(new PeakAndPos(i, downstreamZ));
                }
            } else {
//                if (pUpstream < zScoreThresh && pDownstream < zScoreThresh) {
                if (upstreamZ > zScoreThresh && downstreamZ > zScoreThresh) {
                    Map<String, Double> infos = new HashMap<>();
                    infos.put("zScore-US", upstreamZ);
                    infos.put("zScore-DS", downstreamZ);
                    foundPeaksNew.put(i, infos);
                }
            }

            if (ref.isPlus()) {
                windowUpstream.insertSortedAndDelete(Math.log(data.getDouble(i) + pseudoCount), Math.log(data.getDouble(i - windowSize) + pseudoCount));
                windowDownstream.insertSortedAndDelete(Math.log(data.getDouble(i + windowSize + 1) + pseudoCount), Math.log(data.getDouble(i + 1) + pseudoCount));
            } else {
                windowDownstream.insertSortedAndDelete(Math.log(data.getDouble(i) + pseudoCount), Math.log(data.getDouble(i - windowSize) + pseudoCount));
                windowUpstream.insertSortedAndDelete(Math.log(data.getDouble(i + windowSize + 1) + pseudoCount), Math.log(data.getDouble(i + 1) + pseudoCount));
            }

            sumUpstream = (sumUpstream - Math.log(data.getDouble(i-windowSize)+pseudoCount)) + Math.log(data.getDouble(i)+pseudoCount);
            sumDownstream = (sumDownstream - Math.log(data.getDouble(i+1)+pseudoCount)) + Math.log(data.getDouble(i+windowSize+1)+pseudoCount);
            meanUpstream = sumUpstream/(windowUpstream.getSize()-1);
            meanDownstream = sumDownstream/(windowDownstream.getSize()-1);
            sdUpsteam = sampleSd(windowUpstream,sumUpstream/windowUpstream.getSize());
            sdDownsteam = sampleSd(windowDownstream, sumDownstream/windowDownstream.getSize());
        }

        if (mmDataL != null) {
//            mmDatOut.addAll(mmDataC.stream().map(PeakAndPos::getValue).collect(Collectors.toList()));
            for (int i = 0; i < mmDataL.size(); i++) {
                Map<String, Double> info = new HashMap<>();
                info.put("zScore-US", mmDataL.get(i).getValue());
                info.put("zScore-DS", mmDataR.get(i).getValue());
                foundPeaksNew.put(mmDataL.get(i).getPos(), info);
            }
        }

//        System.err.println("Finished searching for peaks in the DENSE_PEAK module.");
//        System.err.println("Found peaks: " + foundPeaks.size());
    }

    private double sum(StaticSizeSortedArrayList<Double> lst) {
        double sum = 0;
        for (int i = 0; i < lst.getSize(); i++) {
            sum += lst.getValueAtIndex(i);
        }
        return sum;
    }

    private double sampleSd(StaticSizeSortedArrayList<Double> lst, double mean) {
        double sum = 0;
        for (int i = 0; i < lst.getSize(); i++) {
            sum += Math.pow(lst.getValueAtIndex(i) - mean, 2);
        }
        return Math.sqrt(sum/(lst.getSize()-1));
    }

    @Override
    public void calculateMLResults(String prefix, boolean plot) throws IOException {
        LineWriter writerup = new LineOrientedFile(prefix + "densePeakThresholdData.tsv").write();
        writerup.writeLine("Ref\tPos\tValue1\tValue2");

        List<PeakAndPos> mlDataUp = new ArrayList<>();
        List<PeakAndPos> mlDataDown = new ArrayList<>();
        for (ReferenceSequence ref : res.keySet()) {
            Map<Integer, Map<String, Double>> tss = res.get(ref);
            for (int tssPos : tss.keySet()) {
                Map<String, Double> info = tss.get(tssPos);
                mlDataUp.add(new PeakAndPos(tssPos, info.get("zScore-US")));
                mlDataDown.add(new PeakAndPos(tssPos, info.get("zScore-DS")));
                writerup.writeLine(ref.toPlusMinusString() + "\t" + tssPos + "\t" + info.get("zScore-US") + "\t" + info.get("zScore-DS"));
            }
        }
        writerup.close();
        int movingAverageUp = (int)(mlDataUp.size()*0.2);
        int movingAverageDown = (int)(mlDataUp.size()*0.2);
        double upThresh = TiSSUtils.calculateThreshold(mlDataUp, movingAverageUp);
        double downThresh = TiSSUtils.calculateThreshold(mlDataDown, movingAverageDown);

        for (ReferenceSequence ref : res.keySet()) {
            Map<Integer, Map<String,Double>> filtered = new HashMap<>();
            Map<Integer, Map<String, Double>> tss2val = res.get(ref);
            if (cleanupThresh > 0) {
                List<MutablePair<Integer, Double>> upstream = EI.wrap(res.get(ref).keySet()).map(t -> new MutablePair<>(t, res.get(ref).get(t).get("zScore-US"))).list();
                List<MutablePair<Integer, Double>> downstream = EI.wrap(res.get(ref).keySet()).map(t -> new MutablePair<>(t, res.get(ref).get(t).get("zScore-DS"))).list();
                upstream = TiSSUtils.cleanUpMultiValueDataPair(upstream, cleanupThresh);
                downstream = TiSSUtils.cleanUpMultiValueDataPair(downstream, cleanupThresh);
                Map<Integer,Map<String,Double>> tss2valFiltered = new HashMap<>();
                EI.wrap(upstream).forEachRemaining(u -> tss2valFiltered.computeIfAbsent(u.Item1, absent->new HashMap<>()).put("zScore-US", u.Item2));
                EI.wrap(downstream).forEachRemaining(u -> tss2valFiltered.computeIfAbsent(u.Item1, absent->new HashMap<>()).put("zScore-DS", u.Item2));
                tss2val = tss2valFiltered;
            }
            for (int tss : tss2val.keySet()) {
                if (tss2val.get(tss).containsKey("zScore-US") && tss2val.get(tss).containsKey("zScore-DS") && tss2val.get(tss).get("zScore-US") > upThresh && tss2val.get(tss).get("zScore-DS") > downThresh) {
                    filtered.put(tss, tss2val.get(tss));
                }
            }
            res.put(ref, filtered);
        }

        if (plot) {
            plotData(prefix, prefix + "densePeakThresholdData.tsv", upThresh, downThresh);
        }
    }

    private void plotData(String prefix, String dataFilePath, double threshUp, double threshDown) throws IOException {
        RRunner r = new RRunner(prefix+".plotDensePeakThresholdData.R");
        r.set("dataFile",dataFilePath);
        r.set("pdfFile",prefix+".densePeakThresholds.pdf");
        r.set("customThreshUp", new DoubleDynamicObject(threshUp));
        r.set("customThreshDown", new DoubleDynamicObject(threshDown));
        r.set("upThreshAutoParam", new DoubleDynamicObject(threshUp));
        r.set("downThreshAutoParam", new DoubleDynamicObject(threshDown));
        r.set("manualSelectionFile", prefix + "densePeakManualSelection.tsv");
        r.set("manualSelectionPdf", prefix + "densePeakManuelThresh.pdf");
        r.addSource(getClass().getResourceAsStream("/resources/plotDenseThresh.R"));
        r.run(false);
    }
}
