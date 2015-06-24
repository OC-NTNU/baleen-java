package edu.ntnu.idi.oc.trees;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.process.PTBTokenizer;


/**
 * Transformation subtrees with Tsurgeon operations
 */
public class Transformation {
    private final List<TreeTransformer> transformers;
    private final static int DEFAULT_MAX_TREE_SIZE = 100;

    private static Logger log = Logger.getLogger("Extraction");


    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("Transformation")
                .description("Transformation variable trees");
        parser.addArgument("inRecords")
                .metavar("IN")
                .help("input file in JSON format");
        parser.addArgument("outRecords")
                .metavar("OUT")
                .help("output file in JSON format");
        parser.addArgument("transforms")
                .nargs("+")
                .metavar("TRANS")
                .help("operations file in Transformer format");
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

        for (String trans: namespace.<String>getList("transforms")) {
            transformation.addTransformer(Paths.get(trans));
        }

        Path inRecords = Paths.get(namespace.getString("inRecords"));
        Path outRecords = Paths.get(namespace.getString("outRecords"));
        Boolean unique = namespace.getBoolean("unique");
        int maxTreeSize = namespace.getInt("max_tree_size");

        transformation.apply(inRecords, outRecords, unique, maxTreeSize);
    }


    public Transformation(List<TreeTransformer> transformers) {
        this.transformers = transformers;
    }

    public Transformation() {
        this(new ArrayList<>(10));
    }

    public void addTransformer(Path filename) {
        transformers.add(new TreeTransformer(filename));
    }

    public void addTransformer(InputStream stream) throws IOException {
        transformers.add(new TreeTransformer(stream));
    }

    public  void
    apply(Path inFilename,
          Path outFilename,
          boolean unique) {
        apply(inFilename, outFilename, unique, DEFAULT_MAX_TREE_SIZE);
    }

    public  void
    apply(Path inFilename,
          Path outFilename,
          boolean unique,
          int maxTreeSize) {

        try (
                BufferedReader reader = Files.newBufferedReader(inFilename);
                BufferedWriter writer = Files.newBufferedWriter(outFilename)
        ) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            JsonFactory factory = mapper.getFactory();
            JsonParser parser = factory.createParser(reader);
            JsonGenerator generator = factory.createGenerator(writer);
            ObjectNode ancestorNode;
            List<ObjectNode> descendants = new ArrayList<>(500);
            Set<String> seen = unique ? new HashSet<>(500) : null;

            if(parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected an array at start of Json file " + inFilename);
            }

            generator.writeStartArray();

            while(parser.nextToken() == JsonToken.START_OBJECT) {
                // read everything from this START_OBJECT to the matching END_OBJECT
                // and return it as a tree model ObjectNode
                ancestorNode = mapper.readTree(parser);
                descendants.clear();
                if (seen != null) seen.clear();
                log.info("Transforming original node with key " + ancestorNode.get("key").asText());
                transformTree(ancestorNode, descendants, seen, maxTreeSize);
                // postponed writing of ancestor, because all its descendants need to be added
                mapper.writeValue(generator, ancestorNode);

                for (ObjectNode descendantNode: descendants) {
                    mapper.writeValue(generator, descendantNode);
                }
            }

            generator.writeEndArray();
            parser.close();
            generator.close();

        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
            System.exit(1);
        }
    }

    public void
    transformTree(ObjectNode ancestorNode,
                  List<ObjectNode> descendants,
                  Set<String> seen,
                  int maxTreeSize)
            throws IOException {
        Tree tree = Tree.valueOf(ancestorNode.get("subTree").asText());

        if (tree == null) {
            // transformation resulted in ill-formed tree, e.g. "NP"
            log.warning("skipping ill-formed tree: " + ancestorNode);
            return;
        }

        if (tree.size() > maxTreeSize) {
            log.warning(String.format("skipping tree because its size (%d nodes) exceeds max tree size (%d nodes)",
                    tree.size(), maxTreeSize));
            return;
        }

        ObjectNode descendantNode;
        String key, origin, subStr;

        if (ancestorNode.has("origin")) {
            origin = ancestorNode.get("origin").asText();
        } else {
            origin = ancestorNode.get("key").asText();
        }

        for (TreeTransformer transformer: transformers) {
            for (Transform transform: transformer.transformTree(tree)) {
                subStr = PTBTokenizer.ptb2Text(Sentence.listToString(transform.subTree.yield()));

                if (seen != null) {
                    if (seen.contains(subStr)) {
                        continue;
                    } else {
                        seen.add(subStr);
                    }
                }

                descendantNode = ancestorNode.deepCopy().remove(Collections.singletonList("descendants"));
                key = ancestorNode.get("key").asText() + ":" + transform.operationName;
                ancestorNode.withArray("descendants").add(key);

                descendantNode.put("key", key);
                descendantNode.put("origin", origin);
                descendantNode.put("ancestor", ancestorNode.get("key").asText());
                descendantNode.put("transformName", transform.operationName);
                descendantNode.put("subTree", transform.subTree.toString());
                descendantNode.put("subStr", subStr);

                descendants.add(descendantNode);
                transformTree(descendantNode, descendants, seen, maxTreeSize);
            }
        }
    }

}
