package com.mtnfog.test.phileas.services.postfilters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.PostFilter;
import com.mtnfog.phileas.services.PhileasFilterService;
import com.mtnfog.phileas.services.postfilters.PartOfSpeechFalsePositiveFilter;
import opennlp.tools.postag.POSModel;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class PartOfSpeechFalsePositiveFilterTest {

    @Test
    public void filter1() throws Exception {

        final InputStream is = PhileasFilterService.class.getClassLoader().getResourceAsStream("en-pos-perceptron.bin");
        final POSModel model = new POSModel(is);

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.LOCATION_STATE, "context", "docid", 0.80, "test", "*****"));

        final PostFilter filter = new PartOfSpeechFalsePositiveFilter(model);
        final List<Span> filteredSpans = filter.filter("He lived in Washington.", spans);

        Assert.assertEquals(1, filteredSpans.size());

    }

    @Test
    public void filter2() throws Exception {

        final InputStream is = PhileasFilterService.class.getClassLoader().getResourceAsStream("en-pos-perceptron.bin");
        final POSModel model = new POSModel(is);

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(3, 8, FilterType.LOCATION_STATE, "context", "docid", 0.80, "test", "*****"));

        final PostFilter filter = new PartOfSpeechFalsePositiveFilter(model);
        final List<Span> filteredSpans = filter.filter("He lived in Washington.", spans);

        Assert.assertEquals(0, filteredSpans.size());

    }

    @Test
    public void filter3() throws Exception {

        final InputStream is = PhileasFilterService.class.getClassLoader().getResourceAsStream("en-pos-perceptron.bin");
        final POSModel model = new POSModel(is);

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(0, 17, FilterType.NER_ENTITY, "context", "docid", 0.80, "test", "*****"));

        final PostFilter filter = new PartOfSpeechFalsePositiveFilter(model);
        final List<Span> filteredSpans = filter.filter("George Washington was president and his ssn was 123-45-6789 and he lived at 90210. Patient id 00076a and 93821a. He is on biotin. Diagnosed with A0100.", spans);

        Assert.assertEquals(0, filteredSpans.size());

    }

}
