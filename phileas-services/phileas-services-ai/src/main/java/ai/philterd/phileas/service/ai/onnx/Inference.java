package ai.philterd.phileas.service.ai.onnx;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Entity;

import opennlp.dl.InferenceOptions;
import opennlp.dl.namefinder.NameFinderDL;
import opennlp.tools.sentdetect.SentenceDetector;
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

    public Inference(File model, File vocab, Map<Integer, String> id2Labels, SentenceDetector sentenceDetector) throws Exception {

        this.id2Labels = id2Labels;
        this.nameFinderDL = new NameFinderDL(model, vocab, id2Labels, new InferenceOptions());

        // TODO: Change to sentence detector once supported in OpenNLP's NameFinderDL.
        //this.nameFinderDL = new NameFinderDL(model, vocab, id2Labels, sentenceDetector);

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

}
