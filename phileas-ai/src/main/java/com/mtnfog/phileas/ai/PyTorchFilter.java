package com.mtnfog.phileas.ai;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.filter.dynamic.NerFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.ai.NerFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.model.services.MetricsService;
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
import java.io.Serializable;
import java.util.*;

public class PyTorchFilter extends NerFilter implements Serializable {

    private static final Logger LOGGER = LogManager.getLogger(PyTorchFilter.class);

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
                         Set<String> ignored,
                         boolean removePunctuation) {

        super(filterType, strategies, stats, metricsService, anonymizationService, ignored, removePunctuation);

        this.tag = tag;

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
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
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

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

                        final Span span = createSpan(context, documentId, phileasSpan.getText(),
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

    private Span createSpan(String context, String documentId, String text,
                            String type, int start, int end, double confidence) throws IOException {

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(NerFilterStrategy.CONFIDENCE, confidence);
        attributes.put(NerFilterStrategy.TYPE, type);

        final String replacement = getReplacement(label, context, documentId, text, attributes);

        if(StringUtils.equals(replacement, text)) {

            // If the replacement is the same as the token then there is no span.
            // A condition in the strategy excluded it.

            return null;

        } else {

            final boolean isIgnored = ignored.contains(text);
            final Span span = Span.make(start, end, FilterType.NER_ENTITY, context, documentId, confidence, text, replacement, isIgnored);

            // Send the entity to the metrics service for reporting.
            metricsService.reportEntitySpan(span);

            return span;

        }

    }

}
