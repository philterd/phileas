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
package ai.philterd.phileas.services.filters.ai.onnx;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.dynamic.NerFilter;
import ai.philterd.phileas.model.objects.Entity;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.profile.FilterProfile;
import ai.philterd.phileas.model.services.MetricsService;
import ai.philterd.phileas.service.ai.onnx.OnnxNer;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.text.WordUtils;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PersonsV2Filter extends NerFilter {

    private final OnnxNer onnxNer;
    private Map<Integer, String> id2Labels;

    public PersonsV2Filter(FilterConfiguration filterConfiguration,
                           String modelFile,
                           String vocabFile,
                           Map<String, DescriptiveStatistics> stats,
                           MetricsService metricsService,
                           Map<String, Double> thresholds) throws Exception {

        super(filterConfiguration, stats, metricsService, thresholds, FilterType.PERSON);

        final File model = new File(modelFile);
        final File vocab = new File(vocabFile);

        // These values come from the config.json that was used to train the model.
        this.id2Labels = new HashMap<>();
        id2Labels.put(0, "O");
        id2Labels.put(1, "B-MISC");
        id2Labels.put(2, "I-MISC");
        id2Labels.put(3, "B-PER");
        id2Labels.put(4, "I-PER");
        id2Labels.put(5, "B-ORG");
        id2Labels.put(6, "I-ORG");
        id2Labels.put(7, "B-LOC");
        id2Labels.put(8, "I-LOC");

        // Initialize the sentence detector.
        // TODO: Load this model locally.
        final SentenceDetector sentenceDetector = new SentenceDetectorME("en");

        LOGGER.info("Initializing persons filter with model {}", modelFile);
        this.onnxNer = new OnnxNer(model, vocab, id2Labels, sentenceDetector);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        // Remove line breaks.
        input = input.replaceAll("\n", " ");

        // Convert all caps words to just first letter capitalized.
        // This changes things like JAMES SMITH to James Smith which the model likes better.
        input = WordUtils.capitalizeFully(input);

        final List<Entity> entities = onnxNer.find(input, context, documentId);

        final List<Span> spans = new LinkedList<>();

        for(final Entity entity : entities) {

            final String[] window = getWindow(input, entity.getCharacterStart(), entity.getCharacterEnd());
            final Replacement replacement = getReplacement(filterProfile.getName(), context, documentId, entity.getText(), window, entity.getConfidence(), classification, null);
            final boolean isIgnored = ignored.contains(entity.getText());

            final Span span = Span.make(
                    entity.getCharacterStart(),
                    entity.getCharacterEnd(),
                    entity.getFilterType(),
                    entity.getContext(),
                    entity.getDocumentId(),
                    entity.getConfidence(),
                    entity.getText(),
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
    public int getOccurrences(FilterProfile filterProfile, String input) throws Exception {
        return 0;
    }

}
