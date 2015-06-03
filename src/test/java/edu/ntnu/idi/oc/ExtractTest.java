package edu.ntnu.idi.oc;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;


public class ExtractTest {

    @Test
    public void testExtract() throws Exception {
        Extract extract = new Extract();
        extract.addExtractor("change", Paths.get("src/test/resources/transforms/change.tfm"));
        extract.addExtractor("increase", Paths.get("src/test/resources/transforms/increase.tfm"));
        extract.addExtractor("decrease", Paths.get("src/test/resources/transforms/decrease.tfm"));

        Path extractFile = Paths.get("src/test/resources/extractions.json");
        Files.deleteIfExists(extractFile);
        Path treesPath = Paths.get("src/test/resources/trees");
        //Path treesPath = Paths.get("/Users/work/BigData/nature/abstracts/lemmaparse");
        extract.apply(treesPath, extractFile);

        assert Files.exists(extractFile);

    }

    @Test
    public void testMain() throws Exception {
        String[] args = {
                "src/test/resources/trees",

                "src/test/resources/extractions.json",

                "change:src/test/resources/transforms/change.tfm",
                "increase:src/test/resources/transforms/increase.tfm",
                "decrease:src/test/resources/transforms/decrease.tfm"
        };

        Path extractFile = Paths.get(args[1]);
        Files.deleteIfExists(extractFile);

        Extract.main(args);

        assert Files.exists(extractFile);
    }

}