package edu.ntnu.idi.oc;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonMatcher;

import java.nio.file.Path;
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

    public List<Transform>
    transformTree(Tree tree) {
        List<Transform> transforms = new LinkedList<>();
        Tree treeCopy;

        for (TreeOperation operation: this.getOperations()) {
            // transform copy of tree, leaving original tree untouched
            treeCopy = tree.deepCopy();
            TregexMatcher patternMatcher = operation.pattern.matcher(treeCopy);
            TsurgeonMatcher actionMatcher = operation.action.matcher();

            if (patternMatcher.find()) {
                actionMatcher.evaluate(treeCopy, patternMatcher);
                transforms.add(new Transform(operation.name, treeCopy));
            }
        }
        return transforms;
    }

}


/**
 * Result of transformation, that is, result of applying an operation to a tree to transform it into another tree
 */
class Transform {
    public final String operationName;
    public final Tree subTree;

    Transform(String operationName, Tree subTree) {
        this.operationName = operationName;
        this.subTree = subTree;
    }
}