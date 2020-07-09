package com.mtnfog.phileas.services.split;

import com.mtnfog.phileas.model.services.SentenceDetector;
import com.mtnfog.phileas.model.services.SplitService;
import com.mtnfog.philter.services.nlp.sentence.SegmentSentenceDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class SentenceRuleBasedSplitService extends AbstractSplitService implements SplitService {

    private static final Logger LOGGER = LogManager.getLogger(SentenceRuleBasedSplitService.class);
    private SentenceDetector sentenceDetector;

    public SentenceRuleBasedSplitService() {

        this.sentenceDetector = new SegmentSentenceDetector();

    }

    @Override
    public List<String> split(String input) {

        return sentenceDetector.detect(input);

    }

    @Override
    public String getSeparator() {
        return " ";
    }

}
