package ai.philterd.phileas.model.filter.rules.dictionary;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.DocumentAnalysis;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.profile.FilterProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A filter that operates on a bloom filter.
 */
public class BloomFilterDictionaryFilter extends DictionaryFilter {

    private static final Logger LOGGER = LogManager.getLogger(BloomFilterDictionaryFilter.class);

    private BloomFilter<String> bloomFilter;
    private Set<String> lowerCaseTerms;

    /**
     * Creates a new bloom filter-based filter.
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     * @param terms
     * @param classification
     * @param fpp
     */
    public BloomFilterDictionaryFilter(FilterType filterType,
                                       FilterConfiguration filterConfiguration,
                                       Set<String> terms,
                                       String classification,
                                       double fpp) {

        super(filterType, filterConfiguration);

        this.lowerCaseTerms = new HashSet<>();
        this.bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), terms.size(), fpp);
        this.classification = classification;

        // Find the max n-gram size. It is equal to the maximum
        // number of spaces in any single dictionary entry.
        for(final String term : terms) {
            final String[] split = term.split("\\s");
            if(split.length > maxNgramSize) {
                maxNgramSize = split.length;
            }
        }
        LOGGER.info("Max ngram size is {}", maxNgramSize);

        // Lowercase the terms and add each to the bloom filter.
        LOGGER.info("Creating bloom filter from {} terms.", terms.size());
        terms.forEach(t -> lowerCaseTerms.add(t.toLowerCase()));
        lowerCaseTerms.forEach(t -> bloomFilter.put(t.toLowerCase()));

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String text) throws Exception {

        final List<Span> spans = new LinkedList<>();

        // PHL-150: Break the input text into n-grams of size max n-grams and smaller.
        final ShingleFilter ngrams = getNGrams(maxNgramSize, text);

        final OffsetAttribute offsetAttribute = ngrams.getAttribute(OffsetAttribute.class);
        final CharTermAttribute termAttribute = ngrams.getAttribute(CharTermAttribute.class);

        try {

            ngrams.reset();

            while (ngrams.incrementToken()) {

                final String token = termAttribute.toString();
                final String lowerCaseToken = token.toLowerCase();

                if(bloomFilter.mightContain(lowerCaseToken)) {

                    if(lowerCaseTerms.contains(lowerCaseToken)) {

                        // Set the meta values for the span.
                        final boolean isIgnored = ignored.contains(token);
                        final int characterStart = offsetAttribute.startOffset();
                        final int characterEnd = offsetAttribute.endOffset();
                        final double confidence = 1.0;
                        final String[] window = getWindow(text, characterStart, characterEnd);

                        // Get the original token to get the right casing.
                        final String originalToken = text.substring(characterStart, characterEnd);

                        final Replacement replacement = getReplacement(filterProfile.getName(), context, documentId,
                                originalToken, window, confidence, classification, null);

                        spans.add(Span.make(characterStart, characterEnd, getFilterType(), context, documentId,
                                confidence, originalToken, replacement.getReplacement(), replacement.getSalt(), isIgnored, window));

                    }

                }

            }

        } catch (IOException ex) {

            LOGGER.error("Error enumerating tokens.", ex);

        } finally {
            try {
                ngrams.end();
                ngrams.close();
            } catch (IOException e) {
                // Do nothing.
            }
        }

        return new FilterResult(context, documentId, spans);

    }

}
