package edu.ntnu.idi.oc.trees.prune;

import org.junit.Test;


public class ModifiersTest extends PruneTest {

    public void setUp() throws Exception {
        transformationsFile = "tsurgeon/prune/modifiers.tfm";
        super.setUp();
    }

    @Test
    public void test_Strip_PP_1() throws Exception {
        testTreeOperation("Strip PP",
                "(ROOT (NP (NN bird) (PP (P with) (NP feathers))))",
                "(ROOT (NP (NN bird)))");
    }

    @Test
    public void test_Strip_PP_2() throws Exception {
        // strip deepest PP
        testTreeOperation("Strip PP",
                "(ROOT (NP (NN bird) (PP (P with) (NP (NN feathers) (PP without color))))))",
                "(ROOT (NP (NN bird) (PP (P with) (NP (NN feathers)))))");
    }
    @Test
    public void test_Strip_PP_3() throws Exception {
        // don't strip PP in coordination
        testTreeOperation("Strip PP",
                "(ROOT (NP (NN bird) (PP (PP (P with) (NP tail)) (CC and) (PP (P without) (NP colour)))))",
                null);
    }


    @Test
    public void test_Strip_Premodifier_1() throws Exception {
        testTreeOperation("Strip Premodifier",
                "(ROOT (NP (JJ blue) (NN bird)))",
                "(ROOT (NP (NN bird)))");
    }

    @Test
    public void test_Strip_Premodifier_2() throws Exception {
        // no match
        testTreeOperation("Strip Premodifier",
                "(ROOT (NP (NN bird)))",
                null);
    }

    @Test
    public void test_Strip_Premodifier_3() throws Exception {
        // match only once
        testTreeOperation("Strip Premodifier",
                "(ROOT (NP (JJ small) (JJ blue) (NN bird)))",
                "(ROOT (NP (JJ blue) (NN bird)))");
    }

    @Test
    public void test_Strip_Premodifier_4() throws Exception {
        // no match in coordination
        testTreeOperation("Strip Premodifier",
                "(ROOT (NP (JJ small) (CC and) (JJ blue) (NN bird)))",
                null);
    }

    @Test
    public void test_Strip_SBAR_1() throws Exception {
        testTreeOperation("Strip SBAR",
                "(ROOT (NP (NN bird) (SBAR that sings)))",
                "(ROOT (NP (NN bird)))");
    }

    @Test
    public void test_Strip_SBAR_2() throws Exception {
        // strip deepest SBAR
        testTreeOperation("Strip SBAR",
                "(ROOT (NP (NN bird) (SBAR that eats (NP insects (SBAR that fly)))))",
                "(ROOT (NP (NN bird) (SBAR that eats (NP insects))))");
    }

    @Test
    public void test_Strip_SBAR_3() throws Exception {
        // no match with non-restrictive clause
        testTreeOperation("Strip SBAR",
                "(ROOT (NP (NN bird) (, ,) (SBAR which sings)))",
                null);
    }

}
