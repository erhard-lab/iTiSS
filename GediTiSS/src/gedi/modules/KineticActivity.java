package gedi.modules;

import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.data.Data;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.dynamic.impl.DoubleDynamicObject;
import gedi.util.dynamic.impl.IntDynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.testing.DirichletLikelihoodRatioTest;
import gedi.util.mutable.MutablePair;
import gedi.util.r.RRunner;
import gedi.utils.TiSSUtils;
import gedi.utils.machineLearning.PeakAndPos;

import java.io.IOException;
import java.util.*;

public class KineticActivity extends ModuleBase {
    private int windowSize;
    private double significanceThresh;
    private double pseudoCount;
    private boolean useML;
    private int cleanupThresh;
    private String writerPath;
    private LineWriter mlWriter;

    public KineticActivity(int windowSize, double significanceThresh, double pseudoCount, int cleanupThresh, boolean useML, String prefix, Data lane, String name) {
        super(name, lane);
        this.windowSize = windowSize;
        this.significanceThresh = significanceThresh;
        this.pseudoCount = pseudoCount;
        this.useML = useML;
        this.cleanupThresh = cleanupThresh;
        if (useML) {
            writerPath = prefix + "kineticThresholdData.tsv";
            mlWriter = new LineOrientedFile(writerPath).write();
            try {
                mlWriter.writeLine("Ref\tPos\tValue");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void findTiSS(NumericArray[] data, ReferenceSequence ref) {
        // data contains the timeseries in ascending order
        Map<Integer, Map<String, Double>> foundPeaksNew = this.res.computeIfAbsent(ref, k -> new HashMap<>());
        List<MutablePair<Integer, Double>> posPVal = new ArrayList<>(data[0].length()/100);

        double[] windowSum = sum(data, 0, windowSize);
        if (ref.isMinus()) {
            windowSum = sum(data, windowSize+1, windowSize*2+1);
        }
        for (int i = windowSize; i < data[0].length()-(windowSize+1); i++) {
            if (useML && i%10000000 == 0) {
                System.err.print("Progress " + i + "/" + data[0].length() + ", mmData size: " + posPVal.size() + "\r");
            }
            double[] peaks = new double[windowSum.length];
            for (int j = 0; j < windowSum.length; j++) {
                peaks[j] = data[j].getDouble(i);
            }
            double[] windowMeans = new double[windowSum.length];
            for (int j = 0; j < windowSum.length; j++) {
                windowMeans[j] = windowSum[j]/(windowSize);
            }
            double p = DirichletLikelihoodRatioTest.testMultinomials(pseudoCount, windowMeans, peaks);
            if (useML) {
                if (p < 0.5) {
//                Map<String, Double> info = new HashMap<>();
//                info.put("pValue", p);
//                foundPeaksNew.put(i, info);
                    posPVal.add(new MutablePair<>(i, p));
                }
            } else if (p <= significanceThresh) {
                Map<String,Double> infos = new HashMap<>();
                infos.put("pValue", p);

                foundPeaksNew.put(i, infos);
            }

            for (int j = 0; j < windowSum.length; j++) {
                double subtract = ref.isPlus() ? data[j].getDouble(i-windowSize) : data[j].getDouble(i+1);
                double add = ref.isPlus() ? data[j].getDouble(i) : data[j].getDouble(i+windowSize+1);
                windowSum[j] -= subtract;
                windowSum[j] += add;
            }
        }

        try {
            for (MutablePair<Integer, Double> tss : posPVal) {
                mlWriter.writeLine(ref.toPlusMinusString() + "\t" + tss.Item1 + "\t" + tss.Item2);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double sum(NumericArray ary, int from, int to) {
        double sum = 0.;
        for (int i = from; i < to; i++) {
            sum += ary.getDouble(i);
        }
        return sum;
    }

    private double[] sum(NumericArray[] ary, int from, int to) {
        double[] sum = new double[ary.length];
        for (int i = 0; i < sum.length; i++) {
            sum[i] = sum(ary[i], from, to);
        }
        return sum;
    }

    @Override
    public void calculateMLResults(String prefix, boolean plot) throws IOException {
        mlWriter.close();
        Map<ReferenceSequence, List<MutablePair<Integer, Double>>> refPosPVal = new HashMap<>();
        List<PeakAndPos> mlData = new ArrayList<>();
        EI.lines(writerPath).skip(1).forEachRemaining(l -> {
            String[] split = StringUtils.split(l, "\t");
            mlData.add(new PeakAndPos(Integer.parseInt(split[1]), 1.-Double.parseDouble(split[2])));
            refPosPVal.computeIfAbsent(Chromosome.obtain(split[0]), absent->new ArrayList<>()).add(new MutablePair<>(Integer.parseInt(split[1]), Double.parseDouble(split[2])));
        });
        int movingAverage = (int)(mlData.size()*0.2);
        double upThresh = TiSSUtils.calculateThreshold(mlData, movingAverage);
        upThresh = 1.-upThresh;

        for (ReferenceSequence ref : refPosPVal.keySet()) {
            Map<Integer, Map<String,Double>> filtered = new HashMap<>();
            List<MutablePair<Integer, Double>> tss2val = refPosPVal.get(ref);
            if (cleanupThresh > 0) {
                tss2val = TiSSUtils.cleanUpMultiValueDataPair(tss2val, cleanupThresh);
            }

            for (MutablePair<Integer, Double> tss : tss2val) {
                if (tss.Item2 < upThresh) {
                    Map<String, Double> info = new HashMap<>();
                    info.put("pValue", tss.Item2);
                    filtered.put(tss.Item1, info);
                }
            }
            res.put(ref, filtered);
        }

        if (plot) {
            plotData(prefix, prefix + "kineticThresholdData.tsv", upThresh);
        }
    }

    private void plotData(String prefix, String dataFilePath, double thresh) throws IOException {
        RRunner r = new RRunner(prefix+".plotKineticThresholdData.R");
        r.set("dataFile",dataFilePath);
        r.set("pdfFile",prefix+".kineticThresholds.pdf");
        r.set("customThresh", new DoubleDynamicObject(thresh));
        r.set("threshAutoParam", new DoubleDynamicObject(thresh));
        r.set("manualSelectionFile", prefix + "kineticManualSelection.tsv");
        r.set("manualSelectionPdf", prefix + "kineticManuelThresh.pdf");
        r.addSource(getClass().getResourceAsStream("/resources/plotPValues.R"));
        r.run(false);
    }
}
