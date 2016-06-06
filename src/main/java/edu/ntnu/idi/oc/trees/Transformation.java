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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;


/**
 * Transformation subtrees with Tsurgeon operations
 */
public class Transformation {
    ObjectMapper mapper = new ObjectMapper();
    private JsonGenerator generator;
    private JsonFactory factory;
    private final List<TreeTransformer> transformers;
    private final static int DEFAULT_MAX_TREE_SIZE = 100;
    private final static boolean DEFAULT_RESUME = false;
    private final static String DEFAULT_TAG = "#trans";

    private static Logger log = Logger.getLogger("Transformation");


    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("transform")
                .description("Transform variables");
        parser.addArgument("varsPath")
                .metavar("IN")
                .help("file or directory containing extracted variables in JSON format");
        parser.addArgument("transDir")
                .metavar("OUT")
                .help("directory for writing extracted variables JSON format");
        parser.addArgument("transforms")
                .nargs("+")
                .metavar("TRANS")
                .help("operations file in Transformer format");
        parser.addArgument("-u", "--unique")
                .setDefault(false)
                .action(Arguments.storeTrue())
                .help("unique substrings (ommit duplicate results)");
        parser.addArgument("-m", "--max-tree-size")
                .setDefault(DEFAULT_MAX_TREE_SIZE)
                .metavar("N")
                .type(Integer.class)
                .help(String.format("skip trees with more than N nodes (default %d)", DEFAULT_MAX_TREE_SIZE));
        parser.addArgument("-r", "--resume")
                .setDefault(false)
                .action(Arguments.storeTrue())
                .help("resume process");
        parser.addArgument("-t", "--tag")
                .setDefault(DEFAULT_TAG)
                .help("filename tag (default '" + DEFAULT_TAG + "')" );

        Namespace namespace = null;
        try {
            namespace = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        Transformation transformation = new Transformation();

        for (String trans : namespace.<String>getList("transforms")) {
            transformation.addTransformer(Paths.get(trans));
        }

        Path varsPath = Paths.get(namespace.getString("varsPath"));
        Path transDir = Paths.get(namespace.getString("transDir"));
        Boolean unique = namespace.getBoolean("unique");
        int maxTreeSize = namespace.getInt("max_tree_size");
        boolean resume = namespace.getBoolean("resume");
        String tag = namespace.getString("tag");

        transformation.apply(varsPath, transDir, unique, maxTreeSize, resume, tag);
    }


    public Transformation(List<TreeTransformer> transformers) {
        this.transformers = transformers;
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        factory = mapper.getFactory();
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

    public void
    apply(Path varsPath,
          Path transDir,
          boolean unique) {
        apply(varsPath, transDir, unique, DEFAULT_MAX_TREE_SIZE, DEFAULT_RESUME, DEFAULT_TAG);
    }

    public void
    apply(Path varsPath,
          Path transDir,
          boolean unique,
          int maxTreeSize,
          boolean resume,
          String tag) {
        try {
            FileUtils.forceMkdir(transDir.toFile());

            Files.walk(varsPath)
                    .filter(Files::isRegularFile)
                    .forEach(p -> transformFile(p, transDir, unique, maxTreeSize, resume, tag));
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }

    public void
    transformFile(Path varFile,
                  Path transDir,
                  boolean unique,
                  int maxTreeSize,
                  boolean resume,
                  String tag) {
        // construct output filename
        Path transFile = Paths.get(FilenameUtils.concat(
                transDir.toString(),
                FilenameUtils.getBaseName(varFile.toString()) + tag + ".json"));

        if (resume && Files.exists(transFile)) {
            log.info("skipping existing output file " + transFile);
            return;
        }

        log.info("reading variables from " + varFile);
        log.info("writing transformed variables to " + transFile);

        try (
                BufferedReader reader = Files.newBufferedReader(varFile);
                BufferedWriter writer = Files.newBufferedWriter(transFile)
        ) {
            JsonParser parser = factory.createParser(reader);
            JsonGenerator generator = factory.createGenerator(writer);
            ObjectNode ancestorNode;
            List<ObjectNode> descendants = new ArrayList<>(500);
            Set<String> seen = unique ? new HashSet<>(500) : null;

            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected an array at start of Json file " + varFile);
            }

            generator.writeStartArray();

            while (parser.nextToken() == JsonToken.START_OBJECT) {
                // read everything from this START_OBJECT to the matching END_OBJECT
                // and return it as a tree model ObjectNode
                ancestorNode = mapper.readTree(parser);
                descendants.clear();
                if (seen != null) seen.clear();
                //log.info("Transforming original node with key " + ancestorNode.get("key").asText());
                transformTree(ancestorNode, descendants, seen, maxTreeSize);
                // postponed writing of ancestor, because all its descendants need to be added
                mapper.writeValue(generator, ancestorNode);

                for (ObjectNode descendantNode : descendants) {
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

        for (TreeTransformer transformer : transformers) {
            for (Transform transform : transformer.transformTree(tree)) {
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
