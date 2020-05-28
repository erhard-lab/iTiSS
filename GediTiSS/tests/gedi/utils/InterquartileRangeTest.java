package gedi.utils;

import gedi.util.datastructure.array.DoubleArray;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class InterquartileRangeTest {

    @Test
    void hoaresQuickselect() {
        double[] sorted = new double[] {0,1,2,3,4,5};
        double[] unsortedDouble = new double[] {4,3,7,5,1,2,2};
        double[] unsortedSingle = new double[] {4,3,7,5,1,2};
        double[] zero = new double[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1000};

        double val1 = InterquartileRange.hoaresQuickselect(sorted, 3, false);
        double val2 = InterquartileRange.hoaresQuickselect(unsortedDouble, 2, false);
        double val3 = InterquartileRange.hoaresQuickselect(unsortedSingle, 2, false);
        double val4 = InterquartileRange.hoaresQuickselect(zero, 2, false);

        Assert.assertEquals(3, val1, Double.MIN_VALUE);
        Assert.assertEquals(2, val2, Double.MIN_VALUE);
        Assert.assertEquals(3, val3, Double.MIN_VALUE);
        Assert.assertEquals(0, val4, Double.MIN_VALUE);

        Assert.assertArrayEquals(new double[] {0,1,2,3,4,5}, sorted, Double.MIN_VALUE);
        Assert.assertArrayEquals(new double[] {4,3,7,5,1,2,2}, unsortedDouble, Double.MIN_VALUE);
        Assert.assertArrayEquals(new double[] {4,3,7,5,1,2}, unsortedSingle, Double.MIN_VALUE);
        Assert.assertArrayEquals(new double[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1000}, zero, Double.MIN_VALUE);
    }

    @Test
    void interquartileRange() {
        double[] sorted = new double[] {0,1,2,3,4,5};
        double[] unsortedDouble = new double[] {4,3,7,6,1,2,2};
        double[] unsortedSingle = new double[] {4,3,7,7,1,2};
        double[] zero = new double[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1000};

        double val1 = InterquartileRange.interquartileRange(sorted, false);
        double val2 = InterquartileRange.interquartileRange(unsortedDouble, false);
        double val3 = InterquartileRange.interquartileRange(unsortedSingle, false);
        double val4 = InterquartileRange.interquartileRange(zero, false);

        Assert.assertEquals(3, val1, Double.MIN_VALUE);
        Assert.assertEquals(4, val2, Double.MIN_VALUE);
        Assert.assertEquals(5, val3, Double.MIN_VALUE);
        Assert.assertEquals(0, val4, Double.MIN_VALUE);

        Assert.assertArrayEquals(new double[] {0,1,2,3,4,5}, sorted, Double.MIN_VALUE);
        Assert.assertArrayEquals(new double[] {4,3,7,6,1,2,2}, unsortedDouble, Double.MIN_VALUE);
        Assert.assertArrayEquals(new double[] {4,3,7,7,1,2}, unsortedSingle, Double.MIN_VALUE);
        Assert.assertArrayEquals(new double[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1000}, zero, Double.MIN_VALUE);
    }
}