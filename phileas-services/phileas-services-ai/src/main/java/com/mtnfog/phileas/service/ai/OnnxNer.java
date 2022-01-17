package com.mtnfog.phileas.service.ai;

import com.mtnfog.phileas.model.objects.Span;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;

public class OnnxNer {

    private static final Logger LOGGER = LogManager.getLogger(OnnxNer.class);

    private final Inference inference;

    public OnnxNer(File model, File vocab, final Map<Integer, String> id2Labels) throws Exception {

        this.inference = new Inference(model, vocab, id2Labels);

    }

    public List<Span> find(final String tokens, final String context, final String documentId) throws Exception {

        // Remove punctuation.
        final String tokensWithoutPunctuation = tokens.replaceAll("\\p{P}", "");

        final List<Span> spans = inference.predict(tokensWithoutPunctuation, context, documentId);

        return spans;

    }

}
