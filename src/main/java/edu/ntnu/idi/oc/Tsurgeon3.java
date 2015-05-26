package edu.ntnu.idi.oc;

import edu.stanford.nlp.io.IOUtils;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Tsurgeon with named operations
 *
 * TODO: write doc
 */
public class Tsurgeon3 {
    // cannot extend Tsurgeon class because it has a private constructor, so delegate

    public static List<Triple<String, TregexPattern, TsurgeonPattern>> getOperationsFromFile(List<String> filenames, String encoding, TregexPatternCompiler compiler) throws IOException {
        List<Triple<String, TregexPattern, TsurgeonPattern>> ops = new ArrayList<>();
        {
            for (String filename : filenames) {
                List<Triple<String, TregexPattern, TsurgeonPattern>> triples = getOperationsFromFile(filename, encoding, compiler);
                ops.addAll(triples);
            }
        }
        return ops;
    }


    public static List<Triple<String, TregexPattern, TsurgeonPattern>> getOperationsFromFile(String filename, String encoding, TregexPatternCompiler compiler) throws IOException {
        BufferedReader reader = IOUtils.readerFromString(filename, encoding);
        List<Triple<String, TregexPattern,TsurgeonPattern>> operations = getOperationsFromReader(reader, compiler);
        reader.close();
        return operations;
    }


    public static List<Triple<String, TregexPattern, TsurgeonPattern>> getOperationsFromReader(BufferedReader reader, TregexPatternCompiler compiler) throws IOException {
        List<Triple<String, TregexPattern,TsurgeonPattern>> operations = new ArrayList<>();
        for ( ; ; ) {
            Triple<String, TregexPattern, TsurgeonPattern> operation = getOperationFromReader(reader, compiler);
            if (operation == null) {
                break;
            }
            operations.add(operation);
        }
        return operations;
    }


    public static Triple<String, TregexPattern, TsurgeonPattern> getOperationFromReader(BufferedReader reader, TregexPatternCompiler compiler) throws IOException {
        String opName = getNameFromReader(reader);
        String patternString = Tsurgeon.getTregexPatternFromReader(reader);
        // System.err.println("Read tregex pattern: " + patternString);
        if (patternString != null && patternString.isEmpty()) {
            return null;
        }
        TregexPattern matchPattern = compiler.compile(patternString);

        TsurgeonPattern collectedPattern = Tsurgeon.getTsurgeonOperationsFromReader(reader);
        return new Triple<>(opName, matchPattern,collectedPattern);
    }


    public static String getNameFromReader(BufferedReader reader) throws IOException {
        // abusing method to read Tregex pattern
        String name = Tsurgeon.getTregexPatternFromReader(reader);
        name = name.replaceAll("\\W*\\$\\W*", "");
        return name;
    }

    public static Tree processPattern(TregexPattern matchPattern, TsurgeonPattern p, Tree t) {
        return Tsurgeon.processPattern(matchPattern, p, t);
    }

    /**
     * New method that applies the pattern just once, whereas processPattern applies it exhaustively.
     */
    public static Tree processPatternOnce(TregexPattern matchPattern, TsurgeonPattern p, Tree t) {
        TregexMatcher m = matchPattern.matcher(t);
        TsurgeonMatcher tsm = p.matcher();
        if (m.find()) {
            t = tsm.evaluate(t, m);
        }
        return t;
    }

}
