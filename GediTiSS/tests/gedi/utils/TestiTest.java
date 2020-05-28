package gedi.utils;

import gedi.util.ArrayUtils;
import gedi.util.algorithm.clustering.hierarchical.HierarchicalCluster;
import gedi.util.algorithm.clustering.hierarchical.HierarchicalClusterer;
import gedi.util.functions.DistanceMeasure;
import gedi.util.mutable.MutablePair;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestiTest {

    @Test
    public void test() {
        List<MutablePair<Integer, Double>> lst = new ArrayList<>();
        lst.add(new MutablePair<>(4,20.0));
        lst.add(new MutablePair<>(1,3.0));
        lst.add(new MutablePair<>(7,19.0));
        lst.add(new MutablePair<>(2,1.0));
        lst.add(new MutablePair<>(3,2.0));
        lst.add(new MutablePair<>(5,15.0));
        lst.add(new MutablePair<>(6,13.0));

        HierarchicalClusterer<MutablePair<Integer,Double>> clusterer = new HierarchicalClusterer<>();
        HierarchicalCluster<MutablePair<Integer, Double>> cluster = clusterer.cluster(lst.toArray(new MutablePair[0]), DistanceMeasure.MANHATTAN.adapt(a -> new double[] {a.Item2}));

        HierarchicalCluster<MutablePair<Integer, Double>> mutablePairs = cluster.leftChild();
        HierarchicalCluster<MutablePair<Integer, Double>> mutablePairs1 = cluster.rightChild();
        System.err.println(cluster);
    }
}
