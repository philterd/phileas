package ai.philterd.phileas.model.filter.rules.dictionary;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Position;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Policy;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FuzzyDictionaryFilter extends DictionaryFilter implements Serializable {

    private final SensitivityLevel sensitivityLevel;
    private final Map<String, Pattern> dictionary;

    public FuzzyDictionaryFilter(final FilterType filterType, final FilterConfiguration filterConfiguration,
                                 final SensitivityLevel sensitivityLevel) throws IOException {
        super(filterType, filterConfiguration);

        this.sensitivityLevel = sensitivityLevel;
        this.dictionary = loadData(filterType);
    }

    @Override
    public FilterResult filter(Policy policy, String context, String documentId, int piece, String input, Map<String, String> attributes) throws Exception {

        final List<Span> spans = new LinkedList<>();

        if(policy.getIdentifiers().hasFilter(filterType)) {

            // Build ngrams from the input text.
            final Map<Integer, Map<String, Position>> ngrams = new HashMap<>();
            ngrams.put(0, splitWithIndexes(input, " "));
            ngrams.put(1, getNgrams(input, 1));
            ngrams.put(2, getNgrams(input, 2));
            ngrams.put(3, getNgrams(input, 3));
            ngrams.put(4, getNgrams(input, 4));

            for(final String entry : dictionary.keySet()) {

                final Matcher matcher = dictionary.get(entry).matcher(input);

                // Exact matches.
                if (matcher.find()) {
                    final int startPosition = matcher.start();
                    spans.add(createSpan(input, startPosition, startPosition + entry.length(), 1.0, context, documentId, entry, policy, attributes));
                } else {

                    // Fuzzy matches.
                    final int spacesInEntry = StringUtils.countMatches(entry, " ");

                    // Compare string distance between word and ngrams.
                    for (final String ngram : ngrams.get(spacesInEntry).keySet()) {

                        final LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();
                        final int distance = levenshteinDistance.apply(entry, ngram);
//LOGGER.info("{}, {}, {}", entry, ngram, distance);
                        if (sensitivityLevel == SensitivityLevel.HIGH && distance <= 1) {
                            spans.add(createSpan(input, 0, input.length(), 1.0, context, documentId, entry, policy, attributes));
                        } else if (sensitivityLevel == SensitivityLevel.MEDIUM && distance <= 2) {
                            spans.add(createSpan(input, 0, input.length(), 1.0, context, documentId, entry, policy, attributes));
                        } else if (sensitivityLevel == SensitivityLevel.LOW && distance <= 3) {
                            spans.add(createSpan(input, 0, input.length(), 1.0, context, documentId, entry, policy, attributes));
                        }

                    }

                }

            }

        }

        return new FilterResult(context, documentId, spans);

    }

    private Span createSpan(String text, int characterStart, int characterEnd, double confidence, String context,
                            String documentId, String token, Policy policy, Map<String, String> attributes) throws Exception {

        final boolean ignored = isIgnored(text);
        final String[] window = getWindow(text, characterStart, characterEnd);

        // Get the replacement token or the original token if no filter strategy conditions are met.
        final Replacement replacement = getReplacement(policy, context, documentId, token,
                window, confidence, classification, attributes, null);

        // Add the span to the list.
        return Span.make(characterStart, characterEnd, getFilterType(), context,
                documentId, confidence, token, replacement.getReplacement(),
                replacement.getSalt(), ignored, replacement.isApplied(), window);

    }

    private Map<String, Pattern> loadData(final FilterType filterType) throws IOException {

        final Map<String, Pattern> lines = new HashMap<>();

        final String fileName;

        if(filterType == FilterType.LOCATION_CITY) {
            fileName = "cities";
        } else if(filterType == FilterType.LOCATION_COUNTY) {
            fileName = "counties";
        } else if(filterType == FilterType.LOCATION_STATE) {
            fileName = "states";
        } else if(filterType == FilterType.HOSPITAL) {
            fileName = "hospitals";
        } else if(filterType == FilterType.HOSPITAL_ABBREVIATION) {
            fileName = "hospital-abbreviations";
        } else if(filterType == FilterType.FIRST_NAME) {
            fileName = "names";
        } else if(filterType == FilterType.SURNAME) {
            fileName = "surnames";
        } else {
            throw new IllegalArgumentException("Invalid filter type.");
        }

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {

                final Pattern pattern = Pattern.compile("\\b" + line + "\\b", Pattern.CASE_INSENSITIVE);
                lines.put(line, pattern);

            }
        }

        return lines;

    }

}

