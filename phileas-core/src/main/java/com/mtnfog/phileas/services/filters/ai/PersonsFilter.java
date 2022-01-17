package com.mtnfog.phileas.services.filters.ai;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.filter.dynamic.NerFilter;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.services.MetricsService;
import com.mtnfog.phileas.service.ai.OnnxNer;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersonsFilter extends NerFilter {

    private final OnnxNer onnxNer;
    private Map<Integer, String> id2Labels;

    public PersonsFilter(FilterConfiguration filterConfiguration,
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

        this.onnxNer = new OnnxNer(model, vocab, id2Labels);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = onnxNer.find(input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

    @Override
    public int getOccurrences(FilterProfile filterProfile, String input) throws Exception {
        return 0;
    }

}
