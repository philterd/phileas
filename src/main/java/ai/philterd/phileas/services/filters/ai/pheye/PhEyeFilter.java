/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.dynamic.NerFilter;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class PhEyeFilter extends NerFilter {

    private static final Logger LOGGER = LogManager.getLogger(PhEyeFilter.class);

    private final boolean removePunctuation;

    private final Collection<String> labels;
    private final PhEyeDetector detector;

    public PhEyeFilter(final FilterConfiguration filterConfiguration,
                       final PhEyeConfiguration phEyeConfiguration,
                       final boolean removePunctuation,
                       final Map<String, Double> thresholds,
                       final FilterType filterType,
                       final HttpClient httpClient) {

        super(filterConfiguration, thresholds, filterType);

        this.removePunctuation = removePunctuation;
        this.labels = phEyeConfiguration.getLabels();
        this.detector = createDetector(phEyeConfiguration, httpClient);

    }

    /**
     * Choose the detector based on configuration: a local (ONNX Runtime) detector when a
     * {@code modelPath} is set, otherwise the remote HTTP detector. The local detector is
     * supplied by the optional {@code ai.philterd:phileas-pheye-onnx} module via the
     * {@link PhEyeDetectorProvider} SPI; if it is not on the classpath, fail clearly.
     */
    private static PhEyeDetector createDetector(final PhEyeConfiguration phEyeConfiguration, final HttpClient httpClient) {

        if (StringUtils.isNotEmpty(phEyeConfiguration.getModelPath())) {

            final ServiceLoader<PhEyeDetectorProvider> loader = ServiceLoader.load(PhEyeDetectorProvider.class);
            final Iterator<PhEyeDetectorProvider> providers = loader.iterator();

            if (!providers.hasNext()) {
                throw new IllegalStateException("PhEye modelPath '" + phEyeConfiguration.getModelPath()
                        + "' is set, but no local detector provider is on the classpath. Add the optional"
                        + " ai.philterd:phileas-pheye-onnx dependency to enable local GLiNER inference.");
            }

            try {
                return providers.next().create(phEyeConfiguration);
            } catch (final Exception e) {
                throw new IllegalStateException("Failed to initialize the local PhEye detector for modelPath '"
                        + phEyeConfiguration.getModelPath() + "'.", e);
            }

        }

        return new RemotePhEyeDetector(phEyeConfiguration, httpClient);

    }

    @Override
    public Filtered filter(final Policy policy, final String context, final int piece, final String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        // Remove punctuation if instructed to do so.
        // It is replacing each punctuation mark with an empty space. This will allow span indexes
        // to remain constant as opposed to removing the punctuation and causing the string to then
        // have a shorter length.
        final String formattedInput;
        if (removePunctuation) {
            formattedInput = input.replaceAll("\\p{Punct}", " ");
        } else {
            formattedInput = input;
        }

        final List<PhEyeSpan> phEyeSpans = detector.detect(formattedInput, labels, context, piece);

        if (CollectionUtils.isNotEmpty(phEyeSpans)) {

            for (final PhEyeSpan phEyeSpan : phEyeSpans) {

                // Only interested in spans matching the tag we are looking for, e.g. PER, LOC, or if there are no labels specified.
                if (labels.isEmpty() || labels.contains(phEyeSpan.getLabel())) {

                    // Check the confidence threshold.
                    if (!thresholds.containsKey(phEyeSpan.getLabel().toUpperCase()) || phEyeSpan.getScore() >= thresholds.get(phEyeSpan.getLabel().toUpperCase())) {

                        // Get the window of text surrounding the token.
                        final String[] window = getWindow(formattedInput, phEyeSpan.getStart(), phEyeSpan.getEnd());

                        // Set the filter type based on the entity's type that's returned from pheye.
                        final FilterType filterType;
                        if (phEyeSpan.getLabel().equalsIgnoreCase("PERSON")) {
                            filterType = FilterType.PERSON;
                        } else {
                            filterType = FilterType.OTHER;
                        }

                        // The span offsets line up with the original input because any removed
                        // punctuation was replaced with a same-length space, so use the original
                        // text for the token rather than the (possibly punctuation-stripped) text
                        // the model saw.
                        final String originalToken = input.substring(phEyeSpan.getStart(), phEyeSpan.getEnd());

                        final Span span = createSpan(policy, context, filterType, originalToken,
                                window, phEyeSpan.getLabel(), phEyeSpan.getStart(), phEyeSpan.getEnd(),
                                phEyeSpan.getScore());

                        // Span will be null if no span was created due to it being excluded.
                        if (span != null) {
                            spans.add(span);
                        }

                    }

                }

            }

        }

        LOGGER.debug("Returning {} NER spans from ph-eye.", spans.size());
        return new Filtered(context, piece, spans);

    }

    private Span createSpan(final Policy policy, final String context,
                            final FilterType filterType, final String text, final  String[] window,
                            final String classification, final int start, final int end, final double confidence) throws Exception {

        final Replacement replacement = getReplacement(policy, context, text, window, confidence, classification, null);

        if(Strings.CI.equals(replacement.getReplacement(), text)) {

            // If the replacement is the same as the token then there is no span.
            // A condition in the strategy excluded it.

            return null;

        } else {

            // Is this term ignored?
            final boolean ignored = isIgnored(text);

            return Span.make(start, end, filterType, context, confidence, text,
                    replacement.getReplacement(), replacement.getSalt(), ignored, replacement.isApplied(), window, priority);

        }

    }

}
