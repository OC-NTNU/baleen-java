package edu.ntnu.idi.oc.trees;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonMatcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Extract of subtrees matching patterns
 */
public class TreeExtractor extends TreeOperator {
    private final String label;

    public TreeExtractor(String label, List<TreeOperation> operations) {
        super(operations);
        this.label = label;
    }

    public TreeExtractor(String label, Path filename) {
        super(filename);
        this.label = label;
    }

    public TreeExtractor(String label, InputStream stream) throws IOException {
        super(stream);
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public List<Extract>
    extractTrees(Tree tree) {
        List<Extract> extracts = new ArrayList<>(25);

        for (TreeOperation operation: this.getOperationsAsList()) {
            // take a deep copy, so the tree is not modified by next operation
            extractTrees(operation, tree.deepCopy(), extracts);
        }
        return extracts;
    }

    private void
    extractTrees(TreeOperation operation,
                 Tree tree,
                 List<Extract> extracts) {
        /* Pattern matching is applied twice, which is inefficient, as are the deep copies.
           However, if done in one iteration, the node numbers become incorrect,
           because nodes are deleted from the original input tree. */

        Integer nodeNumber = null;
        Tree subTree = null;
        List<Integer> nodeNumbers = new ArrayList<>(5);

        TregexMatcher patternMatcher = operation.pattern.matcher(tree);

        // 1. get numbers of matching subtrees
        while (patternMatcher.findNextMatchingNode()) {
            subTree = patternMatcher.getMatch();
            nodeNumber = subTree.nodeNumber(tree);
            nodeNumbers.add(nodeNumber);
        }

        // 2. apply operations (if defined) to subtree
        nodeNumber = 0;
        patternMatcher.reset();
        TsurgeonMatcher actionMatcher = (operation.action != null) ? operation.action.matcher() : null;

        while (patternMatcher.findNextMatchingNode()) {
            subTree = patternMatcher.getMatch();
            if (actionMatcher != null) {
                actionMatcher.evaluate(subTree, patternMatcher);
            }
            // make a copy, so the subtree is not modified by subsequent applications of this operation
            Extract ext = new Extract(operation.name, nodeNumbers.get(nodeNumber++), subTree.deepCopy());
            extracts.add(ext);
        }

    }
}



/**
 * Result of extraction, that is, result of applying an operation to a tree to extract a subtree
 */
class Extract {
    public final String operationName;
    public final int nodeNumber;
    public final Tree subTree;

    Extract(String operationName, int nodeNumber, Tree subTree) {
        this.operationName = operationName;
        this.nodeNumber = nodeNumber;
        this.subTree = subTree;
    }
}
