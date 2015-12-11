package edu.ntnu.idi.oc.event.var;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import edu.ntnu.idi.oc.trees.Extraction;

/**
 * Extract changing/increasing/decreasing variables
 */
public class ExtractVars {
    private static final String[] OPERATION_FILES = {
            "change:tsurgeon/extract/change.tfm",
            "decrease:tsurgeon/extract/decrease.tfm",
            "increase:tsurgeon/extract/increase.tfm"
    };

    public static void main(String[] args) throws IOException {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("ExtractVars")
                .description("Extract changing/increasing/decreasing variables");
        parser.addArgument("trees")
                .metavar("TREES")
                .help("file or directory containing trees in PTB format");
        parser.addArgument("extraction")
                .metavar("EXTRACT")
                .help("file for writing extractions in JSON format");

        Namespace namespace = null;
        try {
            namespace = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        Extraction extraction = new Extraction();

        ClassLoader cLoader = extraction.getClass().getClassLoader();

        for (String pair: OPERATION_FILES) {
            String[] parts = pair.split(":", 2);
            InputStream stream = cLoader.getResourceAsStream(parts[1]);
            extraction.addExtractor(parts[0], stream);
            stream.close();
        }

        Path treesPath = Paths.get(namespace.getString("trees"));
        Path extractFile = Paths.get(namespace.getString("extraction"));

        extraction.apply(treesPath, extractFile);
    }

}
