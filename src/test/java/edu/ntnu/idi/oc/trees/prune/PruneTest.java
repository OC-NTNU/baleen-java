package edu.ntnu.idi.oc.trees.prune;

import edu.ntnu.idi.oc.trees.Transform;
import edu.ntnu.idi.oc.trees.TreeTransformer;
import edu.stanford.nlp.trees.Tree;
import org.junit.Before;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;


public abstract class PruneTest {
    TreeTransformer transformer;
    String transformationsFile;

    @Before
    public void setUp() throws Exception {
        Class cls = Class.forName("edu.ntnu.idi.oc.trees.Transformation");
        ClassLoader cLoader = cls.getClassLoader();

        // finds resource with the given name
        InputStream stream = cLoader.getResourceAsStream(transformationsFile);
        transformer = new TreeTransformer(stream);
        stream.close();
    }

    public void testTreeOperation(String operationName, String input, String expected) {
        Tree inputTree = Tree.valueOf(input);
        Transform transform = transformer.transformTree(inputTree, operationName);
        // if there was no match, transform is null
        String output = (transform != null) ? transform.subTree.toString() : null;
        assertEquals(String.format("\nName     :%s\nInput    :%s", operationName, input), expected, output);
    }
}
