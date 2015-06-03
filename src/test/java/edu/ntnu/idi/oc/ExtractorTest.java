package edu.ntnu.idi.oc;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by work on 29/05/15.
 */
public class ExtractorTest {

    @Test
    public void testExtractOne() throws Exception {
        Tree tree = Tree.valueOf("(S (NP (JJ blue) (N bird)) (V sings))");
        TregexPattern pattern = TregexPattern.compile("NP < JJ=d1");
        TsurgeonPattern operation = Tsurgeon.parseOperation("delete d1");

        Transformation transform = new Transformation("name", pattern, operation);

        List<Extraction> extractions = Extractor.transformTree(transform, tree);

        assertEquals(1, extractions.size());
        assertEquals(2, extractions.get(0).nodeNumber);
        assertEquals(Tree.valueOf("(NP (N bird))"), extractions.get(0).subTree);
    }

    @Test
    public void testExtractTwo() throws Exception {
        Tree tree = Tree.valueOf("(NP (NP (JJ blue) (N car)) (CONJ and) (NP (JJ red) (N cycle)))");
        TregexPattern pattern = TregexPattern.compile("NP < JJ=d1");
        TsurgeonPattern operation = Tsurgeon.parseOperation("delete d1");

        Transformation transform = new Transformation("name", pattern, operation);

        List<Extraction> extractions = Extractor.transformTree(transform, tree);

        assertEquals(2, extractions.size());

        assertEquals(2, extractions.get(0).nodeNumber);
        assertEquals(Tree.valueOf("(NP (N car))"), extractions.get(0).subTree);

        assertEquals(9, extractions.get(1).nodeNumber);
        assertEquals(Tree.valueOf("(NP (N cycle))"), extractions.get(1).subTree);
    }

    @Test
    public void testExtractEmbedded() throws Exception {
        Tree tree = Tree.valueOf("(S (NP (JJ blue) (N bird) (PP (P with) (NP (JJ black) (N eyes)))) (V sings))");
        TregexPattern pattern = TregexPattern.compile("NP < JJ=d1");
        TsurgeonPattern operation = Tsurgeon.parseOperation("delete d1");

        Transformation transform = new Transformation("name", pattern, operation);

        List<Extraction> extractions = Extractor.transformTree(transform, tree);

        assertEquals(2, extractions.size());

        assertEquals(2, extractions.get(0).nodeNumber);
        assertEquals(Tree.valueOf("(NP (N bird) (PP (P with) (NP (JJ black) (N eyes))))"),
                extractions.get(0).subTree);

        assertEquals(10, extractions.get(1).nodeNumber);
        assertEquals(Tree.valueOf("(NP (N eyes))"), extractions.get(1).subTree);
    }
}