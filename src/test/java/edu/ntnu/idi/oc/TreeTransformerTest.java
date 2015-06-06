package edu.ntnu.idi.oc;
;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by work on 05/06/15.
 */
public class TreeTransformerTest {


    @Test
    public void testOneTransform() throws Exception {
        Tree tree = Tree.valueOf("(NP (JJ small) (JJ blue) (N bird))");
        TregexPattern pattern = TregexPattern.compile("JJ=jj > NP");
        TsurgeonPattern action = Tsurgeon.parseOperation("delete jj");

        TreeOperation operation = new TreeOperation("prune adjective", pattern, action);
        List<TreeOperation> operations = Arrays.asList(operation);
        TreeTransformer transformer = new TreeTransformer(operations);

        List<Transform> transforms = transformer.transformTree(tree);

        assertEquals(1, transforms.size());
        assertEquals(Tree.valueOf("(NP (JJ blue) (N bird))"), transforms.get(0).subTree);
    }


    @Test
    public void testTwoTransforms() throws Exception {
        Tree tree = Tree.valueOf("(NP (JJ small) (JJ blue) (N bird) (PP (P with) (NP mark)))");

        TreeOperation operation1 = new TreeOperation(
                "prune JJ",
                TregexPattern.compile("JJ=jj > NP"),
                Tsurgeon.parseOperation("delete jj"));

        TreeOperation operation2 = new TreeOperation(
                "prune PP",
                TregexPattern.compile("PP=pp > NP"),
                Tsurgeon.parseOperation("delete pp"));

        List<TreeOperation> operations = Arrays.asList(operation1, operation2);
        TreeTransformer transformer = new TreeTransformer(operations);

        List<Transform> transforms = transformer.transformTree(tree);

        assertEquals(2, transforms.size());
        // all transformations work on the original inut tree,
        // not on the output of a preceding transformation
        assertEquals(Tree.valueOf("(NP (JJ blue) (N bird) (PP (P with) (NP mark)))"), transforms.get(0).subTree);
        assertEquals(Tree.valueOf("(NP (JJ small) (JJ blue) (N bird))"), transforms.get(1).subTree);
    }
}