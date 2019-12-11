package com.mtnfog.test.phileas.store;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.store.ElasticsearchStore;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.fail;

@Ignore
public class ElasticsearchStoreTest {

    private static final Logger LOGGER = LogManager.getLogger(ElasticsearchStoreTest.class);

    private static final String INDEX_NAME = "philter";
    private static final String ELASTICSEARCH_HOST = "search-philter-ct7odx7mieib27cdmuvivjzl74.us-east-1.es.amazonaws.com";
    private static final int PORT = 443;
    private static final String SCHEME = "https";

    private static final int SLEEP_DELAY_MS = 1000;

    @Before
    public void before() throws IOException {

        // Delete the index.
        RestClientBuilder builder = RestClient.builder(new HttpHost(ELASTICSEARCH_HOST, PORT, SCHEME));
        RestHighLevelClient client = new RestHighLevelClient(builder);

        try {
            client.indices().delete(new DeleteIndexRequest(INDEX_NAME), RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException ex) {
            LOGGER.error("Unable to delete Elasticsearch index.");
        }

        try {
            // Create the index.
            client.indices().create(new CreateIndexRequest(INDEX_NAME), RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException ex) {
            fail("Unable to create Elasticsearch index.");
        }

    }

    @Test
    public void test1() throws IOException, InterruptedException {

        final ElasticsearchStore store = new ElasticsearchStore(INDEX_NAME, SCHEME, ELASTICSEARCH_HOST, PORT);

        final Span span = Span.make(1, 2, FilterType.NER_ENTITY, "context", "documentId", 1.0, "test", "***");

        store.insert(span);

        Thread.sleep(SLEEP_DELAY_MS);

        final List<Span> spans = store.getByDocumentId("documentId");

        store.close();

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertEquals(span.getCharacterStart(), spans.get(0).getCharacterStart());
        Assert.assertEquals(span.getCharacterEnd(), spans.get(0).getCharacterEnd());

    }

    @Test
    public void test2() throws IOException, InterruptedException {

        final ElasticsearchStore store = new ElasticsearchStore(INDEX_NAME, SCHEME, ELASTICSEARCH_HOST, PORT);

        final Span span1 = Span.make(1, 2, FilterType.NER_ENTITY, "context", "documentId", 1.0, "test", "***");
        final Span span2 = Span.make(3, 6, FilterType.NER_ENTITY, "context", "documentId", 1.0, "test", "***");
        final Span span3 = Span.make(7, 9, FilterType.NER_ENTITY, "context", "documentId", 1.0, "test", "***");

        store.insert(Arrays.asList(span1, span2, span3));

        Thread.sleep(SLEEP_DELAY_MS);

        final List<Span> spans = store.getByDocumentId("documentId");

        store.close();

        Assert.assertEquals(3, spans.size());
        Assert.assertEquals(span1, spans.get(0));
        Assert.assertEquals(span2, spans.get(1));
        Assert.assertEquals(span3, spans.get(2));

    }

    @Ignore
    @Test
    public void test3() throws IOException {

        final ElasticsearchStore store = new ElasticsearchStore(INDEX_NAME, SCHEME, ELASTICSEARCH_HOST, PORT);

        final List<Span> spans = store.getByDocumentId("cc5e5078-e863-4814-a9e2-c9c51cadfca5");

        store.close();

        Assert.assertEquals(4, spans.size());

        showSpans(spans);

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
