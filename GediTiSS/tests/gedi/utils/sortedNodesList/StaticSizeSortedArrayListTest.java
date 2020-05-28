package gedi.utils.sortedNodesList;

import gedi.util.datastructure.array.DoubleArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.EI;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class StaticSizeSortedArrayListTest {
    private static StaticSizeSortedArrayList<Double> list;
    private static int listSize;

    @BeforeEach
    void setUp() {
        double[] data = new double[] {10.0, 0.0, -0.0, 13.37, -200.223, -123.123, 1532.234, 11.11, 11.11, 11.11};
        list = new StaticSizeSortedArrayList<>(EI.wrap(data).toList(), Double::compareTo);
        listSize = data.length;
        Assert.assertTrue(isListSorted());
        printList();
    }

    @Test
    void insertSortedAndDelete() {
        System.out.println("insertSortedAndDelete");
        printList();
        list.insertSortedAndDelete(-99999.0, 11.11);
        isSortedAndEqualLengthTest();
        list.insertSortedAndDelete(0.0, -200.233);
        isSortedAndEqualLengthTest();
        list.insertSortedAndDelete(-0.0, -0.0);
        isSortedAndEqualLengthTest();
        list.insertSortedAndDelete(10000.0, 0.0);
        isSortedAndEqualLengthTest();
        printList();
    }

    @Test
    void getValueAtIndex() {
        System.out.println("getValueAtIndex");
        printList();
        list.insertSortedAndDelete(99999.0, 1532.234);
        Assert.assertEquals(99999.0, list.getValueAtIndex(listSize-1), Double.MIN_VALUE);
        list.insertSortedAndDelete(-9999999.0, -0.0);
        Assert.assertEquals(-9999999.0, list.getValueAtIndex(0), Double.MIN_VALUE);
        printList();
    }

    @Test
    void getNextHigherIndex() {
        Assert.assertEquals(8, list.getNextHigherIndex(11.11));
        Assert.assertEquals(4, list.getNextHigherIndex(0.0));
        Assert.assertEquals(1, list.getNextHigherIndex(-200.));
        Assert.assertEquals(0, list.getNextHigherIndex(-201.));
        Assert.assertEquals(9, list.getNextHigherIndex(1532.));
        Assert.assertEquals(10, list.getNextHigherIndex(1533.));
    }

    @Test
    void getNextLowerIndex() {
        Assert.assertEquals(4, list.getNextLowerIndex(11.11));
        Assert.assertEquals(2, list.getNextLowerIndex(0.0));
        Assert.assertEquals(0, list.getNextLowerIndex(-200.));
        Assert.assertEquals(-1, list.getNextLowerIndex(-201.));
        Assert.assertEquals(8, list.getNextLowerIndex(1532.));
        Assert.assertEquals(9, list.getNextLowerIndex(1533.));
    }

    @Test
    void getSize() {
        Assert.assertEquals(listSize, list.getSize());
    }

    private void isSortedAndEqualLengthTest() {
        Assert.assertTrue(isListSorted());
        getSize();
    }

    private static boolean isListSorted() {
        double lastVal = -Double.MAX_VALUE;
        for (int i = 0; i < list.getSize(); i++) {
            if (list.getValueAtIndex(i) < lastVal) {
                return false;
            }
            lastVal = list.getValueAtIndex(i);
        }
        return true;
    }

    private static void printList() {
        for (int i = 0; i < listSize; i++) {
            System.out.print(list.getValueAtIndex(i) + ",");
        }
        System.out.println();
    }
}