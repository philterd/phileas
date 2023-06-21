/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.services.filters.ai.python;

import ai.philterd.phileas.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.dynamic.NerFilter;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.profile.FilterProfile;
import ai.philterd.phileas.model.services.MetricsService;
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
import java.util.concurrent.TimeUnit;

public class PersonsV1Filter extends NerFilter {

    private static final Logger LOGGER = LogManager.getLogger(PersonsV1Filter.class);

    private final int timeoutSec;
    private final int maxIdleConnections;
    private final int keepAliveDurationMs;
    private final boolean removePunctuation;

    private transient PyTorchRestService service;
    private String tag;

    // Response will look like:
    // [{"text": "George Washington", "tag": "PER", "score": 0.2987019270658493, "start": 0, "end": 17}, {"text": "Virginia", "tag": "LOC", "score": 0.3510116934776306, "start": 95, "end": 103}]

    public PersonsV1Filter(FilterConfiguration filterConfiguration,
                           PhileasConfiguration phileasConfiguration,
                           String tag,
                           Map<String, DescriptiveStatistics> stats,
                           MetricsService metricsService,
                           boolean removePunctuation,
                           Map<String, Double> thresholds) {

        super(filterConfiguration, stats, metricsService, thresholds, FilterType.PERSON);

        this.removePunctuation = removePunctuation;
        this.tag = tag;
        this.timeoutSec = phileasConfiguration.nerTimeoutSec();
        this.maxIdleConnections = phileasConfiguration.nerMaxIdleConnections();
        this.keepAliveDurationMs = phileasConfiguration.nerKeepAliveDurationMs();

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(timeoutSec, TimeUnit.SECONDS)
                .writeTimeout(timeoutSec, TimeUnit.SECONDS)
                .readTimeout(timeoutSec, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDurationMs, TimeUnit.MILLISECONDS))
                .build();

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(phileasConfiguration.philterNerEndpoint())
                .client(okHttpClient)
                .callFactory(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(PyTorchRestService.class);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        // Remove punctuation if instructed to do so.
        // It is replacing each punctuation mark with an empty space. This will allow span indexes
        // to remain constant as opposed to removing the punctuation and causing the string to then
        // have a shorter length.
        if(removePunctuation) {
            input = input.replaceAll("\\p{Punct}", " ");
        }

        final Response<PyTorchResponse> response = service.process(context, documentId, piece, input).execute();

        if(response.isSuccessful()) {

            final List<PhileasSpan> phileasSpans = response.body().getSpans();

            if(phileasSpans != null) {

                for (final PhileasSpan phileasSpan : phileasSpans) {

                    // Only interested in spans matching the tag we are looking for, e.g. PER, LOC.
                    if (StringUtils.equalsIgnoreCase(phileasSpan.getTag(), tag)) {

                        // Check the confidence threshold.
                        if(!thresholds.containsKey(tag.toUpperCase()) || phileasSpan.getScore() >= thresholds.get(tag.toUpperCase())) {

                            // Get the window of text surrounding the token.
                            final String[] window = getWindow(input, phileasSpan.getStart(), phileasSpan.getEnd());

                            final Span span = createSpan(filterProfile.getName(), input, context, documentId, phileasSpan.getText(),
                                    window, phileasSpan.getTag(), phileasSpan.getStart(), phileasSpan.getEnd(), phileasSpan.getScore());

                            // Span will be null if no span was created due to it being excluded.
                            if (span != null) {
                                spans.add(span);
                            }

                        }

                    }

                }

                LOGGER.debug("Returning {} NER spans.", spans.size());

                return new FilterResult(context, documentId, piece, spans);

            } else {

                // We received a null list of spans from philter-ner. It means something went wrong.
                throw new IOException("Unable to process document. Received error response from philter-ner.");

            }

        } else {

            // The request to philter-ner was not successful.
            throw new IOException("Unable to process document. Received error response from philter-ner.");

        }

    }

    @Override
    public int getOccurrences(FilterProfile filterProfile, String input) throws Exception {

        return filter(filterProfile, "none", "none", 0, input).getSpans().size();

    }

    private Span createSpan(String filterProfile, String input, String context, String documentId, String text,
                            String[] window, String classification, int start, int end, double confidence) throws Exception {

        final Replacement replacement = getReplacement(filterProfile, context, documentId, text, window, confidence, classification, null);

        if(StringUtils.equals(replacement.getReplacement(), text)) {

            // If the replacement is the same as the token then there is no span.
            // A condition in the strategy excluded it.

            return null;

        } else {

            // Is this term ignored?
            final boolean ignored = isIgnored(text);

            return Span.make(start, end, FilterType.PERSON, context, documentId, confidence, text, replacement.getReplacement(), replacement.getSalt(), ignored, window);

        }

    }

}