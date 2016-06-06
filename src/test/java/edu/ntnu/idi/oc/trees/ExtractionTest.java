package edu.ntnu.idi.oc.trees;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;


public class ExtractionTest {

    @Test
    public void testExtract() throws Exception {
        Extraction extraction = new Extraction();
        extraction.addExtractor("change", Paths.get("src/main/resources/tsurgeon/extract/change.tfm"));
        extraction.addExtractor("increase", Paths.get("src/main/resources/tsurgeon/extract//increase.tfm"));
        extraction.addExtractor("decrease", Paths.get("src/main/resources/tsurgeon/extract/decrease.tfm"));

        Path extractDir = Paths.get("src/test/out/ext");

        if (Files.exists(extractDir)) {
            FileUtils.cleanDirectory(extractDir.toFile());
        }
        Path treesPath = Paths.get("src/test/resources/trees");

        extraction.apply(treesPath, extractDir, false);

        assert Files.exists(extractDir);

    }

    @Test
    public void testMain() throws Exception {
        String[] args = {
                "src/test/resources/trees",

                "src/test/out/ext",

                "change:src/main/resources/tsurgeon/extract/change.tfm",
                "increase:src/main/resources/tsurgeon/extract/increase.tfm",
                "decrease:src/main/resources/tsurgeon/extract/decrease.tfm"
        };

        Path extractDir = Paths.get(args[1]);

        if (Files.exists(extractDir)) {
            FileUtils.cleanDirectory(extractDir.toFile());
        }

        Extraction.main(args);

        assert Files.exists(extractDir);
    }

}