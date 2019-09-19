package com.mtnfog.test.phileas.nlp;

import com.mtnfog.phileas.nlp.UnicodeTokenizer;
import org.junit.Assert;
import org.junit.Test;

public class UnicodeTokenizerTest {

    @Test
    public void testTokenizerTest() {

        final String[] tokenizedText = UnicodeTokenizer.INSTANCE.tokenize("The quick brown fox jumps over the lazy dog.");

        final String[] tokens = new String[] { "The", "quick", "brown", "fox",
                "jumps", "over", "the", "lazy", "dog." };

        Assert.assertArrayEquals(tokenizedText, tokens);

    }

}