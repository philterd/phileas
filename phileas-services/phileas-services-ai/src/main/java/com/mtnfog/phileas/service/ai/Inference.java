package com.mtnfog.phileas.service.ai;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Entity;
import opennlp.dl.namefinder.NameFinderDL;
import opennlp.tools.util.Span;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Inference {

    private static final Logger LOGGER = LogManager.getLogger(Inference.class);

    private final Map<Integer, String> id2Labels;
    private final NameFinderDL nameFinderDL;

    public Inference(File model, File vocab, boolean doLowerCase, Map<Integer, String> id2Labels) throws Exception {

        this.id2Labels = id2Labels;
        this.nameFinderDL = new NameFinderDL(model, vocab, doLowerCase, id2Labels);

    }

    public List<Entity> predict(final String text, final String context, final String documentId) throws Exception {

        final String[] tokens = text.split(" ");

        final long startTime = System.currentTimeMillis();
        final Span[] spans = nameFinderDL.find(tokens);
        final long endTime = System.currentTimeMillis();
        LOGGER.info("Inference took {} ms", endTime - startTime);

        final List<Entity> entities = new LinkedList<>();

        for(final Span span : spans) {

            final String spanText = span.getCoveredText(text).toString();

            // The difference between the OpenNLP Span and Phileas Span
            // is that the OpenNLP span is based on token position instead of character position.

            final int characterStart = text.indexOf(spanText);
            final int characterEnd = characterStart + spanText.length();

            LOGGER.info("Span text = " + spanText);
            LOGGER.info("Span text length = " + spanText.length());
            LOGGER.info("Character start = " + characterStart);
            LOGGER.info("Character end = " + characterEnd);

            // Create a span for this text.
            final Entity entity = new Entity(
                    characterStart,
                    characterEnd,
                    FilterType.PERSON,
                    context,
                    documentId,
                    spanText,
                    span.getProb());

            entities.add(entity);

        }

        return entities;

    }

}
