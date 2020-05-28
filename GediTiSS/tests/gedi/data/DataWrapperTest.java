package gedi.data;

import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.genomic.Annotation;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strandness;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.datastructure.array.DoubleArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.EI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DataWrapperTest {
    private static DataWrapper dw;
    private static DataWrapper dwAnti;
    private static Genomic genomic;
    private static int[][] lanes2check;

    @BeforeAll
    static void setUp() {
        genomic = new Genomic();
        ReferenceGenomicRegion<Transcript> plus = new ImmutableReferenceGenomicRegion<Transcript>(Chromosome.obtain("JN555585+"),
                new ArrayGenomicRegion(0,1), new Transcript("gene","trans",-1,-1));
        ReferenceGenomicRegion<Transcript> minus = new ImmutableReferenceGenomicRegion<Transcript>(Chromosome.obtain("JN555585-"),
                new ArrayGenomicRegion(0,1), new Transcript("gene","trans",-1,-1));
        GenomicRegionStorage<Transcript> transcripts = new MemoryIntervalTreeStorage<>(Transcript.class);
        List<ReferenceGenomicRegion<Transcript>> lst = new ArrayList<>();
        lst.add(plus); lst.add(minus);
        transcripts.fill(EI.wrap(lst));
        genomic.add(new Annotation<Transcript>(Genomic.AnnotationType.Transcripts.name()).set(transcripts));

        AlignedReadsData readsData1 = AlignedReadsDataFactory.createSimple(new int[] {1,2,3,4,5});
        AlignedReadsData readsData2 = AlignedReadsDataFactory.createSimple(new int[] {6,7,8,9,10});

        ReferenceGenomicRegion<AlignedReadsData> rgr1 = new ImmutableReferenceGenomicRegion<AlignedReadsData>(Chromosome.obtain("JN555585+"), new ArrayGenomicRegion(0, 100), readsData1);
        ReferenceGenomicRegion<AlignedReadsData> rgr2 = new ImmutableReferenceGenomicRegion<AlignedReadsData>(Chromosome.obtain("JN555585+"), new ArrayGenomicRegion(0, 100), readsData2);
        ReferenceGenomicRegion<AlignedReadsData> rgr3 = new ImmutableReferenceGenomicRegion<AlignedReadsData>(Chromosome.obtain("JN555585-"), new ArrayGenomicRegion(0, 100), readsData2);

        List<ReferenceGenomicRegion<AlignedReadsData>> l1 = new ArrayList<>();
        List<ReferenceGenomicRegion<AlignedReadsData>> l2 = new ArrayList<>();

        l1.add(rgr1);
        l2.add(rgr2);l2.add(rgr3);

        GenomicRegionStorage<AlignedReadsData> gr1 = new MemoryIntervalTreeStorage<>(AlignedReadsData.class);
        GenomicRegionStorage<AlignedReadsData> gr2 = new MemoryIntervalTreeStorage<>(AlignedReadsData.class);

        gr1.fill(EI.wrap(l1));
        gr2.fill(EI.wrap(l2));

        List<GenomicRegionStorage<AlignedReadsData>> rawData = new ArrayList<>();
        rawData.add(gr1); rawData.add(gr2);

        dw = new DataWrapper(rawData, Strandness.Sense);
        dwAnti = new DataWrapper(rawData, Strandness.Antisense);

        lanes2check = new int[][] {new int[] {3,4,5,9}, new int[] {0,1,2,3,4}, new int[] {0,1,6,7}, new int[] {0,5,2,9,8}};
    }

    @Test
    void initData() throws Exception {
        // The following tests are inside this method (needs to be done as they rely on a certain order):
        // - initData()
        // - getCitIndexAccessList()
        // - startAccessingDataTotalized()
        testInitData();

        testGetCitIndexAccessList();

        testGetCitIndexAccessListNew();

        totalizedTestSense();
        totalizedTestAntisense();

        multiTest();
    }

    private void multiTest() throws Exception {
        NumericArray[] readCounts1 = dw.startAccessingData(new Data(lanes2check[2], true), Chromosome.obtain("JN555585+"), 100);
        NumericArray[] readCounts2 = dw.startAccessingData(new Data(lanes2check[2], false), Chromosome.obtain("JN555585-"), 100);

        Assert.assertNotNull(readCounts1);
        Assert.assertNotNull(readCounts2);
        Assert.assertEquals(4, readCounts1.length);
        Assert.assertEquals(1, readCounts2.length);
    }

    private void totalizedTestSense() throws Exception {
        NumericArray readCount1 =  dw.startAccessingData(new Data(lanes2check[0], false), Chromosome.obtain("JN555585+"), 100)[0];
        NumericArray readCount2 =  dw.startAccessingData(new Data(lanes2check[1], false), Chromosome.obtain("JN555585+"), 100)[0];
        NumericArray readCount3 =  dw.startAccessingData(new Data(lanes2check[0], false), Chromosome.obtain("JN555585-"), 100)[0];
        NumericArray readCount4 =  dw.startAccessingData(new Data(lanes2check[1], false), Chromosome.obtain("JN555585-"), 100)[0];

        Assert.assertNotNull(readCount1);
        Assert.assertNotNull(readCount2);
        Assert.assertNotNull(readCount3);
        Assert.assertNotNull(readCount4);
        Assert.assertEquals(25, readCount1.getDouble(0), Double.MIN_VALUE);
        Assert.assertEquals(15, readCount2.getDouble(0), Double.MIN_VALUE);
        Assert.assertEquals(16, readCount3.getDouble(99), Double.MIN_VALUE);
        Assert.assertEquals(0, readCount3.getDouble(0), Double.MIN_VALUE);
        Assert.assertEquals(0, readCount4.getDouble(0), Double.MIN_VALUE);
        Assert.assertEquals(0, readCount4.getDouble(99), Double.MIN_VALUE);

        dw.finishAccessingData(new Data(lanes2check[0], false), Chromosome.obtain("JN555585+"));
        dw.finishAccessingData(new Data(lanes2check[1], false), Chromosome.obtain("JN555585+"));

        Field field = dw.getClass().getDeclaredField("memoryMap");
        field.setAccessible(true);
        Map<Set<Integer>, Map<ReferenceSequence, MemoryReadCount>> memoryMap = (Map<Set<Integer>, Map<ReferenceSequence, MemoryReadCount>>)field.get(dw);
        MemoryReadCount memoryReadCount = memoryMap.get(EI.wrap(lanes2check[0]).set()).get(Chromosome.obtain("JN555585+"));
        Field fieldFinishedAccesses = memoryReadCount.getClass().getDeclaredField("finishedAccesses");
        fieldFinishedAccesses.setAccessible(true);
        int finishedAccesses = (int) fieldFinishedAccesses.get(memoryReadCount);
        Field fieldData = memoryReadCount.getClass().getDeclaredField("readCounts");
        fieldData.setAccessible(true);
        NumericArray[] data = (NumericArray[]) fieldData.get(memoryReadCount);

        Assert.assertEquals(1, finishedAccesses);
        Assert.assertEquals(1, data.length);
        Assert.assertNotNull(data);
        Assert.assertNotNull(data[0]);


        MemoryReadCount memoryReadCount2 = memoryMap.get(EI.wrap(lanes2check[1]).set()).get(Chromosome.obtain("JN555585+"));
        Field fieldFinishedAccesses2 = memoryReadCount2.getClass().getDeclaredField("finishedAccesses");
        fieldFinishedAccesses2.setAccessible(true);
        int finishedAccesses2 = (int) fieldFinishedAccesses2.get(memoryReadCount2);
        Field fieldData2 = memoryReadCount2.getClass().getDeclaredField("readCounts");
        fieldData2.setAccessible(true);
        NumericArray[] data2 = (NumericArray[]) fieldData2.get(memoryReadCount2);

        Assert.assertEquals(1, finishedAccesses2);
        Assert.assertNull(data2);
    }

    private void totalizedTestAntisense() throws Exception {
        NumericArray readCount1 =  dwAnti.startAccessingData(new Data(lanes2check[0], false), Chromosome.obtain("JN555585+"), 100)[0];
        NumericArray readCount2 =  dwAnti.startAccessingData(new Data(lanes2check[1], false), Chromosome.obtain("JN555585+"), 100)[0];
        NumericArray readCount3 =  dwAnti.startAccessingData(new Data(lanes2check[0], false), Chromosome.obtain("JN555585-"), 100)[0];
        NumericArray readCount4 =  dwAnti.startAccessingData(new Data(lanes2check[1], false), Chromosome.obtain("JN555585-"), 100)[0];

        Assert.assertNotNull(readCount1);
        Assert.assertNotNull(readCount2);
        Assert.assertNotNull(readCount3);
        Assert.assertNotNull(readCount4);
        Assert.assertEquals(25, readCount1.getDouble(99), Double.MIN_VALUE);
        Assert.assertEquals(15, readCount2.getDouble( 99), Double.MIN_VALUE);
        Assert.assertEquals(16, readCount3.getDouble(0), Double.MIN_VALUE);
        Assert.assertEquals(0, readCount3.getDouble(99), Double.MIN_VALUE);
        Assert.assertEquals(0, readCount4.getDouble(99), Double.MIN_VALUE);
        Assert.assertEquals(0, readCount4.getDouble(0), Double.MIN_VALUE);

        dwAnti.finishAccessingData(new Data(lanes2check[0], false), Chromosome.obtain("JN555585+"));
        dwAnti.finishAccessingData(new Data(lanes2check[1], false), Chromosome.obtain("JN555585+"));

        Field field = dwAnti.getClass().getDeclaredField("memoryMap");
        field.setAccessible(true);
        Map<Set<Integer>, Map<ReferenceSequence, MemoryReadCount>> memoryMap = (Map<Set<Integer>, Map<ReferenceSequence, MemoryReadCount>>)field.get(dw);
        MemoryReadCount memoryReadCount = memoryMap.get(EI.wrap(lanes2check[0]).set()).get(Chromosome.obtain("JN555585+"));
        Field fieldFinishedAccesses = memoryReadCount.getClass().getDeclaredField("finishedAccesses");
        fieldFinishedAccesses.setAccessible(true);
        int finishedAccesses = (int) fieldFinishedAccesses.get(memoryReadCount);
        Field fieldData = memoryReadCount.getClass().getDeclaredField("readCounts");
        fieldData.setAccessible(true);
        NumericArray[] data = (NumericArray[]) fieldData.get(memoryReadCount);

        Assert.assertEquals(1, finishedAccesses);
        Assert.assertEquals(1, data.length);
        Assert.assertNotNull(data);
        Assert.assertNotNull(data[0]);


        MemoryReadCount memoryReadCount2 = memoryMap.get(EI.wrap(lanes2check[1]).set()).get(Chromosome.obtain("JN555585+"));
        Field fieldFinishedAccesses2 = memoryReadCount2.getClass().getDeclaredField("finishedAccesses");
        fieldFinishedAccesses2.setAccessible(true);
        int finishedAccesses2 = (int) fieldFinishedAccesses2.get(memoryReadCount2);
        Field fieldData2 = memoryReadCount2.getClass().getDeclaredField("readCounts");
        fieldData2.setAccessible(true);
        NumericArray[] data2 = (NumericArray[]) fieldData2.get(memoryReadCount2);

        Assert.assertEquals(1, finishedAccesses2);
        Assert.assertNull(data2);
    }

    private void testInitData() throws Exception {
        List<Data> data = new ArrayList<>();
        data.add(new Data(lanes2check[0], false));
        data.add(new Data(lanes2check[1], false));
        data.add(new Data(lanes2check[0], false));
        data.add(new Data(lanes2check[2], true));

        dw.initData(genomic, data);
        dwAnti.initData(genomic, data);

        Field field = dw.getClass().getDeclaredField("memoryMap");
        field.setAccessible(true);
        Map<Set<Integer>, Map<ReferenceSequence, MemoryReadCount>> memoryMap = (Map<Set<Integer>, Map<ReferenceSequence, MemoryReadCount>>)field.get(dw);

        Assert.assertEquals(3, memoryMap.keySet().size());
        for (Set<Integer> lane : memoryMap.keySet()) {
            Assert.assertEquals(2, memoryMap.get(lane).keySet().size());
            for (ReferenceSequence ref : memoryMap.get(lane).keySet()) {
                Assert.assertNotNull(memoryMap.get(lane).get(ref));
            }
        }

        MemoryReadCount memoryReadCount = memoryMap.get(EI.wrap(lanes2check[0]).set()).get(Chromosome.obtain("JN555585+"));
        Field fieldMaxAccessCount = memoryReadCount.getClass().getDeclaredField("maxAccessCount");
        fieldMaxAccessCount.setAccessible(true);
        int maxAccessCount = (int) fieldMaxAccessCount.get(memoryReadCount);
        Assert.assertEquals(2, maxAccessCount);

        MemoryReadCount memoryReadCount2 = memoryMap.get(EI.wrap(lanes2check[1]).set()).get(Chromosome.obtain("JN555585+"));
        Field fieldMaxAccessCount2 = memoryReadCount2.getClass().getDeclaredField("maxAccessCount");
        fieldMaxAccessCount2.setAccessible(true);
        int maxAccessCount2 = (int) fieldMaxAccessCount2.get(memoryReadCount2);
        Assert.assertEquals(1, maxAccessCount2);

        MemoryReadCount memoryReadCount3 = memoryMap.get(EI.wrap(lanes2check[2]).set()).get(Chromosome.obtain("JN555585+"));
        Field fieldMaxAccessCount3 = memoryReadCount3.getClass().getDeclaredField("maxAccessCount");
        fieldMaxAccessCount3.setAccessible(true);
        int maxAccessCount3 = (int) fieldMaxAccessCount3.get(memoryReadCount2);
        Assert.assertEquals(1, maxAccessCount3);

    }

    private void testGetCitIndexAccessList() throws Exception {
        int[] lane = lanes2check[0];
        Method getCitIndexAccessList = dw.getClass().getDeclaredMethod("getCitIndexAccessList", int[].class);
        getCitIndexAccessList.setAccessible(true);
        Map<Integer, List<Integer>> citAccessList = (Map<Integer, List<Integer>>)getCitIndexAccessList.invoke(dw, lane);

        Assert.assertEquals(2, citAccessList.size());
        Assert.assertEquals(3, (int) citAccessList.get(0).get(0));
        Assert.assertEquals(4, (int) citAccessList.get(0).get(1));
        Assert.assertEquals(0, (int) citAccessList.get(1).get(0));
        Assert.assertEquals(4, (int) citAccessList.get(1).get(1));
    }

    private void testGetCitIndexAccessListNew() throws Exception {
        int[] lane = lanes2check[3];
        Method getCitIndexAccessList = dw.getClass().getDeclaredMethod("getCitIndexAccessListNew", int[].class);
        getCitIndexAccessList.setAccessible(true);
        CitAccessInfo citAccessList = (CitAccessInfo)getCitIndexAccessList.invoke(dw, lane);

        Assert.assertEquals(4, citAccessList.getCitAccessNum());
        Assert.assertEquals(0, citAccessList.getCitAccess(0));
        Assert.assertEquals(1, citAccessList.getCitAccess(1));
        Assert.assertEquals(0, citAccessList.getCitAccess(2));
        Assert.assertEquals(1, citAccessList.getCitAccess(3));
        Assert.assertEquals(1, citAccessList.getLaneAccess(0).size());
        Assert.assertEquals(1, citAccessList.getLaneAccess(1).size());
        Assert.assertEquals(1, citAccessList.getLaneAccess(2).size());
        Assert.assertEquals(2, citAccessList.getLaneAccess(3).size());
        Assert.assertEquals(0, (int) citAccessList.getLaneAccess(0).get(0));
        Assert.assertEquals(0, (int) citAccessList.getLaneAccess(1).get(0));
        Assert.assertEquals(2, (int) citAccessList.getLaneAccess(2).get(0));
        Assert.assertEquals(4, (int) citAccessList.getLaneAccess(3).get(0));
        Assert.assertEquals(3, (int) citAccessList.getLaneAccess(3).get(1));
    }
}