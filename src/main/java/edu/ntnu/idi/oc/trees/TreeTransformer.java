package edu.ntnu.idi.oc.trees;

import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonMatcher;

import java.io.BufferedReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Transform trees matching patterns with actions
 */
public class TreeTransformer extends TreeOperator {

    public TreeTransformer(List<TreeOperation> operations) {
        super(operations);
    }

    public TreeTransformer(Path filename) {
        super(filename);
    }

    public TreeTransformer(BufferedReader reader) {
        super(reader);
    }

    public List<Transform>
    transformTree(Tree tree) {
        List<Transform> transforms = new ArrayList<>(100);
        Transform transform;

        for (TreeOperation operation: this.getOperationsAsList()) {
            transform = transformTree(tree, operation);
            if (transform != null) {
                transforms.add(transform);
            }
        }
        return transforms;
    }

    public Transform  transformTree(Tree tree, TreeOperation operation) {
        // transform copy of tree, leaving original tree untouched
        Tree treeCopy = tree.deepCopy();
        TregexMatcher patternMatcher = operation.pattern.matcher(treeCopy);
        TsurgeonMatcher actionMatcher = operation.action.matcher();
        Transform transform = null;

        if (patternMatcher.find()) {
            actionMatcher.evaluate(treeCopy, patternMatcher);
            transform = new Transform(operation.name, treeCopy);
        }
    return transform;
    }

    public Transform transformTree(Tree tree, String operationName) {
        return transformTree(tree, getOperation(operationName));
    }

}


