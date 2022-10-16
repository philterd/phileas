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

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, false, getLabels());
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

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, false, getLabels());
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals("George Washington", spans.get(0).getText());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(17, spans.get(0).getCharacterEnd());

    }

    @Test
    public void findMultipleInvocations() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens1 = "George Washington lives in 90210 and his SSN was 123-45-6789.";
        final String tokens2 = "George Washington was president.";
        final String tokens3 = "George Washington lived in the United States.";

        final OnnxNer nameFinderDL1 = new OnnxNer(model, vocab, false, getLabels());
        final List<Entity> spans1 = nameFinderDL1.find(tokens1, "context", "documentId");

        final OnnxNer nameFinderDL2 = new OnnxNer(model, vocab, false, getLabels());
        final List<Entity> spans2 = nameFinderDL2.find(tokens2, "context", "documentId");

        final OnnxNer nameFinderDL3 = new OnnxNer(model, vocab, false, getLabels());
        final List<Entity> spans3 = nameFinderDL3.find(tokens3, "context", "documentId");
        final List<Entity> spans4 = nameFinderDL3.find(tokens2, "context", "documentId");
        final List<Entity> spans5 = nameFinderDL3.find(tokens3, "context", "documentId");

        showEntities(spans5);

        Assertions.assertEquals(1, spans4.size());
        Assertions.assertEquals("George Washington", spans4.get(0).getText());
        Assertions.assertEquals(0, spans4.get(0).getCharacterStart());
        Assertions.assertEquals(17, spans4.get(0).getCharacterEnd());

        Assertions.assertEquals(1, spans5.size());
        Assertions.assertEquals("George Washington", spans5.get(0).getText());
        Assertions.assertEquals(0, spans5.get(0).getCharacterStart());
        Assertions.assertEquals(17, spans5.get(0).getCharacterEnd());

    }

    @Test
    public void findWithLongInput() throws Exception {

        // Tests a long input that has been split. 512 is the max number of tokens.
        // BERT base (12 layers, 768 hidden, input max sequence 512)

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "The decision to pause came after there was a potentially unexplained illness in one of the trials. George Washington was president. Intra-Cellular Therapies, Inc. (NASDAQ: ITCI) shares shot up 67% to $30.86 after the company announced the results from its study 402 evaluating Lumateperone 42mg achieved statistically significant results in primary and key secondary endpoints. Shares of Watford Holdings Ltd. (NASDAQ: WTRE) got a boost, shooting 43% to $25.52 after Reuters reported that Arch Capital is in a $26 per share bid for the company. Trillium Therapeutics Inc. (NASDAQ: TRIL) shares were also up, gaining 37% to $12.93 after the company said it has agreed to sell 2.298 million shares of its common shares in a registered direct offering to Pfizer at a price of $10.88 per share, for raising gross proceeds of $25 million. Separately, Trillium announced updated data from its ongoing TTI-622 and TTI-621 dose escalation studies in relapsed and refractory lymphomas, showing the former demonstrated substantial monotherapy activity in highly pre-treated patients, with a broad therapeutic window, a rapid onset of action, and across a range of lymphoma indications. Since no safety signal was observed, the company said it is further escalating the dose. NextDecade Corporation (NASDAQ: NEXT) shares tumbled 21% to $1.83 after jumping over 75% on Tuesday. Shares of MasterCraft Boat Holdings, Inc. (NASDAQ: MCFT) were down 18% to $18.39 after the company reported quarterly results. Abraham Lincoln wsa president. Ashford Hospitality Trust, Inc. (NYSE: AHT) was down, falling 15% to $2.48. In commodity news, oil traded up 1.6% to $37.36, while gold traded down 0.7% to $1,929.10. Silver traded down 1.1% Wednesday to $26.69, while copper fell 0.2% to $3.0205. European shares were higher today as investors are awaiting the ECB’s monetary policy decision tomorrow for clues regarding further stimulus. The eurozone’s STOXX 600 gained 0.6%, the Spanish Ibex Index rose 0.1%, while Italy’s FTSE MIB Index climbed 0.5%. Meanwhile, the German DAX 30 gained 0.9%, French CAC 40 rose 0.5% and London’s FTSE 100 rose 0.8%. The Johnson Redbook Retail Sales Index fell 1% during the first week of September versus August. The number of job openings rose by 617,000 to 6.618 million in July. The Treasury is set to auction 10-year notes at 1:00 p.m. ET.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, false, getLabels());
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(3, spans.size());

        Assertions.assertEquals("George Washington", spans.get(0).getText());
        Assertions.assertEquals(99, spans.get(0).getCharacterStart());
        Assertions.assertEquals(116, spans.get(0).getCharacterEnd());

        Assertions.assertEquals("Abraham Lincoln", spans.get(1).getText());
        Assertions.assertEquals(1493, spans.get(1).getCharacterStart());
        Assertions.assertEquals(1508, spans.get(1).getCharacterEnd());


    }

    @Test
    public void findWithSpacesInEntity() throws Exception {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "George  Washington lives in California.";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, false, getLabels());
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

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, false, getLabels());
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

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, false, getLabels());
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

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, false, getLabels());
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

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, false, getLabels());
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

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, false, getLabels());
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

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, false, getLabels());
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

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, false, getLabels());
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

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, false, getLabels());
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(6, spans.size());

        // This doesn't check all 6. It only spot checks.

        Assertions.assertEquals("Martin Luther King", spans.get(0).getText());
        Assertions.assertEquals(1043, spans.get(0).getCharacterStart());
        Assertions.assertEquals(1061, spans.get(0).getCharacterEnd());

        Assertions.assertEquals("Barbara Ferrer", spans.get(1).getText());
        Assertions.assertEquals(1154, spans.get(1).getCharacterStart());
        Assertions.assertEquals(1168, spans.get(1).getCharacterEnd());

    }

    @Test
    public void findWithParagraphMultipleNameEntity() throws Exception {

        // The name Barbara Ferrer Smith will be tokenized as:
        // Barbara Fe ##rrer Smith
        // This test is to get that entity text correct.

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "In recent days, healthcare facilities across the nation have again begun to buckle under spiking infection rates. Last week, some local hospitals temporarily postponed scheduled surgeries that require an inpatient stay following an operation, and the trauma center at Harbor-UCLA Medical Center closed for hours because of a blood shortage - a step it hadn't taken in over three decades. A staff shortage at some local ambulance companies further complicated the situation.The virus has spread so fast since the arrival of the Omicron variant that it could take just about a week for California to tally a million new cases. It was only on Jan. 10 that California surpassed 6 million total reported coronavirus cases in the nearly two years since the start of the pandemic, according to data released by state health officials. Even during last winter's surge, it took three weeks to accumulate a million new cases, with the state peaking at 46,000 new infections a day. \"On this national holiday where we celebrate the life and legacy of Dr. Martin Luther King, we remember his deep commitment to health equity,\" said L.A. County Public Health Director Barbara Ferrer Smith. \"As Reverend King memorably said, \"Of all the forms of inequality, injustice in health is the most shocking and the most inhuman because it often results in physical death.\"";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, false, getLabels());
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(7, spans.size());

        // This doesn't check all 7, It only spot checks.

        Assertions.assertEquals("Martin Luther King", spans.get(0).getText());
        Assertions.assertEquals(1043, spans.get(0).getCharacterStart());
        Assertions.assertEquals(1061, spans.get(0).getCharacterEnd());

        Assertions.assertEquals("Barbara Ferrer Smith", spans.get(1).getText());
        Assertions.assertEquals(1154, spans.get(1).getCharacterStart());
        Assertions.assertEquals(1174, spans.get(1).getCharacterEnd());

    }

    @Test
    public void findWithParagraphMultipleWordpieces() throws Exception {

        // The name Barbara Ferrer Ferrer will be tokenized as:
        // Barbara Fe ##rrer Smith
        // This test is to get that entity text correct.

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final String tokens = "In recent days, healthcare facilities across the nation have again begun to buckle under spiking infection rates. Last week, some local hospitals temporarily postponed scheduled surgeries that require an inpatient stay following an operation, and the trauma center at Harbor-UCLA Medical Center closed for hours because of a blood shortage - a step it hadn't taken in over three decades. A staff shortage at some local ambulance companies further complicated the situation.The virus has spread so fast since the arrival of the Omicron variant that it could take just about a week for California to tally a million new cases. It was only on Jan. 10 that California surpassed 6 million total reported coronavirus cases in the nearly two years since the start of the pandemic, according to data released by state health officials. Even during last winter's surge, it took three weeks to accumulate a million new cases, with the state peaking at 46,000 new infections a day. \"On this national holiday where we celebrate the life and legacy of Dr. Martin Luther King, we remember his deep commitment to health equity,\" said L.A. County Public Health Director Barbara Ferrer Ferrer. \"As Reverend King memorably said, \"Of all the forms of inequality, injustice in health is the most shocking and the most inhuman because it often results in physical death.\"";

        final OnnxNer nameFinderDL = new OnnxNer(model, vocab, false, getLabels());
        final List<Entity> spans = nameFinderDL.find(tokens, "context", "documentId");

        showEntities(spans);

        Assertions.assertEquals(7, spans.size());

        // This doesn't check all 7. It only spot checks.

        Assertions.assertEquals("Martin Luther King", spans.get(0).getText());
        Assertions.assertEquals(1043, spans.get(0).getCharacterStart());
        Assertions.assertEquals(1061, spans.get(0).getCharacterEnd());

        Assertions.assertEquals("Barbara Ferrer Ferrer", spans.get(1).getText());
        Assertions.assertEquals(1154, spans.get(1).getCharacterStart());
        Assertions.assertEquals(1175, spans.get(1).getCharacterEnd());

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
