package com.mtnfog.test.phileas.services.ai;

import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.service.ai.OnnxNer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnnxNerTest {

    private static final Logger LOGGER = LogManager.getLogger(OnnxNerTest.class);

    @Test
    public void find1() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "George Washington was president of the United States.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Span> spans = nameFinderDL.find(tokens, "context", "documentId");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertFalse(spans.get(0).isIgnored());
        Assertions.assertEquals("George Washington", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(17, spans.get(0).getCharacterEnd());

    }

    @Test
    public void find2() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "George Washington lives in 90210 and his SSN was 123-45-6789.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Span> spans = nameFinderDL.find(tokens, "context", "documentId");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertFalse(spans.get(0).isIgnored());
        Assertions.assertEquals("George Washington", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(17, spans.get(0).getCharacterEnd());

    }

    @Test
    public void findWithSpacesInEntity() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "George  Washington lives in California.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Span> spans = nameFinderDL.find(tokens, "context", "documentId");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertFalse(spans.get(0).isIgnored());
        Assertions.assertEquals("George  Washington", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(17, spans.get(0).getCharacterEnd());

    }

    @Test
    public void findWithTabsInEntity() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "George   Washington lives in California.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Span> spans = nameFinderDL.find(tokens, "context", "documentId");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertFalse(spans.get(0).isIgnored());
        Assertions.assertEquals("George   Washington", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(17, spans.get(0).getCharacterEnd());

    }

    @Test
    public void findWithPunctuation() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "George Washington lives in George. Washington is a state.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Span> spans = nameFinderDL.find(tokens, "context", "documentId");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertFalse(spans.get(0).isIgnored());
        Assertions.assertEquals("George Washington", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(17, spans.get(0).getCharacterEnd());

    }

    @Test
    public void findEntityWith3Words() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "George Washington Carver lives in California.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Span> spans = nameFinderDL.find(tokens, "context", "documentId");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertFalse(spans.get(0).isIgnored());
        Assertions.assertEquals("George Washington Carver", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(24, spans.get(0).getCharacterEnd());

    }

    @Test
    public void findWithMultipleEntities() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "George Washington was friends with Bob Ross.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Span> spans = nameFinderDL.find(tokens, "context", "documentId");

        showSpans(spans);

        Assertions.assertEquals(2, spans.size());
        Assertions.assertFalse(spans.get(0).isIgnored());
        Assertions.assertEquals("George Washington", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(17, spans.get(0).getCharacterEnd());
        Assertions.assertEquals("Bob Ross", spans.get(1).getText());
        Assertions.assertEquals(35, spans.get(1).getCharacterStart());
        Assertions.assertEquals(43, spans.get(1).getCharacterEnd());

    }

    @Test
    public void findWithMultipleEntitiesOfSameText() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "George Washington was friends with George Washington.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Span> spans = nameFinderDL.find(tokens, "context", "documentId");

        showSpans(spans);

        Assertions.assertEquals(2, spans.size());
        Assertions.assertFalse(spans.get(0).isIgnored());
        Assertions.assertEquals("George Washington", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(17, spans.get(0).getCharacterEnd());
        Assertions.assertEquals("George Washington", spans.get(1).getText());
        Assertions.assertEquals(35, spans.get(1).getCharacterStart());
        Assertions.assertEquals(52, spans.get(1).getCharacterEnd());

    }

    private Map<Integer, String> getLabels() {

        final Map<Integer, String> id2Labels = new HashMap<>();
        id2Labels.put(0, "O");
        id2Labels.put(1, "B-MISC");
        id2Labels.put(2, "I-MISC");
        id2Labels.put(3, "B-PER");
        id2Labels.put(4, "I-PER");
        id2Labels.put(5, "B-ORG");
        id2Labels.put(6, "I-ORG");
        id2Labels.put(7, "B-LOC");
        id2Labels.put(8, "I-LOC");

        return id2Labels;

    }

    private void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

}
