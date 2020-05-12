package com.mtnfog.phileas.model.utils;

import java.util.*;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;
import org.apache.commons.lang3.NotImplementedException;

/**
 * This tokenizer uses ICU4J's unicode tokenization rules.
 */
public class UnicodeTokenizer implements Tokenizer {

    public static final UnicodeTokenizer INSTANCE = new UnicodeTokenizer();

    private UnicodeTokenizer() {
        // Use the static instance.
    }

    @Override
    public String[] tokenize(String s) {

        List<String> tokens = new LinkedList<>();

        com.ibm.icu.util.StringTokenizer stringTokenizer = new com.ibm.icu.util.StringTokenizer(s);

        while (stringTokenizer.hasMoreTokens()) {
            tokens.add(stringTokenizer.nextToken());
        }

        String[] t = new String[tokens.size()];

        return tokens.toArray(t);

    }

    @Override
    public Span[] tokenizePos(String d) {

        throw new NotImplementedException("This is not yet implemented.");

    }

}