package edu.ntnu.idi.oc;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;
import edu.stanford.nlp.util.Triple;


/**
 * Transform subtrees with Tsurgeon operations
 */
public class Transform {

    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("Transform")
                .description("Transform variable trees");
        parser.addArgument("inRecords")
                .metavar("IN")
                .help("input file in JSON format");
        parser.addArgument("outRecords")
                .metavar("OUT")
                .help("output file in JSON format");
        parser.addArgument("transforms")
                .nargs("+")
                .metavar("TRANS")
                .help("transformations file in Tsurgeon3 format");

        Namespace namespace = null;
        try {
            namespace = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        Path inFilename = Paths.get(namespace.getString("inRecords"));
        Path outFilename = Paths.get(namespace.getString("outRecords"));
        List<String> transStrings = namespace.<String>getList("transforms");
        List<Path> transFilenames = new ArrayList<>();
        for (String trans: transStrings) {
            transFilenames.add(Paths.get(trans));
        }

        transform(inFilename, outFilename, transFilenames);
    }


    public static void
    transform(Path inFilename,
              Path outFilename,
              List<Path> transformFilenames) {
        List<Map<String, Object>> records = readRecords(inFilename);

        List<Triple<String, TregexPattern, TsurgeonPattern>> trans;
        trans = readTransformations(transformFilenames);

        applyTransformationsExhaustively(records, trans);

        writeRecords(records, outFilename);
    }


    public static void
    applyTransformationsExhaustively(List<Map<String, Object>> records,
                                     List<Triple<String,
                                             TregexPattern,
                                             TsurgeonPattern>> trans) {
        Integer newIndex = prepareRecords(records);
        List<Map<String, Object>> transRecords = applyTransformationsOnce(records, trans, newIndex);
        //System.out.println(transRecords.size());

        // keep on transforming output until nothing changes
        while ((transRecords.size() > 0)) {
            records.addAll(transRecords);
            newIndex += transRecords.size();
            transRecords = applyTransformationsOnce(transRecords, trans, newIndex);
        }
    }


    private static List<Map<String, Object>>
    applyTransformationsOnce(List<Map<String, Object>> records,
                             List<Triple<String, TregexPattern,
                                     TsurgeonPattern>> trans, Integer newIndex) {
        List<Map<String, Object>> transRecords = new ArrayList<>();

        for (Object obj: records) {
            Map record = (Map) obj;
            String lbs = (String) record.get("subtree");
            Tree orgTree = Tree.valueOf(lbs);

            if (orgTree == null) {
                continue;
            }

            for (Triple triple: trans) {
                TregexPattern tregexPat = (TregexPattern) triple.second;
                TsurgeonPattern tsurgeonPat = (TsurgeonPattern) triple.third;
                Tree tree = orgTree.deepCopy();
                Tree transTree = Tsurgeon3.processPatternOnce(tregexPat, tsurgeonPat, tree);

                if (transTree.equals(orgTree)) {
                    continue;
                }

                // copy pat_name, label, file, rel_tree_n, node_n, origin (if it exists) and
                // any other fields from ancestor record
                Map<String, Object> newRecord = new HashMap<>(record);

                // now update fields that are differ for descendant
                newRecord.put("trans_name", triple.first);
                newRecord.put("index", ++newIndex);
                newRecord.put("ancestor", record.get("index"));
                newRecord.put("descendants", new ArrayList<Integer>());
                newRecord.put("subtree", transTree.toString());
                String substr = PTBTokenizer.ptb2Text(Sentence.listToString(transTree.yield()));
                newRecord.put("substr", substr);

                if (!newRecord.containsKey("origin")) {
                    // this is the first transformation, so origin is same as ancestor
                    newRecord.put("origin",  record.get("index"));
                }

                // add to descendants of ancestor
                // cast to list, because "descendants" may have been present in the input
                List<Integer> descendants = (List<Integer>) record.get("descendants");
                descendants.add(newIndex);

                transRecords.add(newRecord);
            }
        }
        return transRecords;
    }


    private static Integer
    prepareRecords(List<Map<String, Object>> records) {
        // find max index and add "descendants" fields
        Integer newIndex = 0;

        for (Map<String, Object> record: records) {
            Integer index = (Integer) record.get("index");
            newIndex = Math.max(newIndex, index);
            if (!record.containsKey("descendants")) {
                record.put("descendants", new ArrayList<Integer>());
            }
        }
        return newIndex;
    }


    public static List<Map<String, Object>>
    readRecords(Path fileName)  {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String,Object>> records = null;

        try {
            // See http://wiki.fasterxml.com/JacksonInFiveMinutes "Data bindings with generics"
            records = mapper.readValue(fileName.toFile(), new TypeReference<List<Map<String,Object>>>() { });
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return records;
    }


    public static void
    writeRecords(List<Map<String, Object>> records,
                 Path fileName) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            mapper.writeValue(fileName.toFile(), records);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    public static List<Triple<String, TregexPattern, TsurgeonPattern>>
    readTransformations(List<Path> filenames ) {
        // use simple data binding instead of full data binding to POJO
        // because the number of fields in the records is unknown
        List<Triple<String, TregexPattern, TsurgeonPattern>> trans = null;

        try {
            trans = Tsurgeon3.getOperationsFromFile(filenames);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return trans;
    }

}
