/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.filters.ai.pheye;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.dynamic.NerFilter;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.services.MetricsService;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import okhttp3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PhEyeFilter extends NerFilter {

    private static final Logger LOGGER = LogManager.getLogger(PhEyeFilter.class);

    private final boolean removePunctuation;

    private final transient PhEyeService service;
    private final Collection<String> labels;
    
    public PhEyeFilter(final FilterConfiguration filterConfiguration,
                       final PhEyeConfiguration phEyeConfiguration,
                       final Map<String, DescriptiveStatistics> stats,
                       final MetricsService metricsService,
                       final boolean removePunctuation,
                       final Map<String, Double> thresholds) {

        super(filterConfiguration, stats, metricsService, thresholds, FilterType.PERSON);

        this.removePunctuation = removePunctuation;
        this.labels = phEyeConfiguration.getLabels();

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if(StringUtils.isNotEmpty(phEyeConfiguration.getUsername()) && StringUtils.isNotEmpty(phEyeConfiguration.getPassword())) {
            builder.authenticator(new Authenticator() {
                @Override
                public Request authenticate(final Route route, final okhttp3.Response response) {
                    final String credential = Credentials.basic(phEyeConfiguration.getUsername(), phEyeConfiguration.getPassword());
                    return response.request().newBuilder().header("Authorization", credential).build();
                }
            });
        }

        builder.retryOnConnectionFailure(true);
        builder.connectTimeout(phEyeConfiguration.getTimeout(), TimeUnit.SECONDS);
        builder.writeTimeout(phEyeConfiguration.getTimeout(), TimeUnit.SECONDS);
        builder.readTimeout(phEyeConfiguration.getTimeout(), TimeUnit.SECONDS);
        builder.connectionPool(new ConnectionPool(phEyeConfiguration.getMaxIdleConnections(), phEyeConfiguration.getKeepAliveDurationMs(), TimeUnit.MILLISECONDS));

        final OkHttpClient okHttpClient = builder.build();

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(phEyeConfiguration.getEndpoint())
                .client(okHttpClient)
                .callFactory(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(PhEyeService.class);

    }

    @Override
    public FilterResult filter(final Policy policy, final String context, final String documentId, final int piece,
                               String input, final Map<String, String> attributes) throws Exception {

        final List<Span> spans = new LinkedList<>();

        // Remove punctuation if instructed to do so.
        // It is replacing each punctuation mark with an empty space. This will allow span indexes
        // to remain constant as opposed to removing the punctuation and causing the string to then
        // have a shorter length.
        if(removePunctuation) {
            input = input.replaceAll("\\p{Punct}", " ");
        }
        
        final PhEyeRequest phEyeRequest = new PhEyeRequest();
        phEyeRequest.setText(input);
        phEyeRequest.setContext(context);
        phEyeRequest.setDocumentId(documentId);
        phEyeRequest.setPiece(piece);
        phEyeRequest.setLabels(labels);
        
        final Response<String> response = service.find(phEyeRequest).execute();
        
        if(response.isSuccessful()) {

            final Type listType = new TypeToken<ArrayList<PhEyeSpan>>(){}.getType();
            final List<PhEyeSpan> phEyeSpans = new Gson().fromJson(response.body(), listType);
            
            if (CollectionUtils.isNotEmpty(phEyeSpans)) {

                for (final PhEyeSpan phEyeSpan : phEyeSpans) {

                    // Only interested in spans matching the tag we are looking for, e.g. PER, LOC.
                    if (labels.contains(phEyeSpan.getLabel())) {

                        // Check the confidence threshold.
                        if(!thresholds.containsKey(phEyeSpan.getLabel().toUpperCase()) || phEyeSpan.getScore() >= thresholds.get(phEyeSpan.getLabel().toUpperCase())) {

                            // Get the window of text surrounding the token.
                            final String[] window = getWindow(input, phEyeSpan.getStart(), phEyeSpan.getEnd());

                            final Span span = createSpan(policy, context, documentId, phEyeSpan.getText(),
                                    window, phEyeSpan.getLabel(), phEyeSpan.getStart(), phEyeSpan.getEnd(),
                                    phEyeSpan.getScore(), attributes);

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
    public int getOccurrences(final Policy policy, final String input, Map<String, String> attributes) throws Exception {

        return filter(policy, "none", "none", 0, input, attributes).getSpans().size();

    }

    private Span createSpan(final Policy policy, final String context, final String documentId,
                            final String text, final  String[] window, final String classification, final int start,
                            final int end, final double confidence, Map<String, String> attributes) throws Exception {

        final Replacement replacement = getReplacement(policy, context, documentId, text, window, confidence, classification, attributes, null);

        if(StringUtils.equals(replacement.getReplacement(), text)) {

            // If the replacement is the same as the token then there is no span.
            // A condition in the strategy excluded it.

            return null;

        } else {

            // Is this term ignored?
            final boolean ignored = isIgnored(text);

            return Span.make(start, end, FilterType.PERSON, context, documentId, confidence, text,
                    replacement.getReplacement(), replacement.getSalt(), ignored, replacement.isApplied(), window);

        }

    }

}