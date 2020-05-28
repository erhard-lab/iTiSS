package gedi.utils;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strandness;
import gedi.core.region.*;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.merger.Tiss;
import gedi.merger.Tsr;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.EI;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.mutable.MutablePair;
import gedi.util.mutable.MutableTriple;
import gedi.utils.loader.TsrData;
import gedi.utils.machineLearning.PeakAndPos;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TiSSUtils {

    /**
     * Returns a 2D-array containing the indices of each replicate in each row with the possibility
     * to skip certain positions by the {@code skip} character
     * Example:
     * Input: xoxo111222
     * Output: [[0,2], [1,3], [4,5,6], [7,8,9]]
     * @param replicatesString input string
     * @param skip a character indicating positions to skip. \u0000 (the null character) for no skip
     * @return 2D-array with indices of replicates in each row
     */
    public static int[][] extractReplicatesFromString(String replicatesString, char skip) {
        String uniqueReps = StringUtils2.uniqueCharacters(StringUtils.remove(replicatesString, skip));
        int[][] repAry = new int[uniqueReps.length()][];
        for (int i = 0; i < uniqueReps.length(); i++) {
            char current = uniqueReps.charAt(i);
            repAry[i] = StringUtils2.indicesOfChar(replicatesString, current);
        }
        return repAry;
    }

    public static int[][] extractReplicatesFromString(String replicatesString) {
        return extractReplicatesFromString(replicatesString, Character.MIN_VALUE);
    }

    /**
     * Returns a 2D-array containing the indices of each replicate in each row.
     * Replicates are given by {@code reps}.
     * Each row contains the a timepoint indicated by the given characters in {@code timecourseString}.
     * They are ordered by {@code Character}-ordering.
     * @param timecourseString input string
     * @param reps replicate string
     * @param skip a character indicating positions to skip. \u0000 (the null character) for no skip
     * @return 2D-array with indices of replicates in each row
     */
    public static int[][] extractTimecoursesFromString(String timecourseString, int[][] reps, char skip) {
        int[][] tcAry = new int[reps.length][];
        for (int i = 0; i < tcAry.length; i++) {
            String rep = new String(StringUtils2.extract(timecourseString, reps[i]));
            int[] tc = extractSingleTimecourse(rep, skip);
            for (int j = 0; j < tc.length; j++) {
                tc[j] = reps[i][tc[j]];
            }
            tcAry[i] = tc;
        }
        return tcAry;
    }

    private static int[] extractSingleTimecourse(String timecourseString, char skip) {
        if (!hasOnlyUniqueCharacters(StringUtils.remove(timecourseString, skip))) {
            System.out.println("Only unique characters are allowed inside a single replicate for the time points");
            return null;
        }
        String uniqueReps = StringUtils2.uniqueCharacters(StringUtils.remove(timecourseString, skip));
        List<Character> timepoints = new ArrayList<>();
        for (char c : uniqueReps.toCharArray()) {
            timepoints.add(c);
        }
        timepoints.sort(Character::compareTo);
        int[] tcAry = new int[timepoints.size()];

        for (int i = 0; i < tcAry.length; i++) {
            tcAry[i] = StringUtils2.indexOf(timecourseString, timepoints.get(i));
        }

        return tcAry;
    }

    private static boolean hasOnlyUniqueCharacters(String string) {
        return StringUtils2.uniqueCharacters(string).length() == string.length();
    }

    public static int[][] extractTimecoursesFromString(String timecourseString, int[][] reps) {
        return extractTimecoursesFromString(timecourseString, reps, Character.MIN_VALUE);
    }

    public static List<List<Integer>> groupConcurrents(List<Integer> lst, int gap) {
        List<List<Integer>> out = new ArrayList<>();
        if (lst.size() <= 1) {
            out.add(new ArrayList<>(lst));
            return out;
        }

        List<Integer> cpy = new ArrayList<>(lst);
        cpy.sort(Double::compare);
        List<Integer> concurrents = new ArrayList<>();
        int last = cpy.get(0);
        concurrents.add(last);
        for (int i = 1; i < cpy.size(); i++) {
            if (cpy.get(i) - last <= gap) {
            } else {
                out.add(concurrents);
                concurrents = new ArrayList<>();
            }
            last = cpy.get(i);
            concurrents.add(last);
        }
        out.add(concurrents);
        return out;
    }

    public static Map<ReferenceSequence, List<Integer>> extractTissFromFile(String path, int skip) throws IOException {
        Map<ReferenceSequence, List<Integer>> out = new HashMap<>();
        EI.lines(path).skip(skip).forEachRemaining(l -> {
            String[] split = StringUtils.split(l, "\t");
            ReferenceSequence ref = Chromosome.obtain(split[0]);
            int tiss = Integer.parseInt(split[1]);
            if (!out.containsKey(ref)) {
                out.put(ref, new ArrayList<>());
            }
            out.get(ref).add(tiss);
        });
        return out;
    }

    public static Map<ReferenceSequence, List<List<Integer>>> extractTissFromFileGrouped(String path, int skip, int gap) throws IOException {
        Map<ReferenceSequence, List<Integer>> tiss = extractTissFromFile(path, skip);
        Map<ReferenceSequence, List<List<Integer>>> out = new HashMap<>();
        for (ReferenceSequence k : tiss.keySet()) {
            List<Integer> tissList = tiss.computeIfAbsent(k, t -> new ArrayList<>());
            out.put(k, groupConcurrents(tissList, gap));
        }
        return out;
    }

    public static Map<ReferenceSequence, List<Tsr>> extractTsrsFromFile(String path, int skip, int gap, int id) throws IOException {
        Map<ReferenceSequence, List<Tsr>> tsrMap = new HashMap<>();
        Map<ReferenceSequence, List<List<Integer>>> tissGrouped = extractTissFromFileGrouped(path, skip, gap);
        for(ReferenceSequence ref : tissGrouped.keySet()) {
            List<Tsr> tsrList = EI.wrap(tissGrouped.get(ref)).map(tissGroup -> {
                Tsr tsr = new Tsr(createTiss(id, tissGroup.get(0)));
                for (int i = 1; i < tissGroup.size(); i++) {
                    Set<Integer> calledBy = new HashSet<>();
                    calledBy.add(id);
                    tsr.add(new Tiss(calledBy, tissGroup.get(i)));
                }
                return tsr;
            }).list();
            tsrMap.put(ref, tsrList);
        }
        return tsrMap;
    }

    public static Map<ReferenceSequence, List<Tsr>> extractTsrsFromFinalFile(String path, int skip) throws IOException {
        Map<ReferenceSequence, Map<GenomicRegion, Tsr>> tsrMap = new HashMap<>();
        EI.lines(path).skip(skip).forEachRemaining(l -> {
            String[] split = StringUtils.split(l, "\t");
            Map<GenomicRegion, Tsr> reg2tsr = tsrMap.computeIfAbsent(Chromosome.obtain(split[0]), empty -> new HashMap<>());
            GenomicRegion reg = GenomicRegion.parse(split[2]);
            Set<Integer> calledBy = new HashSet<>();
            for (int i = 3; i < split.length-2; i++) {
                int called = Integer.parseInt(split[i]);
                if (called == 1) {
                    calledBy.add(i-3);
                } else if (called == 0) {
                    // Not called
                } else {
                    throw new IllegalStateException();
                }
            }
            Tiss tiss = new Tiss(calledBy, Integer.parseInt(split[1]));
            Tsr tsr = reg2tsr.get(reg);
            if (tsr == null) {
                tsr = new Tsr(tiss);
            } else {
                tsr.add(tiss);
            }
            reg2tsr.put(reg, tsr);
        });
        Map<ReferenceSequence, List<Tsr>> outMap = new HashMap<>();
        EI.wrap(tsrMap.keySet()).forEachRemaining(ref -> {
            EI.wrap(tsrMap.get(ref).keySet()).forEachRemaining(reg -> {
                List<Tsr> tsrList = outMap.computeIfAbsent(ref, empty -> new ArrayList<>());
                tsrList.add(tsrMap.get(ref).get(reg));
            });
        });
        return outMap;
    }

    // NOTE: using this will loose some information as the TSR file does not contain information about
    // single Tiss. In this case, use the final TiSS file and the following function "extractTsrsFromFinalFile(...)"
    public static Map<ReferenceSequence, List<Tsr>> extractTsrsFromFinalTsrFile(String path, int skip) throws IOException {
        Map<ReferenceSequence, List<Tsr>> tsrMap = new HashMap<>();
        EI.lines(path).skip(skip).forEachRemaining(l -> {
            String[] split = StringUtils.split(l, "\t");
            List<Tsr> tsrList = tsrMap.computeIfAbsent(Chromosome.obtain(split[0]), empty -> new ArrayList<>());
            GenomicRegion reg = GenomicRegion.parse(split[2]);
            Tsr tsr = new Tsr(reg);
            Set<Integer> calledBy = new HashSet<>();
            for (int i = 3; i < split.length-2; i++) {
                int called = Integer.parseInt(split[i]);
                if (called == 1) {
                    calledBy.add(i-3);
                } else if (called == 0) {
                    // Not called
                } else {
                    throw new IllegalStateException();
                }
            }
            Tiss tiss = new Tiss(calledBy, Integer.parseInt(split[1]));
            tsr.add(tiss);
            tsrList.add(tsr);
        });
        return tsrMap;
    }

    // ========
    // Those two functions are the latest version. use them!
    // ========

    // NOTE: using this will loose some information as the TSR file does not contain information about
    // single Tiss. In this case, use the final TiSS file and the following function "extractTsrsFromFinalFile(...)"
    public static MemoryIntervalTreeStorage<TsrData> loadTsrsFromFinalTsrFile(String path, int skip) throws IOException {
        MemoryIntervalTreeStorage<TsrData> storage = new MemoryIntervalTreeStorage<>(TsrData.class);
        List<ImmutableReferenceGenomicRegion<TsrData>> entries = new ArrayList<>();
        EI.lines(path).skip(skip).forEachRemaining(l -> {
            String[] split = StringUtils.split(l, "\t");
            ReferenceSequence ref = Chromosome.obtain(split[0]);
            GenomicRegion reg = GenomicRegion.parse(split[2]);
            Set<Integer> calledBy = new HashSet<>();
            for (int i = 3; i < split.length-2; i++) {
                int called = Integer.parseInt(split[i]);
                if (called == 1) {
                    calledBy.add(i-3);
                } else if (called == 0) {
                    // Not called
                } else {
                    throw new IllegalStateException();
                }
            }
            int maxTissPos = Integer.parseInt(split[1]);
            entries.add(new ImmutableReferenceGenomicRegion<>(ref, reg, new TsrData(calledBy.size(), maxTissPos, calledBy)));
        });
        storage.fill(EI.wrap(entries));
        return storage;
    }

    // TsrData will not have the MaxTiSS set!!!
    public static MemoryIntervalTreeStorage<TsrData> loadTsrsFromFinalTissFile(String path, int skip) throws IOException {
        MemoryIntervalTreeStorage<TsrData> storage = new MemoryIntervalTreeStorage<>(TsrData.class);
        Map<ReferenceSequence, Map<GenomicRegion, TsrData>> tsrmap = new HashMap<>();
        EI.lines(path).skip(skip).forEachRemaining(l -> {
            String[] split = StringUtils.split(l, "\t");
            ReferenceSequence ref = Chromosome.obtain(split[0]);
            Map<GenomicRegion, TsrData> regMap = tsrmap.computeIfAbsent(ref, empty -> new HashMap<>());
            GenomicRegion reg = GenomicRegion.parse(split[2]);
            TsrData td = regMap.computeIfAbsent(reg, empty -> new TsrData());
            Set<Integer> calledBy = td.getCalledBy();
            Set<Integer> tissPos = td.getAllTissPos();
            td.setScore(Integer.parseInt(split[split.length-1]));
            for (int i = 3; i < split.length-2; i++) {
                int called = Integer.parseInt(split[i]);
                if (called == 1) {
                    calledBy.add(i-3);
                } else if (called == 0) {
                    // Not called
                } else {
                    throw new IllegalStateException();
                }
            }
            tissPos.add(Integer.parseInt(split[1]));
        });
        List<ImmutableReferenceGenomicRegion<TsrData>> entries = new ArrayList<>();
        EI.wrap(tsrmap.keySet()).forEachRemaining(ref -> {
            EI.wrap(tsrmap.get(ref).keySet()).forEachRemaining(gr -> {
                entries.add(new ImmutableReferenceGenomicRegion<>(ref, gr, tsrmap.get(ref).get(gr)));
            });
        });
        storage.fill(EI.wrap(entries));
        return storage;
    }

    public static void mergeTsrs(Map<ReferenceSequence, List<Tsr>> data) {
        EI.wrap(data.keySet()).forEachRemaining(ref -> {
            List<Tsr> tsrList = data.get(ref);
            tsrList.sort(Tsr::compare);
            if (tsrList.size() < 1) {
                return;
            }
            Tsr lasTsr = tsrList.get(0);
            List<Tsr> toDelete = new ArrayList<>();
            for (int i = 1; i < tsrList.size(); i++) {
                Tsr currentTsr = tsrList.get(i);
                if (currentTsr.getStart() <= lasTsr.getRightmostTissPosition()) {
                    lasTsr.addAll(currentTsr.getMaxTiss());
                    int delta = currentTsr.getEnd() - lasTsr.getEnd();
                    if (delta > 0) {
                        lasTsr.extendBack(delta);
                    }
                    currentTsr.getMaxTiss().clear();
                    toDelete.add(currentTsr);
                } else {
                    lasTsr = currentTsr;
                }
            }
            tsrList.removeAll(toDelete);
        });
    }

    private static Tiss createTiss(int id, int pos) {
        Set<Integer> calledBy = new HashSet<>();
        calledBy.add(id);
        return new Tiss(calledBy, pos);
    }

    public static double log2(double x) {
        return Math.log(x) / Math.log(2) + 1e-10;
    }

    public static double calculateThreshold(List<PeakAndPos> peaks, int avg) {
        if (peaks.size() <= avg) {
            return -9999;
        }
        double xStep = 1.0/peaks.size();
        double yMax = peaks.stream().mapToDouble(PeakAndPos::getValue).max().getAsDouble();
        peaks.sort(Comparator.comparingDouble(PeakAndPos::getValue));
        List<PeakAndPos> peaksNorm = peaks.stream().map(a -> new PeakAndPos(a.getPos(), a.getValue()/yMax)).collect(Collectors.toList());
        peaksNorm.sort(Comparator.comparingDouble(PeakAndPos::getValue));
        double lastMean = lstAvg(peaksNorm, peaksNorm.size() - avg, peaksNorm.size());
        for (int i = peaksNorm.size()-1; i > avg; i--) {
            double currentMean = lstAvg(peaksNorm, i-avg, i);
            if (currentMean - lastMean < xStep) {
                return peaks.get(i-avg/2).getValue();
            }
        }
        return -9999;
    }

    private static double lstAvg(List<PeakAndPos> lst, int from, int to) {
        double sum = 0;
        for (int i = from; i < to; i++) {
            sum += lst.get(i).getValue();
        }
        return sum/(to-from);
    }

    public static int[][] condIndexToCitCondIndex(int[] readConditions, int[] condIndex) {
        int[][] out = new int[readConditions.length][];
        int condAdd = 0;
        for (int i = 0; i < readConditions.length; i++) {
            List<Integer> indexLst = new ArrayList<>();
            int numCond = readConditions[i];
            for (int ci : condIndex) {
                if (ci < numCond + condAdd && ci >= condAdd) {
                    indexLst.add(ci - condAdd);
                }
            }
            condAdd += numCond;
            out[i] = indexLst.stream().mapToInt(Integer::intValue).toArray();
        }
        return out;
    }

    public static float[][] totalsToCitCondIndex(int[] readConditions, float[] totals) {
        float[][] out = new float[readConditions.length][];
        int condAdd = 0;
        for (int i = 0; i < readConditions.length; i++) {
            int numCond = readConditions[i];
            float[] totalTmp = new float[numCond];
            for (int j = 0; j < numCond; j++) {
                totalTmp[j] = totals[condAdd+j];
            }
            condAdd += numCond;
            out[i] = totalTmp;
        }
        return out;
    }

    public static NumericArray extractReadDensities(List<GenomicRegionStorage<AlignedReadsData>> reads,
                                                    int[] condIndex, ReferenceSequence ref, int refLength, Strandness strandness) {
        NumericArray readCounts = NumericArray.createMemory(refLength, NumericArray.NumericArrayType.Float);
        int[] readsNumConds = reads.stream().mapToInt(r -> r.getMetaDataConditions().length).toArray();
        int[][] condCitIndices = condIndexToCitCondIndex(readsNumConds, condIndex);
        for (int citIndex = 0; citIndex < condCitIndices.length; citIndex++) {
            int[] indices = condCitIndices[citIndex];
            extractReadDensitiesFromSingleFile(readCounts, reads.get(citIndex), indices, ref, strandness, new ArrayGenomicRegion(0, refLength));
        }
        return readCounts;
    }

    public static NumericArray extractReadDensities(List<GenomicRegionStorage<AlignedReadsData>> reads,
                                                    int[] condIndex, ReferenceSequence ref, GenomicRegion region, Strandness strandness) {
        NumericArray readCounts = NumericArray.createMemory(region.getTotalLength(), NumericArray.NumericArrayType.Float);
        int[] readsNumConds = reads.stream().mapToInt(r -> r.getMetaDataConditions().length).toArray();
        int[][] condCitIndices = condIndexToCitCondIndex(readsNumConds, condIndex);
        for (int citIndex = 0; citIndex < condCitIndices.length; citIndex++) {
            int[] indices = condCitIndices[citIndex];
            extractReadDensitiesFromSingleFile(readCounts, reads.get(citIndex), indices, ref, strandness, region);
        }
        return readCounts;
    }

    public static NumericArray extractReadDensitiesNormalized(List<GenomicRegionStorage<AlignedReadsData>> reads,
                                                              int[] condIndex, ReferenceSequence ref, GenomicRegion region, Strandness strandness,
                                                              float[] totals) {
        return extractReadDensitiesNormalized(reads, condIndex, ref, region, strandness, totals, ReadCountMode.Weight);
    }

    public static NumericArray extractReadDensitiesNormalized(List<GenomicRegionStorage<AlignedReadsData>> reads,
                                                              int[] condIndex, ReferenceSequence ref, GenomicRegion region, Strandness strandness,
                                                              float[] totals, ReadCountMode mode) {
        NumericArray readCounts = NumericArray.createMemory(region.getTotalLength(), NumericArray.NumericArrayType.Float);
        int[] readsNumConds = reads.stream().mapToInt(r -> r.getMetaDataConditions().length).toArray();
        int[][] condCitIndices = condIndexToCitCondIndex(readsNumConds, condIndex);
        float[][] totalsCitIndices = totalsToCitCondIndex(readsNumConds, totals);
        for (int citIndex = 0; citIndex < condCitIndices.length; citIndex++) {
            int[] indices = condCitIndices[citIndex];
            extractReadDensitiesFromSingleFileNormalized(readCounts, reads.get(citIndex), indices, ref, strandness, region, totalsCitIndices[citIndex], mode);
        }
        return readCounts;
    }

    public static void extractReadDensitiesFromSingleFile(NumericArray ary, GenomicRegionStorage<AlignedReadsData> reads,
                                                    int[] condIndex, ReferenceSequence ref, Strandness strandness, GenomicRegion region) {
        ReferenceSequence refTmp = strandness.equals(Strandness.Antisense) ? ref.toOppositeStrand() : ref;
        reads.ei(refTmp, region).forEachRemaining(r -> {
            GenomicRegion reg = r.getRegion();
            double[] counts = r.getData().getTotalCountsForConditions(ReadCountMode.Weight);
            reg.iterator().forEachRemaining(regPart -> {
                for (int i = regPart.getStart(); i < regPart.getEnd(); i++) {
                    int posIndex = i-region.getStart();
                    if (posIndex < 0 || posIndex >= region.getTotalLength()) {
                        continue;
                    }
                    for (int c : condIndex) {
                        ary.setFloat(posIndex, ary.getFloat(posIndex) + (float)counts[c]);
                    }
                }
            });
        });
    }

    public static void extractReadDensitiesFromSingleFileNormalized(NumericArray ary, GenomicRegionStorage<AlignedReadsData> reads,
                                                                    int[] condIndex, ReferenceSequence ref, Strandness strandness, GenomicRegion region,
                                                                    float[] totals) {
        extractReadDensitiesFromSingleFileNormalized(ary, reads, condIndex, ref, strandness, region, totals, ReadCountMode.Weight);
    }

    public static void extractReadDensitiesFromSingleFileNormalized(NumericArray ary, GenomicRegionStorage<AlignedReadsData> reads,
                                                                    int[] condIndex, ReferenceSequence ref, Strandness strandness, GenomicRegion region,
                                                                    float[] totals, ReadCountMode mode) {
        ReferenceSequence refTmp = strandness.equals(Strandness.Antisense) ? ref.toOppositeStrand() : ref;
        reads.ei(refTmp, region).forEachRemaining(r -> {
            GenomicRegion reg = r.getRegion();
            double[] counts = r.getData().getTotalCountsForConditions(mode);
            reg.iterator().forEachRemaining(regPart -> {
                for (int i = regPart.getStart(); i < regPart.getEnd(); i++) {
                    int posIndex = i-region.getStart();
                    if (posIndex < 0 || posIndex >= region.getTotalLength()) {
                        continue;
                    }
                    for (int c : condIndex) {
                        ary.setFloat(posIndex, ary.getFloat(posIndex) + (((float)counts[c])/totals[c])*1000000);
                    }
                }
            });
        });
    }

    public static double getMappability(List<GenomicRegionStorage<AlignedReadsData>> reads,
                                        int[] condIndex, ReferenceSequence ref, GenomicRegion region, Strandness strandness) {
        int[] readsNumConds = reads.stream().mapToInt(r -> r.getMetaDataConditions().length).toArray();
        int[][] condCitIndices = condIndexToCitCondIndex(readsNumConds, condIndex);
        double readsAll = 0;
        double readsWeighted = 0;
        for (int citIndex = 0; citIndex < condCitIndices.length; citIndex++) {
            int[] indices = condCitIndices[citIndex];
            Mappability m = getMappabilityFromSingleFile(reads.get(citIndex), indices, ref, strandness, region);
            readsAll += m.readSumAll;
            readsWeighted += m.readSumWeighted;
        }
        return readsWeighted / readsAll;
    }

    public static Mappability getMappabilityFromSingleFile(GenomicRegionStorage<AlignedReadsData> reads,
                                                      int[] condIndex, ReferenceSequence ref, Strandness strandness, GenomicRegion region) {
        ReferenceSequence refTmp = strandness.equals(Strandness.Antisense) ? ref.toOppositeStrand() : ref;
        double readAll = 0;
        double readWeight = 0;
        for (ReferenceGenomicRegion<AlignedReadsData> r : reads.ei(refTmp, region).loop()) {
            GenomicRegion reg = r.getRegion();
            double[] countsWeighted = r.getData().getTotalCountsForConditions(ReadCountMode.Weight);
            double[] countsAll = r.getData().getTotalCountsForConditions(ReadCountMode.All);
            for (GenomicRegionPart regPart : reg.iterator().loop()) {
                for (int i = regPart.getStart(); i < regPart.getEnd(); i++) {
                    int posIndex = i-region.getStart();
                    if (posIndex < 0 || posIndex >= region.getTotalLength()) {
                        continue;
                    }
                    for (int c : condIndex) {
                        readWeight += countsWeighted[c];
                        readAll += countsAll[c];
                    }
                }
            }
        }
        return new Mappability(readWeight, readAll);
    }

    //TODO: put somewhere else!
    public static class Mappability {
        public double readSumWeighted;
        public double readSumAll;

        public Mappability(double readSumWeighted, double readSumAll) {
            this.readSumWeighted = readSumWeighted;
            this.readSumAll = readSumAll;
        }
    }

    public static NumericArray extractFivePrimeCounts(List<GenomicRegionStorage<AlignedReadsData>> reads,
                                                      int[] condIndex, ReferenceSequence ref, int refLength, Strandness strandness) {

        NumericArray readCounts = NumericArray.createMemory(refLength, NumericArray.NumericArrayType.Float);
        for (int cond : condIndex) {
            int lstIndex = 0;
            while (cond >= reads.get(lstIndex).getMetaDataConditions().length) {
                cond -= reads.get(lstIndex).getMetaDataConditions().length;
                lstIndex++;
            }
            extractFivePrimeCountsFromSingleFile(readCounts, reads.get(lstIndex), cond, ref, strandness, new ArrayGenomicRegion(0, refLength));
        }
        return readCounts;
    }

    public static NumericArray extractFivePrimeCounts(List<GenomicRegionStorage<AlignedReadsData>> reads,
                                                      int[] condIndex, ReferenceSequence ref, GenomicRegion region, Strandness strandness) {

        NumericArray readCounts = NumericArray.createMemory(region.getTotalLength(), NumericArray.NumericArrayType.Float);
        for (int cond : condIndex) {
            int lstIndex = 0;
            while (cond >= reads.get(lstIndex).getMetaDataConditions().length) {
                cond -= reads.get(lstIndex).getMetaDataConditions().length;
                lstIndex++;
            }
            extractFivePrimeCountsFromSingleFile(readCounts, reads.get(lstIndex), cond, ref, strandness, region);
        }
        return readCounts;
    }

    /**
     * normalized based on read-totals. REGION LENGTH IS NOT CONSIDERED!!!!
     * @param reads
     * @param condIndex
     * @param ref
     * @param region
     * @param strandness
     * @param totals needs to be the same length as conditions in reads
     * @return
     */
    public static NumericArray extractFivePrimeCountsNormalized(List<GenomicRegionStorage<AlignedReadsData>> reads,
                                                                int[] condIndex, ReferenceSequence ref, GenomicRegion region, Strandness strandness,
                                                                float[] totals) {

        NumericArray readCounts = NumericArray.createMemory(region.getTotalLength(), NumericArray.NumericArrayType.Float);
        for (int cond : condIndex) {
            int lstIndex = 0;
            while (cond >= reads.get(lstIndex).getMetaDataConditions().length) {
                cond -= reads.get(lstIndex).getMetaDataConditions().length;
                lstIndex++;
            }
            NumericArray readCountsTmp = NumericArray.createMemory(region.getTotalLength(), NumericArray.NumericArrayType.Float);
            extractFivePrimeCountsFromSingleFile(readCountsTmp, reads.get(lstIndex), cond, ref, strandness, region);
            for (int i = 0; i < readCountsTmp.length(); i++) {
                readCounts.setFloat(i, readCounts.getFloat(i) + ((readCountsTmp.getFloat(i)/totals[cond]) * 1.0E6f));
            }
        }
        return readCounts;
    }

    public static void extractFivePrimeCountsFromSingleFile(NumericArray ary, GenomicRegionStorage<AlignedReadsData> reads, int cond, ReferenceSequence ref, Strandness strandness) {
        extractFivePrimeCountsFromSingleFile(ary, reads, cond, ref, strandness, new ArrayGenomicRegion(0, ary.length()));
    }

    public static void extractFivePrimeCountsFromSingleFile(NumericArray ary, GenomicRegionStorage<AlignedReadsData> reads, int cond, ReferenceSequence ref, Strandness strandness, GenomicRegion region) {
        ReferenceSequence refTmp = strandness.equals(Strandness.Antisense) ? ref.toOppositeStrand() : ref;
        reads.ei(refTmp, region).forEachRemaining(r -> {
            int pos0 = strandness.equals(Strandness.Antisense) ? GenomicRegionPosition.ThreePrime.position(r) : GenomicRegionPosition.FivePrime.position(r);
            int pos1 = strandness.equals(Strandness.Antisense) ? GenomicRegionPosition.ThreePrime.position(r, 1) : GenomicRegionPosition.FivePrime.position(r, 1);
            NumericArray c0 = NumericArray.createMemory(0, NumericArray.NumericArrayType.Float);
            NumericArray c1 = NumericArray.createMemory(0, NumericArray.NumericArrayType.Float);
            for (int k = 0; k < r.getData().getDistinctSequences(); k++) {
                if (hasEndMismatch(r.getData(), k, r.getRegion().getTotalLength(), strandness)) {
                    c1 = r.getData().addCountsForDistinct(k, c1, ReadCountMode.Weight);
                } else {
                    c0 = r.getData().addCountsForDistinct(k, c0, ReadCountMode.Weight);
                }
            }
            int posIndex0 = pos0 - region.getStart();
            if (posIndex0 >= 0 && posIndex0 < ary.length()) {
                if (c0.length() > 0) {
                    ary.setFloat(posIndex0, ary.getFloat(posIndex0) + c0.getFloat(cond));
                }
            }
            int posIndex1 = pos1 - region.getStart();
            if (posIndex1 >= 0 && posIndex1 < ary.length()) {
                if (c1.length() > 0) {
                    ary.setFloat(posIndex1, ary.getFloat(posIndex1) + c1.getFloat(cond));
                }
            }
        });
    }

    private static boolean hasEndMismatch(AlignedReadsData ard, int distict, int readLength, Strandness strandness) {
        if (strandness.equals(Strandness.Antisense)) {
            return hasTailingMismatch(ard, distict, readLength);
        } else {
            return RiboUtils.hasLeadingMismatch(ard, distict);
        }
    }

    private static boolean hasTailingMismatch(AlignedReadsData ard, int distinct, int readLength) {
        for (int i=0; i<ard.getVariationCount(distinct); i++) {
            if (ard.isMismatch(distinct, i) && ard.getMismatchPos(distinct, i)==readLength-1)
                return true;
        }
        return false;
    }

    public static GenomicRegion createRegion(int pos, int windowSize, boolean toTheRight) {
        int regStart = (toTheRight ? pos : pos - windowSize);
        int regStop = (toTheRight ? pos + windowSize: pos);
        return new ArrayGenomicRegion(regStart, regStop);
    }

    public static List<MutableTriple<Integer, Double, Double>> cleanUpMultiValueData(List<MutableTriple<Integer, Double, Double>> lstToClean, int multiThreshold) {
        Map<Long, Integer> countMap = new HashMap<>();
        for (MutableTriple<Integer, Double, Double> itm : lstToClean) {
            long bits = Double.doubleToLongBits(itm.Item2);
            if (countMap.containsKey(bits)) {
                countMap.put(bits, countMap.get(bits) + 1);
            } else {
                countMap.put(bits, 1);
            }
        }
        return EI.wrap(lstToClean).filter(i -> countMap.get(Double.doubleToLongBits(i.Item2)) < multiThreshold).list();
    }

    public static List<MutablePair<Integer, Double>> cleanUpMultiValueDataPair(List<MutablePair<Integer, Double>> lstToClean, int multiThreshold) {
        Map<Long, Integer> countMap = new HashMap<>();
        for (MutablePair<Integer, Double> itm : lstToClean) {
            long bits = Double.doubleToLongBits(itm.Item2);
            if (countMap.containsKey(bits)) {
                countMap.put(bits, countMap.get(bits) + 1);
            } else {
                countMap.put(bits, 1);
            }
        }
        return EI.wrap(lstToClean).filter(i -> countMap.get(Double.doubleToLongBits(i.Item2)) < multiThreshold).list();
    }

    public static void addNeighbourWeights(List<MutableTriple<Integer, Double, Double>> sortedLst) {
        for (int i = 0; i < sortedLst.size(); i++) {
            MutableTriple<Integer, Double, Double> current = sortedLst.get(i);

            int j = i-1;
            while (j >= 0 && current.Item1 - sortedLst.get(j).Item1 < 100) {
                int dist = current.Item1 - sortedLst.get(j).Item1;
                double weight = (double)dist/100.d;
                current.Item3 += sortedLst.get(j).Item2 * weight;
                j--;
            }

            j = i+1;
            while (j < sortedLst.size() && sortedLst.get(j).Item1 - current.Item1 < 100) {
                int dist = sortedLst.get(j).Item1 - current.Item1;
                double weight = (double)dist/100.d;
                current.Item3 += sortedLst.get(j).Item2 * weight;
                j++;
            }
        }
    }

    public static void mergeThresholdData(String[] inFiles, String outFile, int window, boolean isPVal) throws IOException {
//        mergeCustomData(inFiles, outFile, window, isPVal, 1, 0, -1, 1, 2, true, 100);
        Map<ReferenceSequence, List<MutableTriple<Integer, Double, Double>>> map = new HashMap<>();
        for (String inFile : inFiles) {
            EI.lines(inFile).skip(1).forEachRemaining(l -> {
                String[] split = StringUtils.split(l, "\t");
                ReferenceSequence ref = Chromosome.obtain(split[0]);
                int pos = Integer.parseInt(split[1]);
                double val;
                if (split.length > 3) {
                    double v1 = Double.parseDouble(split[2]);
                    double v2 = Double.parseDouble(split[2+1]);
                    val = v1 > v2 ? v1 : v2;
                } else if (isPVal) {
                    val = 1 - Double.parseDouble(split[2]);
                } else {
                    val = Double.parseDouble(split[2]);
                }
                map.computeIfAbsent(ref, absent -> new ArrayList<>()).add(new MutableTriple<>(pos, val, val));
            });
        }
        LineWriter writer = new LineOrientedFile(outFile).write();
        writer.writeLine("Ref\tTiSS\tValue");
        EI.wrap(map.keySet()).forEachRemaining(ref -> {
            List<MutableTriple<Integer, Double, Double>> lst = map.get(ref);
            lst.sort(Comparator.comparingInt(a -> a.Item1));
            addNeighbourWeights(lst);
            lst = cleanUpMultiValueData(lst, 100);
            int lastPos = lst.get(0).Item1;
            MutableTriple<Integer, Double, Double> lastHighest = lst.get(0);
            for (int i = 1; i < lst.size(); i++) {
                MutableTriple<Integer, Double, Double> current = lst.get(i);
                if (current.Item1-lastPos > window) {
                    try {
                        writer.writeLine(ref + "\t" + lastHighest.Item1 + "\t" + lastHighest.Item3);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    lastHighest = current;
                } else {
                    lastHighest = current.Item3 > lastHighest.Item3 ? current : lastHighest;
                }
                lastPos = current.Item1;
            }
            try {
                writer.writeLine(ref + "\t" + lastHighest.Item1 + "\t" + lastHighest.Item3);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        writer.close();
    }

    public static void normalize(Map<ReferenceSequence, List<MutablePair<Integer, Double>>> map) {
        double maxVal = -9999, minVal = 9999999;
        for (List<MutablePair<Integer, Double>> lst : map.values()) {
            for (MutablePair<Integer, Double> p : lst) {
                maxVal = p.Item2 > maxVal ? p.Item2 : maxVal;
                minVal = p.Item2 < minVal ? p.Item2 : minVal;
            }
        }
        maxVal -= minVal;
        for (List<MutablePair<Integer, Double>> lst : map.values()) {
            for (MutablePair<Integer, Double> p : lst) {
                p.Item2 = (p.Item2-minVal) / maxVal;
            }
        }
    }

    // This function is used for the iTiSS-paper only and transforms data formats of ADAPT-CAGE, TSRFinder, TSSPredator into a uniform one
    // p-Values are switched (i.e. 1-pValue) to unify the meaning of scores (high score = TiSS)
    // cRNA-seq data is normalized for all three metrices (DENSITY/KINETIC/PEAK)
    //    This means, that the values are converted into 0-1 range, where 0 is the lowest value and 1 the highest value for the respective dataset
    //
    public static void transformData(String[] inFiles, String outFile, int window, boolean isPVal, int skip, int refCol, int strandCol, int tssCol, int valCol, boolean normalize, boolean twoVal) throws IOException {
        Map<ReferenceSequence, List<MutablePair<Integer, Double>>> map = new HashMap<>();
        for (String inFile : inFiles) {
            Map<ReferenceSequence, List<MutablePair<Integer, Double>>> mapTmp = new HashMap<>();
            EI.lines(inFile).skip(skip).forEachRemaining(l -> {
                String[] split = StringUtils.split(l, "\t");
                ReferenceSequence ref = Chromosome.obtain(split[refCol]);
                if (strandCol >= 0) {
                    ref = Chromosome.obtain(split[refCol] + split[strandCol]);
                }
                int pos = Integer.parseInt(split[tssCol]);
                double val;
                if (twoVal && split.length > 3) {
                    double v1 = Double.parseDouble(split[valCol]);
                    double v2 = Double.parseDouble(split[valCol+1]);
                    val = v1 > v2 ? v1 : v2;
                } else if (isPVal) {
                    val = 1 - Double.parseDouble(split[valCol]);
                } else {
                    if (split[valCol].contains(">")) {
                        val = 100d; // This needs to be done for TSSPredator data...
                    } else {
                        val = Double.parseDouble(split[valCol]);
                    }
                }
                mapTmp.computeIfAbsent(ref, absent -> new ArrayList<>()).add(new MutablePair<>(pos, val));
            });
            if (normalize) normalize(mapTmp);
            for (ReferenceSequence ref : mapTmp.keySet()) {
                map.computeIfAbsent(ref, absent -> new ArrayList<>()).addAll(mapTmp.get(ref));
            }
        }
        LineWriter writer = new LineOrientedFile(outFile).write();
        writer.writeLine("Ref\tTiSS\tValue");
        EI.wrap(map.keySet()).forEachRemaining(ref -> {
            List<MutablePair<Integer, Double>> lst = map.get(ref);
            lst.sort(Comparator.comparingInt(a -> a.Item1));

            int lastPos = lst.get(0).Item1;
            MutablePair<Integer, Double> lastHighest = lst.get(0);
            for (int i = 1; i < lst.size(); i++) {
                MutablePair<Integer, Double> current = lst.get(i);
                if (current.Item1-lastPos >= window) {
                    try {
                        writer.writeLine(ref + "\t" + lastHighest.Item1 + "\t" + lastHighest.Item2);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    lastHighest = current;
                } else {
                    lastHighest = current.Item2 > lastHighest.Item2 ? current : lastHighest;
                }
                lastPos = current.Item1;
            }
            try {
                writer.writeLine(ref + "\t" + lastHighest.Item1 + "\t" + lastHighest.Item2);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        writer.close();
    }
}
