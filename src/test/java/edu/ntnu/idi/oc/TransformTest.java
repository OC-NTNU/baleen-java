package edu.ntnu.idi.oc;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests of Transform
 */
public class TransformTest {

    private static String resourcesPath = "src/test/resources";
    private static Path inFilename = Paths.get(resourcesPath, "input.json");
    private static Path outFilename = Paths.get(resourcesPath, "output.json");
    private static List<Path> transFilenames = Arrays.asList(Paths.get(resourcesPath, "trans.tfm"));
    private static List<Map<String, Object>> outRecords;

    @BeforeClass
    public static void beforeClass() throws IOException {
        Files.deleteIfExists(outFilename);
        Transform.transform(inFilename, outFilename, transFilenames);
        outRecords = Transform.readRecords(outFilename);
    }

    @Test
    public void testSize() {
        assertEquals(outRecords.size(), 7);
    }

    @Test
    public void testOrigin() {
        // 2 <- 5
        assertEquals((int) outRecords.get(2).get("index"),
                     (int) outRecords.get(5).get("origin"));

        // 2 <- 5 <- 6
        assertEquals((int) outRecords.get(2).get("index"),
                     (int) outRecords.get(6).get("origin"));
    }

    @Test
    public void testAncestor() {
        // 2 <- 5
        assertEquals((int) outRecords.get(2).get("index"),
                      (int) outRecords.get(5).get("ancestor"));

        // 5 <- 6
        assertEquals((int) outRecords.get(5).get("index"),
                     (int) outRecords.get(6).get("ancestor"));
    }

    @Test
    public void testDescendants() {
        // 2 -> 5
        Map record = (Map) outRecords.get(2);
        List<Integer> actual = (List<Integer>) record.get("descendants");
        List<Integer> expected = new ArrayList<>();
        expected.add(5);
        assertEquals(actual, expected);

        // 5 -> 6
        record = (Map) outRecords.get(5);
        actual = (List<Integer>) record.get("descendants");
        expected = new ArrayList<>();
        expected.add(6);
        assertEquals(actual, expected);
    }

    @Test
    public void testSubtree() {
        // (NP (JJ other) (JJ low-light) (NNS ecotype)) -> (NP (JJ low-light) (NNS ecotype))
        assertEquals(outRecords.get(5).get("subtree"),
                "(NP (JJ low-light) (NNS ecotype))");

        // (NP (JJ low-light) (NNS ecotype)) -> (NP (JJ (NNS ecotype))
        assertEquals(outRecords.get(6).get("subtree"),
                "(NP (NNS ecotype))");
    }

    @Test
    public void testSubstr() {
        // other low-light ecotype -> low-light ecotype
        assertEquals(outRecords.get(5).get("substr"),
                     "low-light ecotype");

        // low-light ecotype -> ecotype
        assertEquals(outRecords.get(6).get("substr"),
                     "ecotype");
    }
}