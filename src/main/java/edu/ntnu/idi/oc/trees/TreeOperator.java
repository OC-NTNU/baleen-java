package edu.ntnu.idi.oc.trees;

import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.TregexPatternCompiler;
import edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonParseException;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstract class for operations on trees.
 * A tree operation is a named pair of pattern and action.
 */
public abstract class TreeOperator {
    private final LinkedHashMap<String, TreeOperation> operations;

    public TreeOperator() {
        operations = new LinkedHashMap<>();
    }

    public TreeOperator(List<TreeOperation> operations) {
        this();
        setOperationsFromList(operations);
    }

    public TreeOperator(Path filename) {
        this();
        setOperationsFromList(readOperations(filename));
    }

    public TreeOperator(BufferedReader reader) {
        this();
        setOperationsFromList(readOperations(reader));
    }

    private void setOperationsFromList(List<TreeOperation> operations) {
        for (TreeOperation operation: operations) {
            this.operations.put(operation.name, operation);
        }
    }

    public LinkedHashMap<String, TreeOperation> getOperations() {
        return operations;
    }

    public List<TreeOperation> getOperationsAsList() {
        return new ArrayList<>(operations.values());
    }

    public TreeOperation getOperation(String name) {
        return operations.get(name);
    }

    private List<TreeOperation>
    readOperations(Path filename) {
        List<TreeOperation> operations = null;

        // use BufferedReader because Tsurgeon.getTregexPatternFromReader does
        try (BufferedReader reader = Files.newBufferedReader(filename)) {
            operations = readOperations(reader);
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
        return operations;
    }

    private List<TreeOperation>
    readOperations(BufferedReader reader) {
        List<TreeOperation> operations = new LinkedList<>();
        TreeOperation operation;

        try {
            for (; ; )  {
                operation = readOperation(reader);
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


    public TreeOperation
    readOperation(BufferedReader reader) throws IOException {
        String name = readOperationName(reader);

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
    readOperationName(BufferedReader reader) throws IOException {
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

    @Override
    public String toString() {
        String actionStr = (action == null) ? "None" : action.toString();
        return String.join("\n", name, pattern.toString(), actionStr);
    }


}