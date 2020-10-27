package gedi.merger;

import gedi.TiSSMergerParameterSet;
import gedi.core.reference.ReferenceSequence;
import gedi.data.dependencyTree.DependencyNode;
import gedi.data.dependencyTree.DependencyTree;
import gedi.util.functions.EI;
import gedi.util.program.GediProgramContext;
import gedi.utils.TiSSUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class MergeTiSSTest {

    @Test
    void mergeAllTiss2() throws IOException {
        String file5 = this.getClass().getResource("/resources/tissmerger/file5.tsv").getPath();
        String file6 = this.getClass().getResource("/resources/tissmerger/file6.tsv").getPath();
        List<String> inFiles = new ArrayList<>();
        inFiles.add(file5); inFiles.add(file6);

        String dependency = "0:1>2x1";
        String pacbio = "xx";
        int pbDelta = 5;
        String prefix = this.getClass().getResource("/resources/tissmerger/").getPath() + "text2/test";

        MergeTiSS merger = new MergeTiSS(new TiSSMergerParameterSet());

        DependencyTree<Map<ReferenceSequence, List<Tsr>>> mapDependencyTree = merger.mergeAllTiss(inFiles, dependency,
                prefix, pacbio, "", pbDelta, 0, "", null, true, 5);

        Set<DependencyNode<Map<ReferenceSequence, List<Tsr>>>> leaves = mapDependencyTree.getLeaves();

        Assert.assertEquals(1, leaves.size());

        Map<ReferenceSequence, List<Tsr>> data = EI.wrap(leaves).next().getData();
//        TiSSUtils.mergeTsrs(data);

        Assert.assertEquals(1, data.keySet().size());

        ReferenceSequence refP = EI.wrap(data.keySet()).filter(ReferenceSequence::isPlus).next();

        Assert.assertEquals("Ref+", refP.toPlusMinusString());
        Assert.assertEquals(1, data.get(refP).size());

//        Ref+	10	5-35	1	1   2	2
        Tiss tiss1 = createTiss(10, 0,1);
//        Ref+	15	5-35	1	0   1	2
        Tiss tiss2 = createTiss(15, 0);
//        Ref+	20	5-35	1	0   1	2
        Tiss tiss3 = createTiss(20, 0);
//        Ref+	25	5-35	1	0   1	2
        Tiss tiss4 = createTiss(25, 0);
//        Ref+	30	5-35	1	1   2	2
        Tiss tiss5 = createTiss(30, 0,1);

        List<Tiss> expectedP = new ArrayList<>();
        expectedP.add(tiss1);expectedP.add(tiss2);expectedP.add(tiss3);expectedP.add(tiss4);expectedP.add(tiss5);

        List<Tiss> tissP = extractAllTiss(data.get(refP));

        for (Tiss tss : tissP) {
            Assert.assertTrue(isContained(tss, expectedP));
        }
        for (Tiss tss : expectedP) {
            Assert.assertTrue(isContained(tss, tissP));
        }
    }

    @Test
    void mergeAllTiss3() throws IOException {
        String file5 = this.getClass().getResource("/resources/tissmerger/file5.tsv").getPath();
        String file6 = this.getClass().getResource("/resources/tissmerger/file6.tsv").getPath();
        List<String> inFiles = new ArrayList<>();
        inFiles.add(file6); inFiles.add(file5);

        String dependency = "0:1>2x1";
        String pacbio = "xx";
        int pbDelta = 5;
        String prefix = this.getClass().getResource("/resources/tissmerger/").getPath() + "text2/test";

        MergeTiSS merger = new MergeTiSS(new TiSSMergerParameterSet());

        DependencyTree<Map<ReferenceSequence, List<Tsr>>> mapDependencyTree = merger.mergeAllTiss(inFiles, dependency,
                prefix, pacbio, "", pbDelta, 0, "", null, true, 5);

        Set<DependencyNode<Map<ReferenceSequence, List<Tsr>>>> leaves = mapDependencyTree.getLeaves();

        Assert.assertEquals(1, leaves.size());

        Map<ReferenceSequence, List<Tsr>> data = EI.wrap(leaves).next().getData();
//        TiSSUtils.mergeTsrs(data);

        Assert.assertEquals(1, data.keySet().size());

        ReferenceSequence refP = EI.wrap(data.keySet()).filter(ReferenceSequence::isPlus).next();

        Assert.assertEquals("Ref+", refP.toPlusMinusString());
        Assert.assertEquals(1, data.get(refP).size());

//        Ref+	10	5-35	1	1   2	2
        Tiss tiss1 = createTiss(10, 0,1);
//        Ref+	15	5-35	0	1   1	2
        Tiss tiss2 = createTiss(15, 1);
//        Ref+	20	5-35	0	1   1	2
        Tiss tiss3 = createTiss(20, 1);
//        Ref+	25	5-35	0	1   1	2
        Tiss tiss4 = createTiss(25, 1);
//        Ref+	30	5-35	1	1   2	2
        Tiss tiss5 = createTiss(30, 0,1);

        List<Tiss> expectedP = new ArrayList<>();
        expectedP.add(tiss1);expectedP.add(tiss2);expectedP.add(tiss3);expectedP.add(tiss4);expectedP.add(tiss5);

        List<Tiss> tissP = extractAllTiss(data.get(refP));

        for (Tiss tss : tissP) {
            Assert.assertTrue(isContained(tss, expectedP));
        }
        for (Tiss tss : expectedP) {
            Assert.assertTrue(isContained(tss, tissP));
        }
    }

    @Test
    void mergeAllTiss4() throws IOException {
        String file5 = this.getClass().getResource("/resources/tissmerger/file5.tsv").getPath();
        String file6 = this.getClass().getResource("/resources/tissmerger/file6.tsv").getPath();
        List<String> inFiles = new ArrayList<>();
        inFiles.add(file6); inFiles.add(file5);

        String dependency = "0:1>2x0x0";
        String pacbio = "xx";
        int pbDelta = 5;
        String prefix = this.getClass().getResource("/resources/tissmerger/").getPath() + "text2/test";

        MergeTiSS merger = new MergeTiSS(new TiSSMergerParameterSet());

        DependencyTree<Map<ReferenceSequence, List<Tsr>>> mapDependencyTree = merger.mergeAllTiss(inFiles, dependency,
                prefix, pacbio, "", pbDelta, 0, "", null, true, 5);

        Set<DependencyNode<Map<ReferenceSequence, List<Tsr>>>> leaves = mapDependencyTree.getLeaves();

        Assert.assertEquals(1, leaves.size());

        Map<ReferenceSequence, List<Tsr>> data = EI.wrap(leaves).next().getData();
//        TiSSUtils.mergeTsrs(data);

        Assert.assertEquals(1, data.keySet().size());

        ReferenceSequence refP = EI.wrap(data.keySet()).filter(ReferenceSequence::isPlus).next();

        Assert.assertEquals("Ref+", refP.toPlusMinusString());
        Assert.assertEquals(2, data.get(refP).size());

//        Ref+	10	5-16	1	1   2	2
        Tiss tiss1 = createTiss(10, 0,1);
//        Ref+	30	25-31	1	1   2	2
        Tiss tiss5 = createTiss(30, 0,1);

        List<Tiss> expectedP = new ArrayList<>();
        expectedP.add(tiss1);expectedP.add(tiss5);

        List<Tiss> tissP = extractAllTiss(data.get(refP));

        for (Tiss tss : tissP) {
            Assert.assertTrue(isContained(tss, expectedP));
        }
        for (Tiss tss : expectedP) {
            Assert.assertTrue(isContained(tss, tissP));
        }
    }

    @Test
    void mergeAllTiss() throws IOException {
        String file1 = this.getClass().getResource("/resources/tissmerger/file1.tsv").getPath();
        String file2 = this.getClass().getResource("/resources/tissmerger/file2.tsv").getPath();
        String file3 = this.getClass().getResource("/resources/tissmerger/file3.tsv").getPath();
        String file4 = this.getClass().getResource("/resources/tissmerger/file4.tsv").getPath();
        List<String> inFiles = new ArrayList<>();
        inFiles.add(file1); inFiles.add(file2); inFiles.add(file3); inFiles.add(file4);

        MergeTiSS merger = new MergeTiSS(new TiSSMergerParameterSet());

        String dependency = "0:1:2:3>4x1";
        String pacbio = "xxxx";
        int pbDelta = 5;
        String prefix = this.getClass().getResource("/resources/tissmerger/").getPath() + "text/test";

        DependencyTree<Map<ReferenceSequence, List<Tsr>>> mapDependencyTree = merger.mergeAllTiss(inFiles, dependency,
                prefix, pacbio, "", pbDelta, 0, "", null, true, 5);

        Set<DependencyNode<Map<ReferenceSequence, List<Tsr>>>> leaves = mapDependencyTree.getLeaves();

        Assert.assertEquals(1, leaves.size());

        Map<ReferenceSequence, List<Tsr>> data = EI.wrap(leaves).next().getData();
//        TiSSUtils.mergeTsrs(data);

        Assert.assertEquals(2, data.keySet().size());

        ReferenceSequence refP = EI.wrap(data.keySet()).filter(ReferenceSequence::isPlus).next();
        ReferenceSequence refM = EI.wrap(data.keySet()).filter(ReferenceSequence::isMinus).next();

        Assert.assertEquals("Ref+", refP.toPlusMinusString());
        Assert.assertEquals("Ref-", refM.toPlusMinusString());
        Assert.assertEquals(2, data.get(refP).size());
        Assert.assertEquals(2, data.get(refM).size());

//        Ref-	8	3-17	0	0	1	0	1	4
        Tiss tiss1 = createTiss(8, 2);
//        Ref-	9	3-17	0	0	1	0	1	4
        Tiss tiss2 = createTiss(9, 2);
//        Ref-	10	3-17	1	0	0	1	2	4
        Tiss tiss3 = createTiss(10, 0,3);
//        Ref-	11	3-17	0	1	0	0	1	4
        Tiss tiss4 = createTiss(11, 1);
//        Ref-	12	3-17	1	1	0	1	3	4
        Tiss tiss5 = createTiss(12, 0,1,3);
//        Ref+	3	-2-12	1	0	0	0	1	3
        Tiss tiss6 = createTiss(3, 0);
//        Ref+	6	-2-12	0	1	0	1	2	3
        Tiss tiss7 = createTiss(6, 1,3);
//        Ref+	12	7-18	0	0	1	0	1	1
        Tiss tiss8 = createTiss(12, 2);
//        Ref-  50  45-51   0   1   0   0   1   1
        Tiss tiss9 = createTiss(50, 1);

        List<Tiss> expectedP = new ArrayList<>();
        List<Tiss> expectedM = new ArrayList<>();
        expectedM.add(tiss1);expectedM.add(tiss2);expectedM.add(tiss3);expectedM.add(tiss4);expectedM.add(tiss5);expectedM.add(tiss9);
        expectedP.add(tiss6);expectedP.add(tiss7);expectedP.add(tiss8);

        List<Tiss> tissP = extractAllTiss(data.get(refP));
        List<Tiss> tissM = extractAllTiss(data.get(refM));

        for (Tiss tss : tissP) {
            Assert.assertTrue(isContained(tss, expectedP));
        }
        for (Tiss tss : tissM) {
            Assert.assertTrue(isContained(tss, expectedM));
        }
        for (Tiss tss : expectedP) {
            Assert.assertTrue(isContained(tss, tissP));
        }
        for (Tiss tss : expectedM) {
            Assert.assertTrue(isContained(tss, tissM));
        }
    }

    private boolean isContained(Tiss toCheck, List<Tiss> expected) {
        for (Tiss tss : expected) {
            if (tss.getTissPos() != toCheck.getTissPos()) {
                continue;
            }
            if (!tss.getCalledBy().equals(toCheck.getCalledBy())) {
                continue;
            }
            return true;
        }
        System.err.println("Tiss does not exist: " + toCheck.getTissPos());
        return false;
    }

    private List<Tiss> extractAllTiss(List<Tsr> tsr) {
        List<Tiss> tiss = new ArrayList<>();
        EI.wrap(tsr).forEachRemaining(t -> {
            tiss.addAll(t.getMaxTiss());
        });
        return tiss;
    }

    private Tiss createTiss(int pos, int... calledby) {
        Set<Integer> calledbySet = new HashSet<>(EI.wrap(calledby).set());
        return new Tiss(calledbySet, pos);
    }
}