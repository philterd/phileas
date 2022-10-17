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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Inference {

    private static final Logger LOGGER = LogManager.getLogger(Inference.class);

    private final Map<Integer, String> id2Labels;
    private final NameFinderDL nameFinderDL;

    public Inference(File model, File vocab, Map<Integer, String> id2Labels) throws Exception {

        this.id2Labels = id2Labels;
        this.nameFinderDL = new NameFinderDL(model, vocab, id2Labels);

    }

    public List<Entity> predict(final String text, final String context, final String documentId) {

        final String[] tokens = text.split(" ");

        final long startTime = System.currentTimeMillis();
        final Span[] spans = nameFinderDL.find(tokens);
        final long endTime = System.currentTimeMillis();
        LOGGER.info("Inference took {} ms. Found {} spans.", endTime - startTime, spans.length);

        final List<Entity> entities = new LinkedList<>();

        for(final Span span : spans) {

            final String spanText = (String) span.getCoveredText(text);

            // Create a span for this text.
            final Entity entity = new Entity(
                    span.getStart(),
                    span.getEnd(),
                    FilterType.PERSON,
                    context,
                    documentId,
                    spanText,
                    span.getProb());

            entities.add(entity);

        }

        return entities;

    }

    public static String findByRegex(String text, String span) {

        final String regex = span
                .replaceAll(" ", "\\\\s+")
                .replaceAll("\\)", "\\\\)")
                .replaceAll("\\(", "\\\\(");

        final long startTime = System.currentTimeMillis();
        final Span[] spans = nameFinderDL.find(tokens);
        final long endTime = System.currentTimeMillis();
        LOGGER.info("Inference took {} ms", endTime - startTime);

        if(matcher.find()) {
            return matcher.group(0);
        }

        // For some reason the regex match wasn't found. Just return the original span.
        return span;

    }

}
