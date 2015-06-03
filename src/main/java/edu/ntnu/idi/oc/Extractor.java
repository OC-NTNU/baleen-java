package edu.ntnu.idi.oc;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.TregexPatternCompiler;
import edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonMatcher;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonParseException;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Extraction of subtrees matching patterns
 */
public class Extractor {
    private final String label;
    private final List<Transformation> transforms;


    public Extractor(String label, Path filename) {
        this.label = label;
        transforms = getTransformationsFromFile(filename);
    }

    public String getLabel() {
        return label;
    }

    public List<Extraction>
    extract(Tree tree) {
        return transformTree(transforms, tree);
    }


    public static List<Transformation>
    getTransformationsFromFile(Path filename) {
        List<Transformation> transforms = new LinkedList<>();

        // use BufferedReader because Tsurgeon.getTregexPatternFromReader does
        try (BufferedReader reader = Files.newBufferedReader(filename, StandardCharsets.UTF_8)) {
            for (; ; ) {
                Transformation transformation = getTransformationFromReader(reader);
                if (transformation == null) {
                    break;
                }
                transforms.add(transformation);
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
        return transforms;
    }


    public static Transformation
    getTransformationFromReader(BufferedReader reader) throws IOException {
        // get name of transformation
        String transName = getTransformationNameFromReader(reader);

        // get compiled pattern for matching
        String patternString = Tsurgeon.getTregexPatternFromReader(reader);
        // System.err.println("Read tregex pattern: " + patternString);
        if (patternString.isEmpty()) {
            return null;
        }
        TregexPatternCompiler compiler = new TregexPatternCompiler();
        TregexPattern matchPattern = compiler.compile(patternString);

        // get compiled operations, possibly none
        TsurgeonPattern collectedPattern = null;
        try {
            collectedPattern = Tsurgeon.getTsurgeonOperationsFromReader(reader);
        } catch (TsurgeonParseException e) {
            if (!("No Tsurgeon operation provided.".equals(e.getMessage()))) {
                // another type of exception, e.g. parsing error
                throw e;
            }
        }
        return new Transformation(transName, matchPattern, collectedPattern);
    }


    private static String
    getTransformationNameFromReader(BufferedReader reader) throws IOException {
        // abusing method to read Tregex pattern
        String name = Tsurgeon.getTregexPatternFromReader(reader);
        name = name.replaceAll("\\W*\\$\\W*", "");
        return name;
    }


    public static List<Extraction>
    transformTree(List<Transformation> transforms,
                  Tree tree) {
        List<Extraction> extractions = new LinkedList<>();

        for (Transformation transform: transforms) {
            extractions.addAll(transformTree(transform, tree));
        }
        return extractions;
    }

    public List<Extraction>
    transformTree(Tree tree) {
        return Extractor.transformTree(this.transforms, tree);
    }


    public static List<Extraction>
    transformTree(Transformation transform,
                  Tree tree) {
        List<Extraction> extractions = new LinkedList<>();
        List<Integer> nodeNumbers = new ArrayList<>(10);

        TregexMatcher patterMatcher = transform.pattern.matcher(tree);

        TsurgeonMatcher operationMatcher = null;

        if (transform.operation != null) {
            operationMatcher = transform.operation.matcher();
        }

        // 1. get numbers of matching subtrees
        while (patterMatcher.findNextMatchingNode()) {
            Tree subTree = patterMatcher.getMatch();
            Integer nodeNumber = subTree.nodeNumber(tree);
            nodeNumbers.add(nodeNumber);
        }

        // 2. apply operations (if defined) to subtree
        int i = 0;
        patterMatcher.reset();

        while (patterMatcher.findNextMatchingNode()) {
            Tree subTree = patterMatcher.getMatch();
            if (operationMatcher != null) {
                operationMatcher.evaluate(subTree, patterMatcher);
            }

            // after operation, resulting subTree may be an invalid tree like "NP"
            Extraction ext = new Extraction(transform.name, nodeNumbers.get(i++), subTree.deepCopy());
            extractions.add(ext);
        }

        // TODO pattern matching is applied twice, which is inefficient
        // However, if done in one iteration, the node numbers become incorrect,
        // because nodes are deleted from the original input tree

        return extractions;
    }
}


class Transformation {
    public final String name;
    public final TregexPattern pattern;
    public final TsurgeonPattern operation;

    Transformation(String name, TregexPattern pattern, TsurgeonPattern operation) {
        this.name = name;
        this.pattern = pattern;
        this.operation = operation;
    }
}

/**
 * Result of applying a transformation to extract a subtree
 */
class Extraction {
    public final String transName;
    public final int nodeNumber;
    public final Tree subTree;

    Extraction(String transName, int nodeNumber, Tree subTree) {
        this.transName = transName;
        this.nodeNumber = nodeNumber;
        this.subTree = subTree;
    }
}
