package com.mtnfog.phileas.store;

import com.google.gson.Gson;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.Store;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of {@link Store} that uses Elasticsearch.
 */
public class ElasticsearchStore implements Store, Closeable {

    private static final Logger LOGGER = LogManager.getLogger(ElasticsearchStore.class);

    private String indexName;
    private RestHighLevelClient client;
    private Gson gson;

    public ElasticsearchStore(String indexName, String scheme, String host, int port) {

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("username", "password"));

        //RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme)).setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme));

        this.indexName = indexName;
        this.client =  new RestHighLevelClient(builder);
        this.gson = new Gson();

    }

    @Override
    public void insert(Span span) throws IOException {

        final String json = gson.toJson(span);

        final IndexRequest request = new IndexRequest(indexName);
        request.source(json, XContentType.JSON);

        client.index(request, RequestOptions.DEFAULT);

    }

    @Override
    public void insert(List<Span> spans) throws IOException {

        BulkRequest bulkRequest = new BulkRequest();

        spans.forEach(span -> {
            final String json = gson.toJson(span);
            IndexRequest indexRequest = new IndexRequest(indexName).source(json, XContentType.JSON);
            bulkRequest.add(indexRequest);
        });

        client.bulk(bulkRequest, RequestOptions.DEFAULT);

    }

    @Override
    public List<Span> getByDocumentId(String documentId) throws IOException {

        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery("documentId", documentId));
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHit[] searchHit = searchResponse.getHits().getHits();

        final List<Span> spans = new ArrayList<>();

        if (searchHit.length > 0) {

            Arrays.stream(searchHit)
                    .forEach(hit -> spans
                            .add(gson.fromJson(hit.getSourceAsString(), Span.class))
                    );

        }

        return spans;

    }

    @Override
    public void close() throws IOException {
        client.close();
    }

}
