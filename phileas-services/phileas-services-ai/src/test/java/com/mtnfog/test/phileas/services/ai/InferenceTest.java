package com.mtnfog.test.phileas.services.ai;

import com.mtnfog.phileas.service.ai.Inference;
import org.junit.Assert;
import org.junit.Test;

public class InferenceTest {

    @Test
    public void findByRegex1() {

        final String text = "George Washington was president.";
        final String span = "George Washington";

        final String result = Inference.findByRegex(text, span);

        Assert.assertEquals("George Washington", result);

    }

    @Test
    public void findByRegex2() {

        final String text = "George     Washington was president.";
        final String span = "George Washington";

        final String result = Inference.findByRegex(text, span);

        Assert.assertEquals("George     Washington", result);

    }

    @Test
    public void findByRegex3() {

        final String text = "George Washi)ngton was president.";
        final String span = "George Washi)ngton";

        final String result = Inference.findByRegex(text, span);

        Assert.assertEquals("George Washi)ngton", result);

    }

    @Test
    public void findByRegex4() {

        final String text = "George Washi(ngton was president.";
        final String span = "George Washi(ngton";

        final String result = Inference.findByRegex(text, span);

        Assert.assertEquals("George Washi(ngton", result);

    }

}
