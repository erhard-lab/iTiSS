package gedi.data.dependencyTree;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("Duplicates")
class DependencyTreeTest {
    //(2)(3)(0)(1)(4)(5)
    //  \ |   \ |   \ |
    // (2,3) (0,1) (4,5)
    //   |     |     |
    //    \   / \   /
    //  (11,10) (10,12)
    //      \     /
    //      (14,15)
    String depString = "0:1>10x0,2:3>11x0,4:5>12x0,10:11>14x0,10:12>15x0,14:15>16x0";
    DependencyTree<Void> tree = new DependencyTree<Void>();
    //(0)(1)(3)(2)(4)
    // |  |  |  |  |
    // |   \/ \/   |
    // |  (5) (6)  |
    // |   |   |   |
    //  \  |  /    /
    //   \ | /    /
    //    (7)    /
    //      \   /
    //       (8)
    String depString2 = "3:1>5x0,3:2>6x0,0:5:6>7x0,7:4>8x0";
    DependencyTree<Void> tree2 = new DependencyTree<Void>();

    private void poolNodes(Collection<DependencyNode<Void>> root, Set<DependencyNode<Void>> pooled) {
        if (root == null) {
            return;
        }
        for (DependencyNode<Void> node : root) {
            pooled.add(node);
            poolNodes(node.getOutNodes(), pooled);
        }
    }

    @Test
    void getLeaves() {
        tree.buildTree(depString);
        Set<DependencyNode<Void>> leaves = tree.getLeaves();

        Assert.assertNotNull(leaves);
        Assert.assertEquals(1, leaves.size());
        Assert.assertEquals(16, leaves.iterator().next().getId());

        tree2.buildTree(depString2);
        Set<DependencyNode<Void>> leaves2 = tree2.getLeaves();

        Assert.assertNotNull(leaves2);
        Assert.assertEquals(1, leaves2.size());
        Assert.assertEquals(8, leaves2.iterator().next().getId());
    }

    @Test
    void buildTree() {
        tree.buildTree(depString);
        Set<DependencyNode<Void>> root = tree.getRoot();

        Assert.assertEquals(6, root.size());

        Set<DependencyNode<Void>> depNodes = new HashSet<>();
        poolNodes(root, depNodes);
        Assert.assertEquals(12, depNodes.size());

        for (DependencyNode<Void> node : root) {
            if (node.getId() == 0) {
                Assert.assertEquals(1, node.getOutNodes().size());
                Assert.assertEquals(10, node.getOutNodes().get(0).getId());

                DependencyNode<Void> nextNode = node.getOutNodes().get(0);
                Assert.assertEquals(2, nextNode.getOutNodes().size());
                for (DependencyNode<Void> outNode : nextNode.getOutNodes()) {
                    if (outNode.getId() == 14) {
                        DependencyNode<Void> nextnextNode = outNode.getOutNodes().get(0);
                        Assert.assertEquals(16, nextnextNode.getId());
                        Assert.assertNull(nextnextNode.getOutNodes());
                    }
                    else if (outNode.getId() == 15) {
                        DependencyNode<Void> nextnextNode = outNode.getOutNodes().get(0);
                        Assert.assertEquals(16, nextnextNode.getId());
                        Assert.assertNull(nextnextNode.getOutNodes());
                    }
                    else {
                        Assert.fail();
                    }
                }
            }
            else if (node.getId() == 1) {
                Assert.assertEquals(1, node.getOutNodes().size());
                Assert.assertEquals(10, node.getOutNodes().get(0).getId());

                DependencyNode<Void> nextNode = node.getOutNodes().get(0);
                Assert.assertEquals(2, nextNode.getOutNodes().size());
                for (DependencyNode<Void> outNode : nextNode.getOutNodes()) {
                    if (outNode.getId() == 14) {
                        DependencyNode nextnextNode = outNode.getOutNodes().get(0);
                        Assert.assertEquals(16, nextnextNode.getId());
                        Assert.assertNull(nextnextNode.getOutNodes());
                    }
                    else if (outNode.getId() == 15) {
                        DependencyNode nextnextNode = outNode.getOutNodes().get(0);
                        Assert.assertEquals(16, nextnextNode.getId());
                        Assert.assertNull(nextnextNode.getOutNodes());
                    }
                    else {
                        Assert.fail();
                    }
                }
            }
            else if (node.getId() == 2) {
                Assert.assertEquals(1, node.getOutNodes().size());
                Assert.assertEquals(11, node.getOutNodes().get(0).getId());

                DependencyNode<Void> nextNode = node.getOutNodes().get(0);
                Assert.assertEquals(1, nextNode.getOutNodes().size());

                DependencyNode<Void> outNode = nextNode.getOutNodes().get(0);
                Assert.assertEquals(14, outNode.getId());
                Assert.assertEquals(1, outNode.getOutNodes().size());

                DependencyNode nextnextNode = outNode.getOutNodes().get(0);
                Assert.assertEquals(16, nextnextNode.getId());
                Assert.assertNull(nextnextNode.getOutNodes());
            }
            else if (node.getId() == 3) {
                Assert.assertEquals(1, node.getOutNodes().size());
                Assert.assertEquals(11, node.getOutNodes().get(0).getId());

                DependencyNode<Void> nextNode = node.getOutNodes().get(0);
                Assert.assertEquals(1, nextNode.getOutNodes().size());

                DependencyNode<Void> outNode = nextNode.getOutNodes().get(0);
                Assert.assertEquals(14, outNode.getId());
                Assert.assertEquals(1, outNode.getOutNodes().size());

                DependencyNode nextnextNode = outNode.getOutNodes().get(0);
                Assert.assertEquals(16, nextnextNode.getId());
                Assert.assertNull(nextnextNode.getOutNodes());
            }
            else if (node.getId() == 4) {
                Assert.assertEquals(1, node.getOutNodes().size());
                Assert.assertEquals(12, node.getOutNodes().get(0).getId());

                DependencyNode<Void> nextNode = node.getOutNodes().get(0);
                Assert.assertEquals(1, nextNode.getOutNodes().size());

                DependencyNode<Void> outNode = nextNode.getOutNodes().get(0);
                Assert.assertEquals(15, outNode.getId());
                Assert.assertEquals(1, outNode.getOutNodes().size());

                DependencyNode nextnextNode = outNode.getOutNodes().get(0);
                Assert.assertEquals(16, nextnextNode.getId());
                Assert.assertNull(nextnextNode.getOutNodes());
            }
            else if (node.getId() == 5) {
                Assert.assertEquals(1, node.getOutNodes().size());
                Assert.assertEquals(12, node.getOutNodes().get(0).getId());

                DependencyNode<Void> nextNode = node.getOutNodes().get(0);
                Assert.assertEquals(1, nextNode.getOutNodes().size());

                DependencyNode<Void> outNode = nextNode.getOutNodes().get(0);
                Assert.assertEquals(15, outNode.getId());
                Assert.assertEquals(1, outNode.getOutNodes().size());

                DependencyNode nextnextNode = outNode.getOutNodes().get(0);
                Assert.assertEquals(16, nextnextNode.getId());
                Assert.assertNull(nextnextNode.getOutNodes());
            } else {
                Assert.fail();
            }
        }
    }
}