package com.mtnfog.phileas.services.postfilters;

import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.PostFilter;
import com.mtnfog.phileas.nlp.UnicodeTokenizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.Sequence;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

/**
 * Implementation of {@link PostFilter} that performs false positive
 * filtering by determining text parts-of-speech.
 */
public class PartOfSpeechFalsePositiveFilter extends PostFilter implements Serializable {

    private static final Logger LOGGER = LogManager.getLogger(PartOfSpeechFalsePositiveFilter.class);

    private transient POSTaggerME tagger;

    /**
     * Creates a new post filter.
     */
    public PartOfSpeechFalsePositiveFilter(POSModel model) {

        LOGGER.info("Initializing parts of speech false positive post filter...");

        this.tagger = new POSTaggerME(model);

    }

    public PartOfSpeechFalsePositiveFilter(InputStream modelIn) throws IOException {

        LOGGER.info("Initializing parts of speech false positive post filter...");

        final POSModel model = new POSModel(modelIn);
        this.tagger = new POSTaggerME(model);

    }

    @Override
    protected boolean process(String text, Span span) {

        final String spanText = span.getText(text);
        final String[] tokens = UnicodeTokenizer.INSTANCE.tokenize(text);

        final Sequence[] sequences = tagger.topKSequences(tokens);

        for(int i = 0; i < tokens.length; i++) {

            String token = tokens[i].replaceAll("\\p{Punct}", "");

            final Sequence sequence = sequences[0];

            final double[] probs = sequence.getProbs();
            final List<String> tags = sequence.getOutcomes();

            final double prob = probs[i];
            final String tag = tags.get(i);

            // LOGGER.info("Token: {}, POS: {}, Prob: {}", token, tag, prob);

            if(StringUtils.equalsIgnoreCase(token, spanText)) {

                // https://cs.nyu.edu/grishman/jet/guide/PennPOS.html

                if(tag.startsWith("NN") || tag.startsWith("NNS") || tag.startsWith("NNPS")) {

                    //LOGGER.info("Span text: {}, POS: {}", spanText, tag);

                    // TODO: PHI-117: Consider the probabilities of the POS tags.

                    // LOGGER.info("Token {} is not post-filtered by POS.", spanText);

                    return true;

                }

            }

        }

        // LOGGER.info("Token {} is post-filtered by POS.", spanText);

        return false;

    }

}
