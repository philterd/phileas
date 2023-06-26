package ai.philterd.phileas.services.filters.ai.opennlp;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.dynamic.NerFilter;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.profile.FilterProfile;
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

    public PersonsV3Filter(FilterConfiguration filterConfiguration,
                           String modelFile,
                           Map<String, DescriptiveStatistics> stats,
                           MetricsService metricsService,
                           Map<String, Double> thresholds) throws Exception {

        super(filterConfiguration, stats, metricsService, thresholds, FilterType.PERSON);

        LOGGER.info("Initializing persons filter with model {}", modelFile);

        // Load the NER filter for the given modelFile.
        final InputStream tokenNameFinderInputStream = new FileInputStream(modelFile);
        final TokenNameFinderModel tokenNameFinderModel = new TokenNameFinderModel(tokenNameFinderInputStream);
        nameFinderME = new NameFinderME(tokenNameFinderModel);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

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
            final Replacement replacement = getReplacement(filterProfile.getName(), context, documentId, text, window, openNlpSpan.getProb(), classification, null);
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
    public int getOccurrences(FilterProfile filterProfile, String input) throws Exception {
        return 0;
    }

}
