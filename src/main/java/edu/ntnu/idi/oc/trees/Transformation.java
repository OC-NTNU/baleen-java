package edu.ntnu.idi.oc.trees;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.sourceforge.argparse4j.ArgumentParsers;
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
    private final ObjectMapper mapper = new ObjectMapper();
    private JsonGenerator generator;


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

        transformation.apply(inRecords, outRecords);
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

    public  void
    apply(Path inFilename,
          Path outFilename) {

        try (
                BufferedReader reader = Files.newBufferedReader(inFilename);
                BufferedWriter writer = Files.newBufferedWriter(outFilename)
        ) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            JsonFactory factory = mapper.getFactory();
            JsonParser parser = factory.createParser(reader);
            generator = factory.createGenerator(writer);
            ObjectNode ancestorNode;

            if(parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected an array at start of Json file " + inFilename);
            }

            generator.writeStartArray();

            while(parser.nextToken() == JsonToken.START_OBJECT) {
                // read everything from this START_OBJECT to the matching END_OBJECT
                // and return it as a tree model ObjectNode
                ancestorNode = mapper.readTree(parser);

                transformTree(ancestorNode);

                // postponed writing of ancestor, because all its descendants need to added
                mapper.writeValue(generator, ancestorNode);
            }

            generator.writeEndArray();

            parser.close();
            generator.close();

        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
            System.exit(1);
        }
    }

    public void transformTree(ObjectNode ancestorNode) throws IOException{
        Tree tree = Tree.valueOf(ancestorNode.get("subTree").asText());
        ObjectNode descendantNode;
        String key, origin, subStr;

        if (ancestorNode.has("origin")) {
            origin = ancestorNode.get("origin").asText();
        } else {
            origin = ancestorNode.get("key").asText();
        }

        for (TreeTransformer transformer: transformers) {
            for (Transform transform: transformer.transformTree(tree)) {
                descendantNode = ancestorNode.deepCopy().remove(Collections.singletonList("descendants"));
                key = ancestorNode.get("key").asText() + ":" + transform.operationName;
                ancestorNode.withArray("descendants").add(key);

                descendantNode.put("key", key);
                descendantNode.put("origin", origin);
                descendantNode.put("ancestor", ancestorNode.get("key").asText());
                descendantNode.put("transformName", transform.operationName);
                descendantNode.put("subTree", transform.subTree.toString());
                subStr = PTBTokenizer.ptb2Text(Sentence.listToString(transform.subTree.yield()));
                descendantNode.put("subStr", subStr);

                transformTree(descendantNode);
                mapper.writeValue(generator, descendantNode);
            }
        }
    }

}