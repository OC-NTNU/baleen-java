package edu.ntnu.idi.oc.trees;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


public class TreeExtractorTest {
    /**
     * Test matching a single pattern with deletion once
     */
    @Test
    public void testExtractOne() throws Exception {
        Tree tree = Tree.valueOf("(S (NP (JJ blue) (NN bird)) (V sings))");
        TregexPattern pattern = TregexPattern.compile("NP < JJ=d1");
        TsurgeonPattern action = Tsurgeon.parseOperation("delete d1");

        TreeOperation operation = new TreeOperation("name", pattern, action);
        List<TreeOperation> operations = Arrays.asList(operation);
        TreeExtractor extractor = new TreeExtractor("label", operations);

        List<Extract> extracts = extractor.extractTrees(tree);

        assertEquals(1, extracts.size());
        assertEquals(2, extracts.get(0).nodeNumber);
        assertEquals(Tree.valueOf("(NP (NN bird))"), extracts.get(0).subTree);
    }

    /**
     * Test matching a single pattern with deletion twice on coordinated nodes
     */
    @Test
    public void testExtractOneCoordinated() throws Exception {
        Tree tree = Tree.valueOf("(NP (NP (JJ blue) (NN car)) (CONJ and) (NP (JJ red) (NN cycle)))");
        TregexPattern pattern = TregexPattern.compile("NP < JJ=d1");
        TsurgeonPattern action = Tsurgeon.parseOperation("delete d1");

        TreeOperation operation = new TreeOperation("name", pattern, action);
        List<TreeOperation> operations = Arrays.asList(operation);
        TreeExtractor extractor = new TreeExtractor("label", operations);

        List<Extract> extracts = extractor.extractTrees(tree);


        assertEquals(2, extracts.size());

        assertEquals(2, extracts.get(0).nodeNumber);
        assertEquals(Tree.valueOf("(NP (NN car))"), extracts.get(0).subTree);

        assertEquals(9, extracts.get(1).nodeNumber);
        assertEquals(Tree.valueOf("(NP (NN cycle))"), extracts.get(1).subTree);
    }

    /**
     * Test matching a single pattern with deletion twice on embedded nodes
     */
    @Test
    public void testExtractOneEmbedded() throws Exception {
        Tree tree = Tree.valueOf("(S (NP (JJ blue) (NN bird) (PP (P with) (NP (JJ black) (NN eyes)))) (V sings))");
        TregexPattern pattern = TregexPattern.compile("NP < JJ=d1");
        TsurgeonPattern action = Tsurgeon.parseOperation("delete d1");

        TreeOperation operation = new TreeOperation("name", pattern, action);
        List<TreeOperation> operations = Arrays.asList(operation);
        TreeExtractor extractor = new TreeExtractor("label", operations);

        List<Extract> extracts = extractor.extractTrees(tree);

        assertEquals(2, extracts.size());

        assertEquals(2, extracts.get(0).nodeNumber);
        assertEquals(Tree.valueOf("(NP (NN bird) (PP (P with) (NP (JJ black) (NN eyes))))"),
                extracts.get(0).subTree);

        assertEquals(10, extracts.get(1).nodeNumber);
        assertEquals(Tree.valueOf("(NP (NN eyes))"), extracts.get(1).subTree);
    }

    /**
     * Test matching two patterns with deletion once on coordinated nodes
     */
    @Test
    public void testExtractTwoCoordinated() throws Exception {
        Tree tree = Tree.valueOf("(NP (NP (JJ blue) (NN car)) (CONJ and) (NP (DT a) (NN cycle)))");

        TregexPattern pattern1 = TregexPattern.compile("NP < JJ=d1");
        TsurgeonPattern action1 = Tsurgeon.parseOperation("delete d1");
        TreeOperation operation1 = new TreeOperation("name1", pattern1, action1);

        TregexPattern pattern2 = TregexPattern.compile("NP < DT=d1");
        TsurgeonPattern action2 = Tsurgeon.parseOperation("delete d1");
        TreeOperation operation2 = new TreeOperation("name2", pattern2, action2);

        List<TreeOperation> operations = Arrays.asList(operation1, operation2);
        TreeExtractor extractor = new TreeExtractor("label", operations);

        List<Extract> extracts = extractor.extractTrees(tree);

        assertEquals(2, extracts.size());

        assertEquals(2, extracts.get(0).nodeNumber);
        assertEquals(Tree.valueOf("(NP (NN car))"), extracts.get(0).subTree);

        // test that earlier deletion of JJ does not alter the node count
        assertEquals(9, extracts.get(1).nodeNumber);
        assertEquals(Tree.valueOf("(NP (NN cycle))"), extracts.get(1).subTree);
    }

    /**
     * Test matching two patterns with deletion twice on embedded nodes
     */
    @Test
    public void testExtractTwoEmbedded() throws Exception {
        Tree tree = Tree.valueOf("(S (NP (DT a) (JJ blue) (NN bird) (PP (P with) (NP (DT a) (JJ black) (NN eye)))) (V sings))");

        TregexPattern pattern1 = TregexPattern.compile("NP < JJ=d1");
        TsurgeonPattern action1 = Tsurgeon.parseOperation("delete d1");
        TreeOperation operation1 = new TreeOperation("name1", pattern1, action1);

        TregexPattern pattern2 = TregexPattern.compile("NP < DT=d1");
        TsurgeonPattern action2 = Tsurgeon.parseOperation("delete d1");
        TreeOperation operation2 = new TreeOperation("name2", pattern2, action2);

        List<TreeOperation> operations = Arrays.asList(operation1, operation2);
        TreeExtractor extractor = new TreeExtractor("label", operations);

        List<Extract> extracts = extractor.extractTrees(tree);

        assertEquals(4, extracts.size());

        assertEquals(2, extracts.get(0).nodeNumber);
        assertEquals(Tree.valueOf("(NP (DT a) (NN bird) (PP (P with) (NP (DT a) (JJ black) (NN eye))))"),
                extracts.get(0).subTree);

        assertEquals(12, extracts.get(1).nodeNumber);
        assertEquals(Tree.valueOf("(NP (DT a) (NN eye))"),
                extracts.get(1).subTree);

        assertEquals(2, extracts.get(2).nodeNumber);
        assertEquals(Tree.valueOf("(NP (JJ blue) (NN bird) (PP (P with) (NP (DT a) (JJ black) (NN eye))))"),
                extracts.get(2).subTree);

        assertEquals(12, extracts.get(3).nodeNumber);
        assertEquals(Tree.valueOf("(NP (JJ black) (NN eye))"),
                extracts.get(3).subTree);


    }
}