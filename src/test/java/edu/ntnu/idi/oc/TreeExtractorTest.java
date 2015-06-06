package edu.ntnu.idi.oc;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


public class TreeExtractorTest {

    @Test
    public void testExtractOne() throws Exception {
        Tree tree = Tree.valueOf("(S (NP (JJ blue) (N bird)) (V sings))");
        TregexPattern pattern = TregexPattern.compile("NP < JJ=d1");
        TsurgeonPattern action = Tsurgeon.parseOperation("delete d1");

        TreeOperation operation = new TreeOperation("name", pattern, action);
        List<TreeOperation> operations = Arrays.asList(operation);
        TreeExtractor extractor = new TreeExtractor("label", operations);

        List<Extract> extracts = extractor.extractTrees(tree);

        assertEquals(1, extracts.size());
        assertEquals(2, extracts.get(0).nodeNumber);
        assertEquals(Tree.valueOf("(NP (N bird))"), extracts.get(0).subTree);
    }

    @Test
    public void testExtractTwo() throws Exception {
        Tree tree = Tree.valueOf("(NP (NP (JJ blue) (N car)) (CONJ and) (NP (JJ red) (N cycle)))");
        TregexPattern pattern = TregexPattern.compile("NP < JJ=d1");
        TsurgeonPattern action = Tsurgeon.parseOperation("delete d1");

        TreeOperation operation = new TreeOperation("name", pattern, action);
        List<TreeOperation> operations = Arrays.asList(operation);
        TreeExtractor extractor = new TreeExtractor("label", operations);

        List<Extract> extracts = extractor.extractTrees(tree);


        assertEquals(2, extracts.size());

        assertEquals(2, extracts.get(0).nodeNumber);
        assertEquals(Tree.valueOf("(NP (N car))"), extracts.get(0).subTree);

        assertEquals(9, extracts.get(1).nodeNumber);
        assertEquals(Tree.valueOf("(NP (N cycle))"), extracts.get(1).subTree);
    }

    @Test
    public void testExtractEmbedded() throws Exception {
        Tree tree = Tree.valueOf("(S (NP (JJ blue) (N bird) (PP (P with) (NP (JJ black) (N eyes)))) (V sings))");
        TregexPattern pattern = TregexPattern.compile("NP < JJ=d1");
        TsurgeonPattern action = Tsurgeon.parseOperation("delete d1");

        TreeOperation operation = new TreeOperation("name", pattern, action);
        List<TreeOperation> operations = Arrays.asList(operation);
        TreeExtractor extractor = new TreeExtractor("label", operations);

        List<Extract> extracts = extractor.extractTrees(tree);

        assertEquals(2, extracts.size());

        assertEquals(2, extracts.get(0).nodeNumber);
        assertEquals(Tree.valueOf("(NP (N bird) (PP (P with) (NP (JJ black) (N eyes))))"),
                extracts.get(0).subTree);

        assertEquals(10, extracts.get(1).nodeNumber);
        assertEquals(Tree.valueOf("(NP (N eyes))"), extracts.get(1).subTree);
    }
}