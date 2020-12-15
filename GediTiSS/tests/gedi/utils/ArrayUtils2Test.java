package gedi.utils;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArrayUtils2Test {

    @Test
    void smooth() {
        int[] test1 = new int[] {2,3,5,4,5,3,5,1,1};
        double[] expected1 = new double[] {0,0,3.8,4,4.4,3.6,3,0,0};

        double[] result1 = ArrayUtils2.smooth(test1, 5);

        Assert.assertEquals(expected1.length, test1.length);
        Assert.assertEquals(expected1.length, result1.length);
        Assert.assertArrayEquals(expected1, result1, 0.0001);
    }
}