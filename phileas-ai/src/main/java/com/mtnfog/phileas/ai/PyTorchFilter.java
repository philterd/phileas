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
                         Set<String> ignored) {

        super(filterType, strategies, stats, metricsService, anonymizationService, ignored);

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

        final List<? extends AbstractFilterStrategy> strategies = Filter.getFilterStrategies(filterProfile, filterType, 0);

        LOGGER.debug("Applying {} NER filtering strategies.", strategies.size());

        for(AbstractFilterStrategy strategy : strategies) {

            final Response<List<PhileasSpan>> response = service.process(input).execute();
            final List<PhileasSpan> phileasSpans = response.body();

            for(PhileasSpan p : phileasSpans) {

                // Only interested in spans matching the tag we are looking for, e.g. PER, LOC.
                if(StringUtils.equalsIgnoreCase(p.getTag(), tag)) {

                    final Span span = createSpan(filterProfile, context, documentId, p.getText(),
                            p.getTag(), p.getStart(), p.getEnd(), p.getScore());

                    spans.add(span);

                }

            }

        }

        LOGGER.debug("Returning {} NER spans.", spans.size());

        return spans;

    }

    private Span createSpan(FilterProfile filterProfile, String context, String documentId, String text,
                            String type, int start, int end, double confidence) throws IOException {

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(NerFilterStrategy.CONFIDENCE, confidence);
        attributes.put(NerFilterStrategy.TYPE, type);

        final String replacement = getReplacement(label, context, documentId, text, attributes);
        final boolean isIgnored = ignored.contains(text);
        final Span span = Span.make(start, end, FilterType.NER_ENTITY, context, documentId, confidence, text, replacement, isIgnored);

        // Send the entity to the metrics service for reporting.
        metricsService.reportEntitySpan(span);

        return span;

    }

}
