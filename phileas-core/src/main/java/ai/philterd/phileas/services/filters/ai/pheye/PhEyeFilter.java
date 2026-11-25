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
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.utils.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;

public class PhEyeFilter extends NerFilter {

    private static final Logger LOGGER = LogManager.getLogger(PhEyeFilter.class);

    private final boolean removePunctuation;

    private final Collection<String> labels;
    private final PhEyeDetector detector;

    // The local model's threshold, for labels without their own; null for the remote service, whose
    // model applies its own default to them.
    private final Double localThreshold;

    // The policy's per-label thresholds, matched to a detected label regardless of case.
    private final Map<String, Double> labelThresholds;

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
        this.localThreshold = StringUtils.isNotEmpty(phEyeConfiguration.getModelPath()) ? phEyeConfiguration.getThreshold() : null;

        this.labelThresholds = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (thresholds != null) {
            this.labelThresholds.putAll(thresholds);
        }

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
                final String message = "PhEye modelPath '" + phEyeConfiguration.getModelPath()
                        + "' is set, but no local detector provider is on the classpath. Add the optional"
                        + " ai.philterd:phileas-pheye-onnx dependency to enable local GLiNER inference.";
                LOGGER.error(message);
                throw new MissingPhEyeProviderException(message);
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
    public Filtered filter(final ContextService contextService, final Policy policy, final String context, final int piece, final String input) throws Exception {

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

        final List<PhEyeSpan> phEyeSpans = detect(formattedInput, context, piece);

        if (CollectionUtils.isNotEmpty(phEyeSpans)) {

            for (final PhEyeSpan phEyeSpan : phEyeSpans) {

                // Only interested in spans matching the tag we are looking for, e.g. PER, LOC, or if there are no labels specified.
                if (labels.isEmpty() || labels.contains(phEyeSpan.getLabel())) {

                    // Keep the span when it scores above its label's threshold.
                    final Double threshold = thresholdFor(phEyeSpan.getLabel());
                    if (threshold == null || phEyeSpan.getScore() > threshold) {

                        // Get the window of text surrounding the token.
                        final String[] window = getWindow(formattedInput, phEyeSpan.getStart(), phEyeSpan.getEnd());

                        // Set the filter type based on the entity's type that's returned from pheye.
                        // The ph-eye-pii-en-* models label person names "name"; older models (and
                        // explicit policies) use "Person". Map both to FilterType.PERSON.
                        final FilterType filterType;
                        final String phEyeLabel = phEyeSpan.getLabel();
                        if (phEyeLabel.equalsIgnoreCase("PERSON") || phEyeLabel.equalsIgnoreCase("name")) {
                            filterType = FilterType.PERSON;
                        } else {
                            filterType = FilterType.OTHER;
                        }

                        // The span offsets line up with the original input because any removed
                        // punctuation was replaced with a same-length space, so use the original
                        // text for the token rather than the (possibly punctuation-stripped) text
                        // the model saw.
                        final String originalToken = input.substring(phEyeSpan.getStart(), phEyeSpan.getEnd());

                        final Span span = createSpan(contextService, policy, context, filterType, originalToken,
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

    /**
     * The threshold a span of the label must score above: the label's own threshold in the policy, or else the
     * local model's threshold. Null for a label without its own threshold on the remote service.
     */
    private Double thresholdFor(final String label) {
        final Double threshold = labelThresholds.get(label);
        return threshold != null ? threshold : localThreshold;
    }

    /**
     * The lowest threshold of the labels detected, which the detector is given so it does not drop a span a
     * label's threshold would keep. With no labels given, the model chooses them, so every configured threshold
     * counts. Null when none is configured, so a remote model applies its own default.
     */
    private Double lowestThreshold() {

        final List<Double> configured = new ArrayList<>();

        if (labels.isEmpty()) {
            configured.addAll(labelThresholds.values());
            if (localThreshold != null) {
                configured.add(localThreshold);
            }
        } else {
            for (final String label : labels) {
                final Double threshold = thresholdFor(label);
                if (threshold != null) {
                    configured.add(threshold);
                }
            }
        }

        return configured.stream().min(Double::compare).orElse(null);

    }

    /**
     * Detects over the whole text: in one call when it fits in what the model examines, otherwise one
     * window at a time, with each span's offsets made relative to the text.
     */
    private List<PhEyeSpan> detect(final String text, final String context, final int piece) throws Exception {

        final int maxWords = detector.maxWords();
        final List<PhEyeWindows.Window> windows = PhEyeWindows.of(text, maxWords, detector.maxWidth());

        final Double lowestThreshold = lowestThreshold();

        if (windows.size() == 1) {
            return detector.detect(text, labels, lowestThreshold, context, piece);
        }

        LOGGER.debug("Detecting over {} windows of at most {} words.", windows.size(), maxWords);

        final List<PhEyeSpan> spans = new LinkedList<>();

        for (final PhEyeWindows.Window window : windows) {

            final List<PhEyeSpan> found = detector.detect(text.substring(window.start(), window.end()), labels,
                    lowestThreshold, context, piece);

            if (found != null) {
                for (final PhEyeSpan span : found) {
                    final int start = span.getStart() + window.start();
                    if (window.owns(start)) {
                        span.setStart(start);
                        span.setEnd(span.getEnd() + window.start());
                        spans.add(span);
                    }
                }
            }

        }

        return spans;

    }

    private Span createSpan(final ContextService contextService, final Policy policy, final String context,
                            final FilterType filterType, final String text, final  String[] window,
                            final String classification, final int start, final int end, final double confidence) throws Exception {

        final Replacement replacement = getReplacement(contextService, policy, context, text, window, confidence, classification, null);

        if(Strings.CI.equals(replacement.getReplacement(), text)) {

            // If the replacement is the same as the token then there is no span.
            // A condition in the strategy excluded it.

            return null;

        } else {

            // Is this term ignored?
            final boolean ignored = isIgnored(text);

            final Span span = Span.make(start, end, filterType, context, confidence, text,
                    replacement.getReplacement(), replacement.getSalt(), ignored, replacement.isApplied(), window, priority);

            // The span's classification is the model's label, as the model returned it, so a caller can tell
            // what the model found even when the label has no filter type of its own.
            span.setClassification(classification);
            span.setColor(replacement.getColor());
            span.setReplacer(t -> getReplacement(contextService, policy, context, t, window, confidence, classification, null));

            return span;

        }

    }

}
