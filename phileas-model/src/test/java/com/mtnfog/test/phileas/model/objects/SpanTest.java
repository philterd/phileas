package com.mtnfog.test.phileas.model.objects;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

        Span span1 = Span.make(1, 6, FilterType.NER_ENTITY, "context", "document", 1.0,  "test", "***", false, new String[0]);
        Span span2 = span1.copy();

        Assertions.assertTrue(span1.equals(span2));

    }

    @Test
    public void shiftSpansTest1() {

        Span span1 = Span.make(1, 6, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]);
        Span span2 = Span.make(8, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]);
        Span span3 = Span.make(14, 20, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]);

        final List<Span> spans = Arrays.asList(span1, span2, span3);
        final List<Span> shiftedSpans = Span.shiftSpans(4, span1, spans);

        Assertions.assertEquals(2, shiftedSpans.size());
        Assertions.assertEquals(12, shiftedSpans.get(0).getCharacterStart());
        Assertions.assertEquals(16, shiftedSpans.get(0).getCharacterEnd());
        Assertions.assertEquals(18, shiftedSpans.get(1).getCharacterStart());
        Assertions.assertEquals(24, shiftedSpans.get(1).getCharacterEnd());

    }

    @Test
    public void shiftSpansTest2() {

        Span span1 = Span.make(1, 6, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]);

        final List<Span> spans = Arrays.asList(span1);
        final List<Span> shiftedSpans = Span.shiftSpans(4, span1, spans);

        Assertions.assertEquals(0, shiftedSpans.size());

    }

    @Test
    public void doesIndexStartSpanTest1() {

        Span span1 = Span.make(1, 6, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]);
        Span span2 = Span.make(8, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]);

        List<Span> spans = Arrays.asList(span1, span2);

        Span span = Span.doesIndexStartSpan(8, spans);

        Assertions.assertNotNull(span);
        Assertions.assertEquals(span2, span);

    }

    @Test
    public void doesIndexStartSpanTest2() {

        Span span1 = Span.make(1, 6, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]);
        Span span2 = Span.make(8, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]);

        List<Span> spans = Arrays.asList(span1, span2);

        Span span = Span.doesIndexStartSpan(1, spans);

        Assertions.assertNotNull(span);
        Assertions.assertEquals(span1, span);

    }

    @Test
    public void doesIndexStartSpanTest3() {

        Span span1 = Span.make(1, 6, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]);
        Span span2 = Span.make(8, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]);

        List<Span> spans = Arrays.asList(span1, span2);

        Span span = Span.doesIndexStartSpan(4, spans);

        Assertions.assertNull(span);

    }

    @Test
    public void ignored1() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(1, 5, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(2, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", true, new String[0]));

        List<Span> nonIgnoredSpans = Span.dropIgnoredSpans(spans);

        showSpans(nonIgnoredSpans);

        Assertions.assertEquals(1, nonIgnoredSpans.size());
        Assertions.assertEquals(1, nonIgnoredSpans.get(0).getCharacterStart());

    }

    @Test
    public void ignored2() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(1, 5, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(2, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]));

        List<Span> nonIgnoredSpans = Span.dropIgnoredSpans(spans);

        showSpans(nonIgnoredSpans);

        Assertions.assertEquals(2, nonIgnoredSpans.size());
        Assertions.assertEquals(1, nonIgnoredSpans.get(0).getCharacterStart());
        Assertions.assertEquals(2, nonIgnoredSpans.get(1).getCharacterStart());

    }

    @Test
    public void ignored3() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(1, 5, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", true, new String[0]));
        spans.add(Span.make(2, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", true, new String[0]));

        List<Span> nonIgnoredSpans = Span.dropIgnoredSpans(spans);

        showSpans(nonIgnoredSpans);

        Assertions.assertEquals(0, nonIgnoredSpans.size());

    }

    @Test
    public void overlapping1() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(1, 5, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(2, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        showSpans(nonOverlappingSpans);

        Assertions.assertEquals(1, nonOverlappingSpans.size());
        Assertions.assertEquals(2, nonOverlappingSpans.get(0).getCharacterStart());
        Assertions.assertEquals(12, nonOverlappingSpans.get(0).getCharacterEnd());

    }

    @Test
    public void overlapping2() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(2, 12, FilterType.NER_ENTITY, "context", "document", 0.5, "test", "***", false, new String[0]));
        spans.add(Span.make(2, 12, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        Assertions.assertEquals(1, nonOverlappingSpans.size());
        Assertions.assertEquals(nonOverlappingSpans.get(0).getCharacterStart(), 2);
        Assertions.assertEquals(nonOverlappingSpans.get(0).getCharacterEnd(), 12);
        Assertions.assertEquals(nonOverlappingSpans.get(0).getConfidence(), 1.0, 0);

    }

    @Test
    public void overlapping3() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(2, 12, FilterType.NER_ENTITY, "context", "document", 0.5, "test", "***", false, new String[0]));
        spans.add(Span.make(14, 20, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        Assertions.assertEquals(2, nonOverlappingSpans.size());

    }

    @Test
    public void overlapping4() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(2, 12, FilterType.NER_ENTITY, "context", "document", 0.5, "test", "***", false, new String[0]));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        Assertions.assertEquals(1, nonOverlappingSpans.size());

    }

    @Test
    public void overlapping5() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(7, 17, FilterType.NER_ENTITY, "context", "document", 0.5, "test", "***", false, new String[0]));
        spans.add(Span.make(0, 17, FilterType.NER_ENTITY, "context", "document", 1.0, "test", "***", false, new String[0]));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        Assertions.assertEquals(1, nonOverlappingSpans.size());
        Assertions.assertEquals(0, nonOverlappingSpans.get(0).getCharacterStart());
        Assertions.assertEquals(17, nonOverlappingSpans.get(0).getCharacterEnd());
        Assertions.assertEquals(1.0, nonOverlappingSpans.get(0).getConfidence(), 0);

    }

    @Test
    public void overlapping6() {

        // Duplicate spans should be dropped in favor of the one that appears in the list first.

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(7, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(7, 17, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", false, new String[0]));

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        showSpans(nonOverlappingSpans);

        Assertions.assertEquals(1, nonOverlappingSpans.size());
        Assertions.assertEquals(7, nonOverlappingSpans.get(0).getCharacterStart());
        Assertions.assertEquals(17, nonOverlappingSpans.get(0).getCharacterEnd());
        Assertions.assertEquals(FilterType.ZIP_CODE, nonOverlappingSpans.get(0).getFilterType());

    }

    @Test
    public void getIdenticalSpans1() {

        final Span span1 = Span.make(7, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", false, new String[0]);

        final List<Span> spans = new LinkedList<>();
        spans.add(span1);
        spans.add(Span.make(7, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(7, 17, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(4, 19, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(22, 25, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", false, new String[0]));

        final List<Span> identicalSpans = Span.getIdenticalSpans(span1, spans);

        Assertions.assertEquals(1, identicalSpans.size());

    }

    @Test
    public void getIdenticalSpans2() {

        final Span span1 = Span.make(7, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", false, new String[0]);

        final List<Span> spans = new LinkedList<>();
        spans.add(span1);
        spans.add(Span.make(7, 17, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(4, 19, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(22, 25, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(7, 17, FilterType.URL, "context", "document", 1.0, "test", "***", false, new String[0]));

        final List<Span> identicalSpans = Span.getIdenticalSpans(span1, spans);

        Assertions.assertEquals(2, identicalSpans.size());

    }

    @Test
    public void getIdenticalSpans3() {

        final Span span1 = Span.make(7, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", false, new String[0]);

        final List<Span> spans = new LinkedList<>();
        spans.add(span1);
        spans.add(Span.make(7, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(7, 17, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(4, 19, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(22, 25, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(7, 17, FilterType.URL, "context", "document", 1.0, "test", "***", false, new String[0]));
        spans.add(Span.make(22, 25, FilterType.AGE, "context", "document", 1.0, "test", "***", false, new String[0]));

        final List<Span> identicalSpans = Span.getIdenticalSpans(span1, spans);

        Assertions.assertEquals(2, identicalSpans.size());

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
