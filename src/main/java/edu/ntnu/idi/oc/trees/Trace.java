package edu.ntnu.idi.oc.trees;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import edu.stanford.nlp.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trace transformation history
 */
public class Trace {
    private List<ObjectNode> origins;
    private Map<String,ObjectNode> descendants;
    private final int INDENT = 4;
    private final String TRANS_TEMPLATE = "%s=== %s ===>";

    public static void main(String[] args) {

        ArgumentParser parser = ArgumentParsers.newArgumentParser("Trace")
                .description("Trace transformation history");
        parser.addArgument("records")
                .metavar("RECORDS")
                .help("results of transformation in JSON format");

        Namespace namespace = null;
        try {
            namespace = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        Path recordsPath = Paths.get(namespace.getString("records"));
        new Trace(recordsPath);
    }

    public Trace(Path recordsFilename) {
        readRecords(recordsFilename);
        writeTrace();
    }

    public void writeTrace() {
        for (ObjectNode orgNode: origins) {
            System.out.println("key = " + orgNode.get("key").asText());
            traceNode(orgNode, 0);
            System.out.println();
        }
    }

    public void traceNode(ObjectNode ancestorNode, int depth) {
        System.out.print(StringUtils.repeat(' ', depth * INDENT));
        System.out.println(ancestorNode.get("subStr").asText());
        String transIndent = StringUtils.repeat(' ', (depth + 1) * INDENT/2);

        for (JsonNode jsonNode: ancestorNode.path("descendants")) {
            String key = jsonNode.asText();
            ObjectNode descendantNode = descendants.get(key);
            String transformName = descendantNode.get("transformName").asText();
            System.out.println(String.format(TRANS_TEMPLATE, transIndent, transformName));
            traceNode(descendantNode, depth + 1);
        }
    }

    public void readRecords(Path recordsPath) {
        origins = new ArrayList<>(1000);
        descendants = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(recordsPath)) {
            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = mapper.getFactory();
            JsonParser parser = factory.createParser(reader);
            ObjectNode node;

            if(parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected an array at start of Json file " + recordsPath);
            }

            while(parser.nextToken() == JsonToken.START_OBJECT) {
                node = mapper.readTree(parser);
                if (node.has("origin")) {
                    // an node derived from an original node
                    descendants.put(node.get("key").asText(), node);
                } else if (node.has("descendants")) {
                    // an original node with descendants
                    origins.add(node);
                }
            }
            parser.close();

        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
            System.exit(1);
        }
    }
}
