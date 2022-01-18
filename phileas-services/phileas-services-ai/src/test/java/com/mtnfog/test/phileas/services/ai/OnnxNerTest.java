package com.mtnfog.test.phileas.services.ai;

import com.mtnfog.phileas.model.objects.Entity;
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
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(1, spans.size());
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
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(1, spans.size());
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
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals("George  Washington", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(18, spans.get(0).getCharacterEnd());

    }

    @Test
    public void findWithTabsInEntity() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "George   Washington lives in California.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals("George   Washington", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(19, spans.get(0).getCharacterEnd());

    }

    @Test
    public void findWithPunctuation() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "George Washington lives in George. Washington is a state.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals("George Washington", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(17, spans.get(0).getCharacterEnd());

    }

    @Test
    public void findEntityWithWordpieces() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "123-45-6789 was George Washington ssn.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals("George Washington", spans.get(0).getText());
        Assertions.assertEquals(16, spans.get(0).getCharacterStart());
        Assertions.assertEquals(33, spans.get(0).getCharacterEnd());

    }

    @Test
    public void findEntityWith3Words() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "George Washington Carver lives in California.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(1, spans.size());
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
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(2, spans.size());
        Assertions.assertEquals("George Washington", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(17, spans.get(0).getCharacterEnd());
        Assertions.assertEquals("Bob Ross", spans.get(1).getText());
        Assertions.assertEquals(35, spans.get(1).getCharacterStart());
        Assertions.assertEquals(43, spans.get(1).getCharacterEnd());

    }

    @Test
    public void findWithMultipleIdenticalEntities() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "George Washington was friends with George Washington.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(2, spans.size());
        Assertions.assertEquals("George Washington", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(17, spans.get(0).getCharacterEnd());
        Assertions.assertEquals("George Washington", spans.get(1).getText());
        Assertions.assertEquals(35, spans.get(1).getCharacterStart());
        Assertions.assertEquals(52, spans.get(1).getCharacterEnd());

    }

    @Test
    public void findWithMultipleEntitiesOfSameText() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "George Washington was friends with George Washington.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(2, spans.size());
        Assertions.assertEquals("George Washington", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(17, spans.get(0).getCharacterEnd());
        Assertions.assertEquals("George Washington", spans.get(1).getText());
        Assertions.assertEquals(35, spans.get(1).getCharacterStart());
        Assertions.assertEquals(52, spans.get(1).getCharacterEnd());

    }

    @Test
    public void findWithParagraphOneWordpiece() throws Exception {

        // The name Barbara Ferrer will be tokenized as:
        // Barbara Fe ##rrer
        // This test is to get that entity text correct.

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "In recent days, healthcare facilities across the nation have again begun to buckle under spiking infection rates. Last week, some local hospitals temporarily postponed scheduled surgeries that require an inpatient stay following an operation, and the trauma center at Harbor-UCLA Medical Center closed for hours because of a blood shortage - a step it hadn't taken in over three decades. A staff shortage at some local ambulance companies further complicated the situation.The virus has spread so fast since the arrival of the Omicron variant that it could take just about a week for California to tally a million new cases. It was only on Jan. 10 that California surpassed 6 million total reported coronavirus cases in the nearly two years since the start of the pandemic, according to data released by state health officials. Even during last winter's surge, it took three weeks to accumulate a million new cases, with the state peaking at 46,000 new infections a day. \"On this national holiday where we celebrate the life and legacy of Dr. Martin Luther King, we remember his deep commitment to health equity,\" said L.A. County Public Health Director Barbara Ferrer. \"As Reverend King memorably said, \"Of all the forms of inequality, injustice in health is the most shocking and the most inhuman because it often results in physical death.\"";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(3, spans.size());

        Assertions.assertEquals("Martin Luther King", spans.get(0).getText());
        Assertions.assertEquals(1043, spans.get(0).getCharacterStart());
        Assertions.assertEquals(1061, spans.get(0).getCharacterEnd());

        Assertions.assertEquals("Barbara Ferrer", spans.get(1).getText());
        Assertions.assertEquals(1154, spans.get(1).getCharacterStart());
        Assertions.assertEquals(1168, spans.get(1).getCharacterEnd());

        Assertions.assertEquals("Reverend King", spans.get(2).getText());
        Assertions.assertEquals(1174, spans.get(2).getCharacterStart());
        Assertions.assertEquals(1187, spans.get(2).getCharacterEnd());

    }

    @Test
    public void findWithParagraphMultipleNameEntity() throws Exception {

        // The name Barbara Ferrer Smith will be tokenized as:
        // Barbara Fe ##rrer Smith
        // This test is to get that entity text correct.

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "In recent days, healthcare facilities across the nation have again begun to buckle under spiking infection rates. Last week, some local hospitals temporarily postponed scheduled surgeries that require an inpatient stay following an operation, and the trauma center at Harbor-UCLA Medical Center closed for hours because of a blood shortage - a step it hadn't taken in over three decades. A staff shortage at some local ambulance companies further complicated the situation.The virus has spread so fast since the arrival of the Omicron variant that it could take just about a week for California to tally a million new cases. It was only on Jan. 10 that California surpassed 6 million total reported coronavirus cases in the nearly two years since the start of the pandemic, according to data released by state health officials. Even during last winter's surge, it took three weeks to accumulate a million new cases, with the state peaking at 46,000 new infections a day. \"On this national holiday where we celebrate the life and legacy of Dr. Martin Luther King, we remember his deep commitment to health equity,\" said L.A. County Public Health Director Barbara Ferrer Smith. \"As Reverend King memorably said, \"Of all the forms of inequality, injustice in health is the most shocking and the most inhuman because it often results in physical death.\"";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(3, spans.size());

        Assertions.assertEquals("Martin Luther King", spans.get(0).getText());
        Assertions.assertEquals(1043, spans.get(0).getCharacterStart());
        Assertions.assertEquals(1061, spans.get(0).getCharacterEnd());

        Assertions.assertEquals("Barbara Ferrer Smith", spans.get(1).getText());
        Assertions.assertEquals(1154, spans.get(1).getCharacterStart());
        Assertions.assertEquals(1174, spans.get(1).getCharacterEnd());

        Assertions.assertEquals("Reverend King", spans.get(2).getText());
        Assertions.assertEquals(1180, spans.get(2).getCharacterStart());
        Assertions.assertEquals(1193, spans.get(2).getCharacterEnd());

    }

    @Test
    public void findWithParagraphMultipleWordpieces() throws Exception {

        // The name Barbara Ferrer Ferrer will be tokenized as:
        // Barbara Fe ##rrer Smith
        // This test is to get that entity text correct.

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "In recent days, healthcare facilities across the nation have again begun to buckle under spiking infection rates. Last week, some local hospitals temporarily postponed scheduled surgeries that require an inpatient stay following an operation, and the trauma center at Harbor-UCLA Medical Center closed for hours because of a blood shortage - a step it hadn't taken in over three decades. A staff shortage at some local ambulance companies further complicated the situation.The virus has spread so fast since the arrival of the Omicron variant that it could take just about a week for California to tally a million new cases. It was only on Jan. 10 that California surpassed 6 million total reported coronavirus cases in the nearly two years since the start of the pandemic, according to data released by state health officials. Even during last winter's surge, it took three weeks to accumulate a million new cases, with the state peaking at 46,000 new infections a day. \"On this national holiday where we celebrate the life and legacy of Dr. Martin Luther King, we remember his deep commitment to health equity,\" said L.A. County Public Health Director Barbara Ferrer Ferrer. \"As Reverend King memorably said, \"Of all the forms of inequality, injustice in health is the most shocking and the most inhuman because it often results in physical death.\"";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, getLabels());
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(3, spans.size());

        Assertions.assertEquals("Martin Luther King", spans.get(0).getText());
        Assertions.assertEquals(1043, spans.get(0).getCharacterStart());
        Assertions.assertEquals(1061, spans.get(0).getCharacterEnd());

        Assertions.assertEquals("Barbara Ferrer Ferrer", spans.get(1).getText());
        Assertions.assertEquals(1154, spans.get(1).getCharacterStart());
        Assertions.assertEquals(1175, spans.get(1).getCharacterEnd());

        Assertions.assertEquals("Reverend King", spans.get(2).getText());
        Assertions.assertEquals(1181, spans.get(2).getCharacterStart());
        Assertions.assertEquals(1194, spans.get(2).getCharacterEnd());

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

    private void showEntities(List<Entity> entities) {

        for(Entity entity : entities) {
            LOGGER.info(entity.toString());
        }

    }

}
