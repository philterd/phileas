package com.mtnfog.phileas.model.filter.rules.dictionary;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.ibm.icu.util.StringTokenizer;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A filter that operates on a bloom filter.
 */
public class BloomFilterDictionaryFilter extends DictionaryFilter implements Serializable {

    private static final Logger LOGGER = LogManager.getLogger(BloomFilterDictionaryFilter.class);

    private BloomFilter<String> bloomFilter;
    private Set<String> terms;
    private String classification;

    public BloomFilterDictionaryFilter(FilterType filterType,
                                       List<? extends AbstractFilterStrategy> strategies,
                                       Set<String> terms,
                                       String classification,
                                       AnonymizationService anonymizationService,
                                       AlertService alertService,
                                       Set<String> ignored,
                                       Crypto crypto,
                                       int windowSize) {

        super(filterType, strategies, anonymizationService, alertService, ignored, crypto, windowSize);

        this.bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()),terms.size(),0.01);
        this.terms = terms;
        this.classification = classification;

        LOGGER.info("Creating bloom filter from {} terms.", terms.size());
        for(final String term : terms) {
            this.bloomFilter.put(term);
        }

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String text) throws Exception {

        final List<Span> spans = new LinkedList<>();

        // Tokenize the text.
        final StringTokenizer st = new StringTokenizer(text);

        int index = 0;

        while(st.hasMoreTokens()) {

            final String token = st.nextToken();

            if(bloomFilter.mightContain(token)) {

                if(terms.contains(token)) {

                    // Set the meta values for the span.
                    final boolean isIgnored = ignored.contains(token);
                    final int characterStart = 0;
                    final int characterEnd = 0;
                    final double confidence = 1.0;
                    final String[] window = getWindow(text, characterStart, characterEnd);

                    final String replacement = getReplacement(filterProfile.getName(), label, context, documentId, token, confidence, classification);
                    spans.add(Span.make(characterStart, characterEnd, getFilterType(), context, documentId, confidence, token, replacement, isIgnored, window));

                }

            }

            // This accounts for the token length but not any punctuation or whitespace
            // so the index will always remain behind the actual location of the token.
            index += token.length();

        }

        return spans;

    }

}
