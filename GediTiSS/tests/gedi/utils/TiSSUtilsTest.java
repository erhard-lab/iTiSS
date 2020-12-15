package gedi.utils;

import cern.colt.function.DoubleComparator;
import cern.colt.function.FloatComparator;
import com.sun.deploy.util.ArrayUtil;
import gedi.util.ArrayUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class TiSSUtilsTest {

    @Test
    public void testTotalsToCitCondIndex() {
        int[] readConditions = new int[] {5,3,1};
        int[] condIndices = new int[] {3,4, 8};
        float[] totals = new float[] {0,1,2,3,4,5,6,7,8};
        float[] expectedArray1 = new float[] {0,1,2,3,4};
        float[] expectedArray2 = new float[] {5,6,7};
        float[] expectedArray3 = new float[] {8};

        float[][] output = TiSSUtils.totalsToCitCondIndex(readConditions, totals);

        Assert.assertEquals(3, output.length);
        Assert.assertArrayEquals(expectedArray1, output[0], 0.0001f);
        Assert.assertArrayEquals(expectedArray2, output[1], 0.0001f);
        Assert.assertArrayEquals(expectedArray3, output[2], 0.0001f);
    }

    @Test
    public void testCondIndexToCitCondIndex() {
        int[] readConditions = new int[] {5,3,1};
        int[] condIndices = new int[] {3, 8};
        int[] expectedArray1 = new int[] {3};
        int[] expectedArray2 = new int[] {};
        int[] expectedArray3 = new int[] {0};

        int[][] output = TiSSUtils.condIndexToCitCondIndex(readConditions, condIndices);

        Assert.assertEquals(3, output.length);
        Assert.assertArrayEquals(expectedArray1, output[0]);
        Assert.assertArrayEquals(expectedArray2, output[1]);
        Assert.assertArrayEquals(expectedArray3, output[2]);
    }

    @Test
    public void testReplicationIndicesExtraction() {
        int[][] repAry = TiSSUtils.extractReplicatesFromString("xoxo111222");

        Assert.assertThat(repAry.length, CoreMatchers.is(4));

        int[] x = new int[] {0,2};
        int[] o = new int[] {1,3};
        int[] one = new int[] {4,5,6};
        int[] two = new int[] {7,8,9};

        Assert.assertTrue("x", containsEqualArray(repAry, x));
        Assert.assertTrue("o", containsEqualArray(repAry, o));
        Assert.assertTrue("1", containsEqualArray(repAry, one));
        Assert.assertTrue("2", containsEqualArray(repAry, two));

        int[][] repAryWithSkip = TiSSUtils.extractReplicatesFromString("__ww__xx", '_');

        Assert.assertEquals(2, repAryWithSkip.length);

        int[] water = new int[] {2,3};
        int[] xrn1 = new int[] {6,7};

        Assert.assertTrue("water", containsEqualArray(repAryWithSkip, water));
        Assert.assertTrue("xrn1", containsEqualArray(repAryWithSkip, xrn1));

        int[][] singleRep = TiSSUtils.extractReplicatesFromString("__xxx_x", '_');

        Assert.assertEquals(1, singleRep.length);

        int[] vals = new int[] {2,3,4,6};

        Assert.assertTrue("single", containsEqualArray(singleRep, vals));

        int[][] empty = TiSSUtils.extractReplicatesFromString("", '_');

        Assert.assertEquals(0, empty.length);
    }

    @Test
    public void testExtractTimecoursesFromString() {
        int[][] tcAry = TiSSUtils.extractTimecoursesFromString("123231", new int[][] {new int[] {0, 1, 2}, new int[] {3, 4, 5}});

        Assert.assertEquals(2, tcAry.length);

        int[] t1 = new int[] {0,1,2};
        int[] t2 = new int[] {5,3,4};

        Assert.assertTrue(containsEqualArray(tcAry, t1));
        Assert.assertTrue(containsEqualArray(tcAry, t2));

        int[][] tcAry2 = TiSSUtils.extractTimecoursesFromString("113322", new int[][] {new int[] {0, 3, 4}, new int[] {1, 2, 5}});

        Assert.assertEquals(2, tcAry2.length);

        int[] t12 = new int[] {0,4,3};
        int[] t22 = new int[] {1,5,2};

        Assert.assertTrue(containsEqualArray(tcAry2, t12));
        Assert.assertTrue(containsEqualArray(tcAry2, t22));

        int[][] tcAry3 = TiSSUtils.extractTimecoursesFromString("11_33_22", new int[][] {new int[] {0, 4, 6, 2}, new int[] {1, 3, 7, 5}}, '_');

        Assert.assertEquals(2, tcAry3.length);

        int[] t13 = new int[] {0,6,4};
        int[] t23 = new int[] {1,7,3};

        Assert.assertTrue(containsEqualArray(tcAry3, t13));
        Assert.assertTrue(containsEqualArray(tcAry3, t23));

        int[][] tcAry4 = TiSSUtils.extractTimecoursesFromString("_643121234_6", new int[][] {new int[] {1, 2, 3, 4, 5}, new int[] {6, 7, 8, 9, 10, 11}}, '_');

        Assert.assertEquals(2, tcAry.length);

        int[] t14 = new int[] {4,5,3,2,1};
        int[] t24 = new int[] {6,7,8,9,11};

        Assert.assertTrue(containsEqualArray(tcAry4, t14));
        Assert.assertTrue(containsEqualArray(tcAry4, t24));
    }

    private boolean areArraysEqual(int[] a, int[] b) {
        if(a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean containsEqualArray(int[][] a, int[] b) {
        for (int[] ary : a) {
            if (areArraysEqual(ary, b)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void testGroupConcurrents() {
        List<Integer> testLst1 = new ArrayList<>(Arrays.asList(1,4,5,7,10,20));

        List<List<Integer>> outLst1_1 = TiSSUtils.groupConcurrents(testLst1, 1);
        List<List<Integer>> outLst1_2 = TiSSUtils.groupConcurrents(testLst1, 2);

        Assert.assertEquals(5, outLst1_1.size());
        Assert.assertEquals(4, outLst1_2.size());

        Assert.assertEquals(new Integer(1), outLst1_1.get(0).get(0));
        Assert.assertEquals(new Integer(1), outLst1_2.get(0).get(0));
        Assert.assertEquals(new Integer(4), outLst1_1.get(1).get(0));
        Assert.assertEquals(new Integer(4), outLst1_2.get(1).get(0));
        Assert.assertEquals(new Integer(5), outLst1_1.get(1).get(1));
        Assert.assertEquals(new Integer(5), outLst1_2.get(1).get(1));
        Assert.assertEquals(new Integer(7), outLst1_1.get(2).get(0));
        Assert.assertEquals(new Integer(7), outLst1_2.get(1).get(2));
        Assert.assertEquals(new Integer(10), outLst1_1.get(3).get(0));
        Assert.assertEquals(new Integer(10), outLst1_2.get(2).get(0));
        Assert.assertEquals(new Integer(20), outLst1_1.get(4).get(0));
        Assert.assertEquals(new Integer(20), outLst1_2.get(3).get(0));
    }

    @Test
    void testGetOrder() {
        double[] ary1 = new double[] {2,4,3,2,5,63,3,3,2};
        int[] solution = new int[] {0,3,8,2,6,7,1,4,5};
        int[] test = TiSSUtils.getOrder(ary1, Comparator.comparingDouble(d -> d));
        Assert.assertArrayEquals(solution, test);

        double[] ary2 = new double[] {2,4,3,2,5,63,3,3,2};
        int[] solution2 = new int[] {5,4,1,2,6,7,0,3,8};
        int[] test2 = TiSSUtils.getOrder(ary2, Comparator.comparingDouble(d -> -d));
        Assert.assertArrayEquals(solution2, test2);
    }
}