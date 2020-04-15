package com.mtnfog.test.phileas.services.disambiguation;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.services.disambiguation.SpanDisambiguationService;
import org.junit.Assert;
import org.junit.Test;

public class SpanDisambiguationServiceTest {

    @Test
    public void test1() {

        final SpanDisambiguationService spanDisambiguationService = new SpanDisambiguationService();

        Span span1 = Span.make(0, 4, FilterType.SSN, "c", "d", 0.00, "123-45-6789", "000-00-0000", false, new String[]{"ssn", "was", "he", "was"});
        spanDisambiguationService.hashAndInsert(span1);

        Span span2 = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", "d", 0.00, "123-45-6789", "000-00-0000", false, new String[]{"phone", "number", "she", "had"});
        spanDisambiguationService.hashAndInsert(span2);

        Span ambiguousSpan = Span.make(0, 4, FilterType.SSN, "c", "d", 0.00, "123-45-6789", "000-00-0000", false, new String[]{"phone", "number", "he", "was"});
        final FilterType filterType = spanDisambiguationService.disambiguate(span1, span2, ambiguousSpan);

        Assert.assertEquals(FilterType.PHONE_NUMBER, filterType);

    }

}
