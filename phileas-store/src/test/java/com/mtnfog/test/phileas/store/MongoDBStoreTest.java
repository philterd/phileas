package com.mtnfog.test.phileas.store;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.store.MongoDBStore;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

public class MongoDBStoreTest {

    private static final Logger LOGGER = LogManager.getLogger(MongoDBStoreTest.class);

    @Test
    public void test1() throws IOException {

        final MongoServer server = new MongoServer(new MemoryBackend());
        final InetSocketAddress serverAddress = server.bind();
        final MongoDBStore store = new MongoDBStore(serverAddress.getPort());

        final Span span = Span.make(1, 2, FilterType.NER_ENTITY, "context", "documentId", 1.0, "test", "***");

        store.insert(span);

        final List<Span> spans = store.getByDocumentId("documentId");

        store.close();
        server.shutdown();

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertEquals(span.getCharacterStart(), spans.get(0).getCharacterStart());
        Assert.assertEquals(span.getCharacterEnd(), spans.get(0).getCharacterEnd());

    }

    @Test
    public void test2() throws IOException {

        final MongoServer server = new MongoServer(new MemoryBackend());
        final InetSocketAddress serverAddress = server.bind();
        final MongoDBStore store = new MongoDBStore(serverAddress.getPort());

        final Span span1 = Span.make(1, 2, FilterType.NER_ENTITY, "context", "documentId", 1.0, "test", "***");
        final Span span2 = Span.make(3, 6, FilterType.NER_ENTITY, "context", "documentId", 1.0, "test", "***");
        final Span span3 = Span.make(7, 9, FilterType.NER_ENTITY, "context", "documentId", 1.0, "test", "***");

        store.insert(Arrays.asList(span1, span2, span3));

        final List<Span> spans = store.getByDocumentId("documentId");

        store.close();
        server.shutdown();

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
