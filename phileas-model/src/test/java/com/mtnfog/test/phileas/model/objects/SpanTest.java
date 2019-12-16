package com.mtnfog.test.phileas.model.objects;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SpanTest {

    private static final Logger LOGGER = LogManager.getLogger(SpanTest.class);

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(Span.class).suppress(Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void cloneTest() {

        Span span1 = Span.make(1, 6, FilterType.NER_ENTITY, "context", "document", 1.0,  "test", "***", false);
        Span span2 = span1.copy();

        Assert.assertTrue(span1.equals(span2));

    }

    @Test(expected = IllegalArgumentException.class)
    public void getSpanWindowTestInvalidWindowSize() {

        final String[] tokens = {"George", "Washington", "was", "president", "of", "the", "United", "States"};
        final String[] window = Span.getSpanWindow(tokens, 3, 2);

    }

    @Test
    public void getSpanWindowTest1() {

        final String[] tokens = {"George", "Washington", "was", "president", "of", "the", "United", "States"};
        final String[] window = Span.getSpanWindow(tokens, 3, 3);

        Assert.assertEquals("was, president, of", StringUtils.join(window, ", "));

    }

    @Test
    public void getSpanWindowTest2() {

        final String[] tokens = {"George", "Washington", "was", "president", "of", "the", "United", "States"};
        final String[] window = Span.getSpanWindow(tokens, 3, 5);

        Assert.assertEquals("Washington, was, president, of, the", StringUtils.join(window, ", "));

    }

    @Test
    public void getSpanWindowTest3() {

        final String[] tokens = {"George", "Washington", "was", "president", "of", "the", "United", "States"};
        final String[] window = Span.getSpanWindow(tokens, 1, 5);

        Assert.assertEquals("_, George, Washington, was, president", StringUtils.join(window, ", "));

    }

    @Test
    public void getSpanWindowTest4() {

        final String[] tokens = {"George", "Washington", "was", "president", "of", "the", "United", "States"};
        final String[] window = Span.getSpanWindow(tokens, 5, 5);

        Assert.assertEquals("president, of, the, United, States", StringUtils.join(window, ", "));

    }

    @Test
    public void getSpanWindowTest5() {

        final String[] tokens = {"George", "Washington", "was", "president", "of", "the", "United", "States"};
        final String[] window = Span.getSpanWindow(tokens, 6, 5);

        Assert.assertEquals("of, the, United, States, _", StringUtils.join(window, ", "));

    }

    @Test
    public void shiftSpansTest1() {

        Span span1 = Span.make(1, 6, FilterType.NER_ENTITY, "context", "document", 1.0, "test",  "***", false);
        Span span2 = Span.make(8, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false);
        Span span3 = Span.make(14, 20, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false);

        final List<Span> spans = Arrays.asList(span1, span2, span3);
        final List<Span> shiftedSpans = Span.shiftSpans(4, span1, spans);

        Assert.assertEquals(2, shiftedSpans.size());
        Assert.assertEquals(12, shiftedSpans.get(0).getCharacterStart());
        Assert.assertEquals(16, shiftedSpans.get(0).getCharacterEnd());
        Assert.assertEquals(18, shiftedSpans.get(1).getCharacterStart());
        Assert.assertEquals(24, shiftedSpans.get(1).getCharacterEnd());

    }

    @Test
    public void shiftSpansTest2() {

        Span span1 = Span.make(1, 6, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false);

        final List<Span> spans = Arrays.asList(span1);
        final List<Span> shiftedSpans = Span.shiftSpans(4, span1, spans);

        Assert.assertEquals(0, shiftedSpans.size());

    }

    @Test
    public void doesIndexStartSpanTest1() {

        Span span1 = Span.make(1, 6, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false);
        Span span2 = Span.make(8, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false);

        List<Span> spans = Arrays.asList(span1, span2);

        Span span = Span.doesIndexStartSpan(8, spans);

        Assert.assertNotNull(span);
        Assert.assertEquals(span2, span);

    }

    @Test
    public void doesIndexStartSpanTest2() {

        Span span1 = Span.make(1, 6, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false);
        Span span2 = Span.make(8, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false);

        List<Span> spans = Arrays.asList(span1, span2);

        Span span = Span.doesIndexStartSpan(1, spans);

        Assert.assertNotNull(span);
        Assert.assertEquals(span1, span);

    }

    @Test
    public void doesIndexStartSpanTest3() {

        Span span1 = Span.make(1, 6, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false);
        Span span2 = Span.make(8, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false);

        List<Span> spans = Arrays.asList(span1, span2);

        Span span = Span.doesIndexStartSpan(4, spans);

        Assert.assertNull(span);

    }

    @Test
    public void overlapping1() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(1, 5, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false));
        spans.add(Span.make(2, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        showSpans(nonOverlappingSpans);

        Assert.assertEquals(1, nonOverlappingSpans.size());
        Assert.assertEquals(nonOverlappingSpans.get(0).getCharacterStart(), 2);
        Assert.assertEquals(nonOverlappingSpans.get(0).getCharacterEnd(), 12);

    }

    @Test
    public void overlapping2() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(2, 12, FilterType.NER_ENTITY, "context", "document", 0.5, "test", "***", false));
        spans.add(Span.make(2, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        Assert.assertEquals(1, nonOverlappingSpans.size());
        Assert.assertEquals(nonOverlappingSpans.get(0).getCharacterStart(), 2);
        Assert.assertEquals(nonOverlappingSpans.get(0).getCharacterEnd(), 12);
        Assert.assertEquals(nonOverlappingSpans.get(0).getConfidence(), 1.0, 0);

    }

    @Test
    public void overlapping3() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(2, 12, FilterType.NER_ENTITY, "context", "document", 0.5, "test", "***", false));
        spans.add(Span.make(14, 20, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        Assert.assertEquals(2, nonOverlappingSpans.size());

    }

    @Test
    public void overlapping4() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(2, 12, FilterType.NER_ENTITY, "context", "document", 0.5, "test", "***", false));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        Assert.assertEquals(1, nonOverlappingSpans.size());

    }

    @Test
    public void overlapping5() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(7, 17, FilterType.NER_ENTITY, "context", "document", 0.5, "test", "***", false));
        spans.add(Span.make(0, 17, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        Assert.assertEquals(1, nonOverlappingSpans.size());
        Assert.assertEquals(nonOverlappingSpans.get(0).getCharacterStart(), 0);
        Assert.assertEquals(nonOverlappingSpans.get(0).getCharacterEnd(), 17);
        Assert.assertEquals(nonOverlappingSpans.get(0).getConfidence(), 1.0, 0);

    }

    @Test
    public void overlapping6() {

        // Duplicate spans should be dropped in favor of the one that appears in the list first.

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(7, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", false));
        spans.add(Span.make(7, 17, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", false));

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        showSpans(nonOverlappingSpans);

        Assert.assertEquals(1, nonOverlappingSpans.size());
        Assert.assertEquals(nonOverlappingSpans.get(0).getCharacterStart(), 7);
        Assert.assertEquals(nonOverlappingSpans.get(0).getCharacterEnd(), 17);
        Assert.assertEquals(nonOverlappingSpans.get(0).getFilterType(), FilterType.ZIP_CODE);

    }

    private void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

    private boolean checkSpan(Span span, int characterStart, int characterEnd, FilterType filterType) {

        LOGGER.info("Checking span: {}", span.toString());

        return (span.getCharacterStart() == characterStart
                && span.getCharacterEnd() == characterEnd
                && span.getFilterType() == filterType);

    }

}
