package edu.ntnu.idi.oc;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by work on 05/06/15.
 */
public class TransformationTest {

    @Test
    public void testTransform() throws Exception {
        Transformation transformation = new Transformation();
        transformation.addTransformer(Paths.get("src/test/resources/transforms/coordination.tfm"));

        Path inRecords = Paths.get("src/test/resources/extractions.json");
        Path outRecords = Paths.get("src/test/resources/transformed.json");
        Files.deleteIfExists(outRecords);
        //Path treesPath = Paths.get("/Users/work/BigData/nature/abstracts/lemmaparse");
        transformation.apply(inRecords, outRecords);

        assert Files.exists(outRecords);

    }

}