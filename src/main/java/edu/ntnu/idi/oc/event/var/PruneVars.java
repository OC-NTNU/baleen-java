package edu.ntnu.idi.oc.event.var;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import edu.ntnu.idi.oc.trees.Transformation;

/**
 * Extract changing/increasing/decreasing variables
 */
public class PruneVars {
    private static final String[] OPERATION_FILES = {
            "tsurgeon/prune/coordination.tfm",
            "tsurgeon/prune/parenthetical.tfm",
            "tsurgeon/prune/non-restrict.tfm",
            "tsurgeon/prune/modifiers.tfm",
    };
    private final static int DEFAULT_MAX_TREE_SIZE = 100;

    public static void main(String[] args) throws IOException {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("ExtractVars")
                .description("Prune changing/increasing/decreasing variables");
        parser.addArgument("inRecords")
                .metavar("IN")
                .help("input file in JSON format");
        parser.addArgument("outRecords")
                .metavar("OUT")
                .help("output file in JSON format");
        parser.addArgument("--unique")
                .setDefault(false)
                .action(Arguments.storeTrue())
                .help("unique substrings (ommit duplicate results)");
        parser.addArgument("--max-tree-size")
                .setDefault(DEFAULT_MAX_TREE_SIZE)
                .metavar("N")
                .type(Integer.class)
                .help(String.format("skip trees with more than N nodes (default %d)", DEFAULT_MAX_TREE_SIZE));


        Namespace namespace = null;
        try {
            namespace = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        Transformation transformation = new Transformation();

        ClassLoader cLoader = transformation.getClass().getClassLoader();

        for (String file: OPERATION_FILES) {
            InputStream stream = cLoader.getResourceAsStream(file);
            transformation.addTransformer(stream);
            stream.close();
        }
        Path inRecords = Paths.get(namespace.getString("inRecords"));
        Path outRecords = Paths.get(namespace.getString("outRecords"));
        Boolean unique = namespace.getBoolean("unique");
        int maxTreeSize = namespace.getInt("max_tree_size");

        transformation.apply(inRecords, outRecords, unique, maxTreeSize);
    }

}
