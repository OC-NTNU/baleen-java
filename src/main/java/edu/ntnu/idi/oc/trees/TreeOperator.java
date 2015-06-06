package edu.ntnu.idi.oc.trees;

import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.TregexPatternCompiler;
import edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonParseException;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstract class for operations on trees.
 * A tree operation is a named pair of pattern and action.
 */
public abstract class TreeOperator {
    private final List<TreeOperation> operations;


    public TreeOperator(List<TreeOperation> operations) {
        this.operations = operations;
    }

    public TreeOperator(Path filename) {
        operations = getOperationsFromFile(filename);
    }

    public List<TreeOperation> getOperations() {
        return operations;
    }

    private List<TreeOperation>
    getOperationsFromFile(Path filename) {
        List<TreeOperation> operations = new LinkedList<>();

        // use BufferedReader because Tsurgeon.getTregexPatternFromReader does
        try (BufferedReader reader = Files.newBufferedReader(filename, StandardCharsets.UTF_8)) {
            for (; ; ) {
                TreeOperation operation = getOperationFromReader(reader);
                if (operation == null) {
                    break;
                }
                operations.add(operation);
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
        return operations;
    }

    private TreeOperation
    getOperationFromReader(BufferedReader reader) throws IOException {
        String name = getOperationNameFromReader(reader);

        // get compiled pattern for matching
        String patternString = Tsurgeon.getTregexPatternFromReader(reader);
        if (patternString.isEmpty()) {
            return null;
        }
        TregexPatternCompiler compiler = new TregexPatternCompiler();
        TregexPattern pattern = compiler.compile(patternString);

        // get compiled actions, possibly none
        TsurgeonPattern action = null;
        try {
            action = Tsurgeon.getTsurgeonOperationsFromReader(reader);
        } catch (TsurgeonParseException e) {
            if (!("No Tsurgeon operation provided.".equals(e.getMessage()))) {
                // another type of exception, e.g. parsing error
                throw e;
            }
        }
        return new TreeOperation(name, pattern, action);
    }

    private String
    getOperationNameFromReader(BufferedReader reader) throws IOException {
        // abusing method to read Tregex pattern
        String name = Tsurgeon.getTregexPatternFromReader(reader);
        name = name.replaceAll("\\W*\\$\\W*", "");
        return name;
    }


}

class TreeOperation {
    public final String name;
    public final TregexPattern pattern;
    public final TsurgeonPattern action;

    TreeOperation(String name, TregexPattern pattern, TsurgeonPattern action) {
        this.name = name;
        this.pattern = pattern;
        this.action = action;
    }
}