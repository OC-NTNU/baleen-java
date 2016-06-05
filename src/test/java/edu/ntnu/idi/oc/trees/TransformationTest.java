package edu.ntnu.idi.oc.trees;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

/**
 * Created by work on 05/06/15.
 */
public class TransformationTest {

    @Test
    public void testTransform() throws Exception {
        Transformation transformation = new Transformation();
        transformation.addTransformer(Paths.get("src/main/resources/tsurgeon/prune/coordination.tfm"));

        Path varRecords = Paths.get("src/test/resources/vars");
        Path transDir = Paths.get("src/test/out/trans");
        if (Files.exists(transDir)) {
            FileUtils.cleanDirectory(transDir.toFile());
        };
        transformation.apply(varRecords, transDir,false);

        assert Files.exists(transDir);

    }

}