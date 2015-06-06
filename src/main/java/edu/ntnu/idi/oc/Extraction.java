package edu.ntnu.idi.oc;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * Extraction of matching subtrees from trees
 */
public class Extraction {
    private final List<TreeExtractor> extractors;
    ObjectMapper mapper = new ObjectMapper();
    private JsonGenerator generator;


    private static Logger log = Logger.getLogger("edu.ntnu.idi.oc.Extraction");

    public static void main(String[] args) throws IOException {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("Extraction")
                .description("Extraction matching subtrees from trees");
        parser.addArgument("trees")
                .metavar("TREES")
                .help("file or directory containing trees in PTB format");
        parser.addArgument("extraction")
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

        Extraction extraction = new Extraction();

        for (String pair: namespace.<String>getList("trans")) {
            String[] parts = pair.split(":", 2);
            extraction.addExtractor(parts[0], Paths.get(parts[1]));
        }

        Path treesPath = Paths.get(namespace.getString("trees"));
        Path extractFile = Paths.get(namespace.getString("extraction"));

        extraction.apply(treesPath, extractFile);
    }

    public Extraction(List<TreeExtractor> extractors) {
        this.extractors = extractors;
    }

    public Extraction() {
        this(new ArrayList<>(10));
    }

    public void addExtractor(String label, Path filename) {
        extractors.add(new TreeExtractor(label, filename));
    }

    public void apply(Path treesPath, Path extractFile) {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        JsonFactory factory = mapper.getFactory();

        try (BufferedWriter writer = Files.newBufferedWriter(extractFile)) {
            generator = factory.createGenerator(writer);
            generator.writeStartArray();

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
            int treeNumber = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                Tree tree = Tree.valueOf(line);

                if (tree == null) {
                    log.warning("Skipping ill-formed tree: " + line);
                } else {
                    extractFromTree(filename, ++treeNumber, tree);
                }
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }

    private void extractFromTree(String filename, int treeNumber, Tree tree) throws IOException {
        String key;

        for (TreeExtractor extractor: extractors) {
            for (Extract extract : extractor.extractTrees(tree)) {
                ObjectNode node = mapper.createObjectNode();
                // construct unique key
                key = String.join(":", filename,  String.valueOf(treeNumber), String.valueOf(extract.nodeNumber),
                        extract.operationName);
                node.put("key", key);
                node.put("label", extractor.getLabel());
                node.put("filename", filename);
                node.put("treeNumber", treeNumber);
                node.put("extractName", extract.operationName);
                node.put("nodeNumber", extract.nodeNumber);
                node.put("subTree", extract.subTree.toString());
                node.put("subStr", PTBTokenizer.ptb2Text(Sentence.listToString(extract.subTree.yield())));
                mapper.writeValue(generator, node);
            }
        }
    }


}