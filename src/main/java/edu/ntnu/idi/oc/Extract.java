package edu.ntnu.idi.oc;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.Tree;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * Extract matching subtrees from trees
 */
public class Extract {
    private final List<Extractor> extractors;
    private JsonGenerator generator;

    private static Logger log = Logger.getLogger("edu.ntnu.idi.oc.Extract");

    public static void main(String[] args) throws IOException {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("Extract")
                .description("Extract matching subtrees from trees");
        parser.addArgument("trees")
                .metavar("TREES")
                .help("file or directory containing trees in PTB format");
        parser.addArgument("extract")
                .metavar("EXTRACT")
                .help("file for writing extractions in JSON format");
        parser.addArgument("trans")
                .nargs("+")
                .metavar("LABEL:TRANS")
                .help("pair of a label and file with transformations in named Tsurgeon format");

        Namespace namespace = null;
        try {
            namespace = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        Extract extract = new Extract();

        for (String pair: namespace.<String>getList("trans")) {
            String[] parts = pair.split(":", 2);
            extract.addExtractor(parts[0], Paths.get(parts[1]));
        }

        Path treesPath = Paths.get(namespace.getString("trees"));
        Path extractFile = Paths.get(namespace.getString("extract"));
        extract.apply(treesPath, extractFile);
    }


    public Extract(List<Extractor> extractors) {
        this.extractors = extractors;
    }

    public Extract() {
        this(new ArrayList<>(10));
    }

    public void addExtractor(String label, Path filename) {
        extractors.add(new Extractor(label, filename));
    }

    public void apply(Path treesPath, Path extractFile) {
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();

        try (BufferedWriter writer = Files.newBufferedWriter(extractFile)) {
            generator = factory.createGenerator(writer);
            generator.writeStartArray();
            generator.writeRaw("\n");

            Files.walk(treesPath)
                    .filter(Files::isRegularFile)
                    .forEach(this::extractFromFile);

            generator.writeEndArray();
            generator.close();
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }


    private void extractFromFile(Path treeFile) {
        log.info(treeFile.toString());

        try (BufferedReader reader = Files.newBufferedReader(treeFile)) {
            String filename = treeFile.getFileName().toString();
            int treeCount = 1;
            String line;

            while ((line = reader.readLine()) != null) {
                extractFromTree(filename, treeCount, line);
                treeCount++;
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }

    private void extractFromTree(String filename, int treeCount, String line) throws IOException {
        Tree tree = Tree.valueOf(line);

        if (tree == null) {
            log.warning("Skipping ill-formed tree: " + line);
            return;
        }

        String subStr, subTreeStr;


        for (Extractor extractor: extractors) {
            List<Extraction> extractions = extractor.extract(tree);

            for (Extraction extraction : extractions) {

                subTreeStr = extraction.subTree.toString();
                subStr = PTBTokenizer.ptb2Text(Sentence.listToString(extraction.subTree.yield()));

                // TODO Get pretty print output
                // mapper.enable(SerializationFeature.INDENT_OUTPUT) has no effect
                generator.writeStartObject();
                generator.writeRaw("\n");
                generator.writeStringField("label", extractor.getLabel());
                generator.writeRaw("\n");
                generator.writeStringField("filename", filename);
                generator.writeRaw("\n");
                generator.writeNumberField("treeCount", treeCount);
                generator.writeRaw("\n");
                generator.writeStringField("transName", extraction.transName);
                generator.writeRaw("\n");
                generator.writeNumberField("nodeNumber", extraction.nodeNumber);
                generator.writeRaw("\n");
                generator.writeStringField("subTree", subTreeStr);
                generator.writeRaw("\n");
                generator.writeStringField("subStr", subStr);
                generator.writeRaw("\n");
                generator.writeEndObject();
                generator.writeRaw("\n");
            }
        }
    }


}