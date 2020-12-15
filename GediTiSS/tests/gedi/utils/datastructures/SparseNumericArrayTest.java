package gedi.utils.datastructures;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SparseNumericArrayTest {

    @Test
    void get() {
        SparseNumericArray<Integer> ary = new SparseNumericArray<>(10, 0);
        ary.set(4, 3);
        ary.set(2, 5);
        Assert.assertEquals(3, (int) ary.get(4));
        Assert.assertEquals(5, (int) ary.get(2));
        Assert.assertEquals(0, (int) ary.get(5));
    }
}