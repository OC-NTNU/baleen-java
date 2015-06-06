package edu.ntnu.idi.oc;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class ExtractionTest {

    @Test
    public void testExtract() throws Exception {
        Extraction extraction = new Extraction();
        extraction.addExtractor("change", Paths.get("src/test/resources/transforms/change.tfm"));
        extraction.addExtractor("increase", Paths.get("src/test/resources/transforms/increase.tfm"));
        extraction.addExtractor("decrease", Paths.get("src/test/resources/transforms/decrease.tfm"));

        Path extractFile = Paths.get("src/test/resources/extractions.json");
        Files.deleteIfExists(extractFile);
        Path treesPath = Paths.get("src/test/resources/trees");
        //Path treesPath = Paths.get("/Users/work/BigData/nature/abstracts/lemmaparse");
        extraction.apply(treesPath, extractFile);

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

        Extraction.main(args);

        assert Files.exists(extractFile);
    }

}