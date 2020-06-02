package com.mtnfog.phileas.service.ai;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.dynamic.NerFilter;
import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.model.services.MetricsService;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PyTorchFilter extends NerFilter {

    private static final Logger LOGGER = LogManager.getLogger(PyTorchFilter.class);

    private static int TIMEOUT_SEC = 30;
    private static int MAX_IDLE_CONNECTIONS = 30;
    private static int KEEP_ALIVE_DURATION_MS = 60;

    private transient PyTorchRestService service;
    private String tag;

    // Response will look like:
    // [{"text": "George Washington", "tag": "PER", "score": 0.2987019270658493, "start": 0, "end": 17}, {"text": "Virginia", "tag": "LOC", "score": 0.3510116934776306, "start": 95, "end": 103}]

    public PyTorchFilter(String baseUrl,
                         FilterType filterType,
                         List<? extends AbstractFilterStrategy> strategies,
                         String tag,
                         Map<String, DescriptiveStatistics> stats,
                         MetricsService metricsService,
                         AnonymizationService anonymizationService,
                         AlertService alertService,
                         Set<String> ignored,
                         boolean removePunctuation,
                         Crypto crypto,
                         int windowSize) {

        super(filterType, strategies, stats, metricsService, anonymizationService, alertService, ignored, removePunctuation, crypto, windowSize);

        this.tag = tag;

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_DURATION_MS, TimeUnit.MILLISECONDS))
                .build();

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .callFactory(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(PyTorchRestService.class);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        // Remove punctuation if instructed to do so.
        // It is replacing each punctuation mark with an empty space. This will allow span indexes
        // to remain constant as opposed to removing the punctuation and causing the string to then
        // have a shorter length.
        if(removePunctuation) {
            input = input.replaceAll("\\p{Punct}", " ");
        }

        final Response<List<PhileasSpan>> response = service.process(input).execute();

        if(response.isSuccessful()) {

            final List<PhileasSpan> phileasSpans = response.body();

            if(phileasSpans != null) {

                for (final PhileasSpan phileasSpan : phileasSpans) {

                    // Only interested in spans matching the tag we are looking for, e.g. PER, LOC.
                    if (StringUtils.equalsIgnoreCase(phileasSpan.getTag(), tag)) {

                        final Span span = createSpan(filterProfile.getName(), input, context, documentId, phileasSpan.getText(),
                                phileasSpan.getTag(), phileasSpan.getStart(), phileasSpan.getEnd(), phileasSpan.getScore());

                        // Span will be null if no span was created due to it being excluded.
                        if (span != null) {
                            spans.add(span);
                        }

                    }

                }

                LOGGER.debug("Returning {} NER spans.", spans.size());

                return spans;

            } else {

                // We received a null list of spans from philter-ner. It means something went wrong.
                throw new IOException("Unable to process document. Received error response from philter-ner.");

            }

        } else {

            // The request to philter-ner was not successful.
            throw new IOException("Unable to process document. Received error response from philter-ner.");

        }

    }

    private Span createSpan(String filterProfile, String input, String context, String documentId, String text,
                            String classification, int start, int end, double confidence) throws Exception {

        final Replacement replacement = getReplacement(filterProfile, context, documentId, text, confidence, classification);

        if(StringUtils.equals(replacement.getReplacement(), text)) {

            // If the replacement is the same as the token then there is no span.
            // A condition in the strategy excluded it.

            return null;

        } else {

            final String[] window = getWindow(input, start, end);

            final boolean isIgnored = ignored.contains(text);
            return Span.make(start, end, FilterType.NER_ENTITY, context, documentId, confidence, text, replacement.getReplacement(), replacement.getSalt(), isIgnored, window);

        }

    }

}
