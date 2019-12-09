package com.mtnfog.test.phileas.store;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.store.ElasticsearchStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ElasticsearchStoreTest {

    private static final Logger LOGGER = LogManager.getLogger(ElasticsearchStoreTest.class);

    @Before
    public void before() throws IOException, InterruptedException {

    }

    @After
    public void after() {

    }

    @Test
    public void test1() throws IOException {

        final ElasticsearchStore store = new ElasticsearchStore("philter", "http", "localhost", 9200);

        final Span span = Span.make(1, 2, FilterType.NER_ENTITY, "context", "documentId", 1.0, "test", "***");

        store.insert(span);

        final List<Span> spans = store.getByDocumentId("documentId");

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertEquals(span.getCharacterStart(), spans.get(0).getCharacterStart());
        Assert.assertEquals(span.getCharacterEnd(), spans.get(0).getCharacterEnd());

    }

    @Test
    public void test2() throws IOException {

        final ElasticsearchStore store = new ElasticsearchStore("philter", "http", "localhost", 9350);

        final Span span1 = Span.make(1, 2, FilterType.NER_ENTITY, "context", "documentId", 1.0, "test", "***");
        final Span span2 = Span.make(3, 6, FilterType.NER_ENTITY, "context", "documentId", 1.0, "test", "***");
        final Span span3 = Span.make(7, 9, FilterType.NER_ENTITY, "context", "documentId", 1.0, "test", "***");

        store.insert(Arrays.asList(span1, span2, span3));

        final List<Span> spans = store.getByDocumentId("documentId");

        Assert.assertEquals(3, spans.size());
        Assert.assertEquals(span1, spans.get(0));
        Assert.assertEquals(span2, spans.get(1));
        Assert.assertEquals(span3, spans.get(2));

    }

    public void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

    public boolean checkSpan(Span span, int characterStart, int characterEnd, FilterType filterType) {

        LOGGER.info("Checking span: {}", span.toString());

        return (span.getCharacterStart() == characterStart
                && span.getCharacterEnd() == characterEnd
                && span.getFilterType() == filterType);

    }

}
