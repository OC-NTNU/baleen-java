package edu.ntnu.idi.oc;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.TregexPatternCompiler;
import edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonMatcher;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;
import edu.stanford.nlp.util.Triple;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tsurgeon-like class with named operations
 *
 * TODO: write doc
 */
public class Tsurgeon3 {
    // cannot extend Tsurgeon class because it has a private constructor and static methods, so delegate

    public static List<Triple<String, TregexPattern, TsurgeonPattern>>
    getOperationsFromFile(List<Path> filenames) throws IOException {
        List<Triple<String, TregexPattern, TsurgeonPattern>> ops = new ArrayList<>();
        {
            for (Path filename : filenames) {
                List<Triple<String, TregexPattern, TsurgeonPattern>> triples = getOperationsFromFile(filename);
                ops.addAll(triples);
            }
        }
        return ops;
    }


    public static List<Triple<String, TregexPattern, TsurgeonPattern>>
    getOperationsFromFile(Path filename) throws IOException {
        List<Triple<String, TregexPattern, TsurgeonPattern>> operations;
        // use BufferedReader because Tsurgeon.getTregexPatternFromReader does
        try (BufferedReader reader = Files.newBufferedReader(filename, StandardCharsets.UTF_8)) {
             operations = getOperationsFromReader(reader);
        }
        return operations;
    }


    public static List<Triple<String, TregexPattern, TsurgeonPattern>>
    getOperationsFromReader(BufferedReader reader) throws IOException {
        List<Triple<String, TregexPattern, TsurgeonPattern>> operations = new ArrayList<>();
        for (; ; ) {
            Triple<String, TregexPattern, TsurgeonPattern> operation = getOperationFromReader(reader);
            if (operation == null) {
                break;
            }
            operations.add(operation);
        }
        return operations;
    }


    public static Triple<String, TregexPattern, TsurgeonPattern>
    getOperationFromReader(BufferedReader reader) throws IOException {
        // get name of transformation
        String transName = getNameFromReader(reader);

        // get compiled pattern for matching
        String patternString = Tsurgeon.getTregexPatternFromReader(reader);
        // System.err.println("Read tregex pattern: " + patternString);
        if (patternString != null && patternString.isEmpty()) {
            return null;
        }
        TregexPatternCompiler compiler = new TregexPatternCompiler();
        TregexPattern matchPattern = compiler.compile(patternString);

        // get compiled operations
        TsurgeonPattern collectedPattern = Tsurgeon.getTsurgeonOperationsFromReader(reader);

        return new Triple<>(transName, matchPattern,collectedPattern);
    }

    public static String
    getNameFromReader(BufferedReader reader) throws IOException {
        // abusing method to read Tregex pattern
        String name = Tsurgeon.getTregexPatternFromReader(reader);
        name = name.replaceAll("\\W*\\$\\W*", "");
        return name;
    }

    public static Tree
    processPattern(TregexPattern matchPattern,
                   TsurgeonPattern p,
                   Tree t) {
        return Tsurgeon.processPattern(matchPattern, p, t);
    }

    /**
     * New method that applies the pattern just once, whereas processPattern applies it exhaustively.
     */
    public static Tree
    processPatternOnce(TregexPattern matchPattern,
                       TsurgeonPattern p,
                       Tree t) {
        TregexMatcher m = matchPattern.matcher(t);
        TsurgeonMatcher tsm = p.matcher();
        if (m.find()) {
            t = tsm.evaluate(t, m);
        }
        return t;
    }

}
