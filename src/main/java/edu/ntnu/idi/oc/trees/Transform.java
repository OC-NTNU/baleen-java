package edu.ntnu.idi.oc.trees;

import edu.stanford.nlp.trees.Tree;

/**
 * Result of transformation, that is, result of applying an operation to a tree to transform it into another tree
 */
public class Transform {
    public final String operationName;
    public final Tree subTree;

    Transform(String operationName, Tree subTree) {
        this.operationName = operationName;
        this.subTree = subTree;
    }
}
