package gedi.utils;

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenomicRegionUtilsTest {

    @Test
    void isContainedInIntron() {
    }

    @Test
    void getIntronIndex() {
    }

    @Test
    void getPartIndes() {
    }

    @Test
    void removeIntron() {
        GenomicRegion region = new ArrayGenomicRegion(10,20,30,40,50,60,70,80); // 10-20,30-40,50-60,70-80
        GenomicRegion removedIntron = GenomicRegionUtils.removeIntron(region,1); // 10-20,30-60,70-80
        GenomicRegion expected = new ArrayGenomicRegion(10,20,30,60,70,80);
        Assert.assertEquals(expected, removedIntron);
    }
}