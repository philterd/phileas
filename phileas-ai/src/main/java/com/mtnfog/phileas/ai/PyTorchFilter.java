package com.mtnfog.phileas.ai;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PyTorchFilter extends NerFilter implements Serializable {

    private static final Logger LOGGER = LogManager.getLogger(PyTorchFilter.class);

    private transient PyTorchRestService service;
    private String tag;

    // TODO: Can I pass pre-tokenized strings to the REST service so Philter has control over the tokenizing?

    // Response will look like:
    // [{"text": "George Washington", "tag": "PER", "score": 0.2987019270658493, "start": 0, "end": 17}, {"text": "Virginia", "tag": "LOC", "score": 0.3510116934776306, "start": 95, "end": 103}]

    public PyTorchFilter(String baseUrl, FilterType filterType, String tag,
                         Map<String, DescriptiveStatistics> stats,
                         MetricsService metricsService,
                         AnonymizationService anonymizationService) {

        super(filterType, stats, metricsService, anonymizationService);

        this.tag = tag;

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(PyTorchRestService.class);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        final List<Span> spans = new LinkedList<>();

        final List<? extends AbstractFilterStrategy> strategies = Filter.getFilterStrategies(filterProfile, filterType);

        LOGGER.debug("Applying {} NER filtering strategies.", strategies.size());

        for(AbstractFilterStrategy strategy : strategies) {

            final SensitivityLevel sensitivityLevel = SensitivityLevel.fromName(strategy.getSensitivityLevel());

            final Response<List<PhileasSpan>> response = service.process(input).execute();
            final List<PhileasSpan> phileasSpans = response.body();

            for(PhileasSpan p : phileasSpans) {

                // Only interested in spans matching the tag we are looking for, e.g. PER, LOC.
                if(StringUtils.equalsIgnoreCase(p.getTag(), tag)) {

                    final Span span = createSpan(filterProfile, context, documentId, p.getText(),
                            p.getTag(), p.getStart(), p.getEnd(), p.getScore(), sensitivityLevel);

                    spans.add(span);

                }

            }

        }

        LOGGER.debug("Returning {} NER spans.", spans.size());

        return spans;

    }

    private Span createSpan(FilterProfile filterProfile, String context, String documentId, String text,
                            String type, int start, int end, double confidence, SensitivityLevel sensitivityLevel) throws IOException {

        //boolean filtered = false;

        // TODO: PHL-3: Apply sensitivity level to NER entities.

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(NerFilterStrategy.CONFIDENCE, confidence);
        attributes.put(NerFilterStrategy.TYPE, type);

        final String replacement = getReplacement(filterProfile, context, documentId, text, attributes);
        final Span span = Span.make(start, end, FilterType.NER_ENTITY, context, documentId, confidence, replacement);

        // Send the entity to the metrics service for reporting.
        metricsService.reportEntitySpan(span);

        return span;

        // TODO: PHL-2: Revisit intelligent NER filtering based on confidence values.

        /*// Store this entity's confidence in the statistics.
        stats.computeIfAbsent(context, k -> new DescriptiveStatistics(STATISTICS_WINDOW_SIZE)).addValue(span.getConfidence());

        if(stats.get(context).getN() >= SUFFICIENT_VALUES_COUNT) {

            final double mean = stats.get(context).getMean();

            if(sensitivityLevel == SensitivityLevel.LOW){

                final double threshold = mean - 10;

                if(span.getConfidence() >= threshold) {
                    filtered = true;
                }

            } else if(sensitivityLevel == SensitivityLevel.MEDIUM) {

                final double threshold = mean - 20;

                if(span.getConfidence() >= threshold) {
                    filtered = true;
                }

            } else if(sensitivityLevel == SensitivityLevel.HIGH) {

                final double threshold = mean - 30;

                if(span.getConfidence() >= threshold) {
                    filtered = true;
                }

            } else {

                // Invalid sensitivity level. Will not happen.

            }

        }

        if(!filtered) {
            return span;
        } else {
            return null;
        }

        return span;*/

    }

}
