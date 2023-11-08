/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.filters.ai.opennlp;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.dynamic.NerFilter;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.services.MetricsService;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PersonsV3Filter extends NerFilter {

    private final NameFinderME nameFinderME;

    public PersonsV3Filter(final FilterConfiguration filterConfiguration,
                           String modelFileName,
                           final Map<String, DescriptiveStatistics> stats,
                           final MetricsService metricsService,
                           final Map<String, Double> thresholds) throws Exception {

        super(filterConfiguration, stats, metricsService, thresholds, FilterType.PERSON);

        LOGGER.info("Initializing persons filter with model {}", modelFileName);

        // Load the NER filter for the given modelFile.
        // If the filename starts with "classpath:" it is in the jar's resources.
        if(modelFileName.startsWith("classpath:")) {

            modelFileName = modelFileName.replace("classpath:", "");
            final InputStream tokenNameFinderInputStream = PersonsV3Filter.this.getClass().getClassLoader().getResourceAsStream(modelFileName);
            final TokenNameFinderModel tokenNameFinderModel = new TokenNameFinderModel(tokenNameFinderInputStream);
            nameFinderME = new NameFinderME(tokenNameFinderModel);

        } else {

            final InputStream tokenNameFinderInputStream = new FileInputStream(modelFileName);
            final TokenNameFinderModel tokenNameFinderModel = new TokenNameFinderModel(tokenNameFinderInputStream);
            nameFinderME = new NameFinderME(tokenNameFinderModel);

        }

    }

    @Override
    public FilterResult filter(final Policy policy, final String context, final String documentId, final int piece,
                               String input, Map<String, String> attributes) throws Exception {

        // The final list of spans identified in the text.
        final List<Span> spans = new LinkedList<>();

        // Combine multiple spaces into a single space.
        // This is to allow finding the identiifed span in the original text.
        input = input.trim().replaceAll(" +", " ");

        // Split the input into an array based on whitespace.
        final String[] inputSplit = input.split("\\s+");

        final opennlp.tools.util.Span[] openNlpSpans = nameFinderME.find(inputSplit);

        for (final opennlp.tools.util.Span openNlpSpan : openNlpSpans) {

            final String[] sub = ArrayUtils.subarray(inputSplit, openNlpSpan.getStart(), openNlpSpan.getEnd());
            final String text = String.join(" ", sub);

            final int start = input.indexOf(text);
            final int end = start + text.length();

            final String[] window = getWindow(input, start, end);
            final Replacement replacement = getReplacement(policy, context, documentId, text, window, openNlpSpan.getProb(), classification, attributes, null);
            final boolean isIgnored = ignored.contains(text);

            final Span span = Span.make(
                    start,
                    end,
                    FilterType.PERSON,
                    context,
                    documentId,
                    openNlpSpan.getProb(),
                    text,
                    replacement.getReplacement(),
                    replacement.getSalt(),
                    isIgnored,
                    window
            );

            spans.add(span);

        }

        // Drop overlapping spans.
        final List<Span> nonoverlappingSpans = Span.dropOverlappingSpans(spans);

        return new FilterResult(context, documentId, nonoverlappingSpans);

    }

    @Override
    public int getOccurrences(final Policy policy, final String input, Map<String, String> attributes) throws Exception {
        return 0;
    }

}
