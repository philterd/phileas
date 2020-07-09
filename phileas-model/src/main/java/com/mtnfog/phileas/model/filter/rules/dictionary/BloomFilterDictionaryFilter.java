package com.mtnfog.phileas.model.filter.rules.dictionary;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.Charset;
import java.text.BreakIterator;
import java.util.*;

/**
 * A filter that operates on a bloom filter.
 */
public class BloomFilterDictionaryFilter extends DictionaryFilter {

    private static final Logger LOGGER = LogManager.getLogger(BloomFilterDictionaryFilter.class);

    private BloomFilter<String> bloomFilter;
    private Set<String> lowerCaseTerms;

    public BloomFilterDictionaryFilter(FilterType filterType,
                                       List<? extends AbstractFilterStrategy> strategies,
                                       Set<String> terms,
                                       String classification,
                                       double fpp,
                                       AnonymizationService anonymizationService,
                                       AlertService alertService,
                                       Set<String> ignored,
                                       Crypto crypto,
                                       int windowSize) {

        super(filterType, strategies, anonymizationService, alertService, ignored, crypto, windowSize);

        this.lowerCaseTerms = new HashSet<>();
        this.bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), terms.size(), fpp);
        this.classification = classification;

        // Lowercase the terms and add each to the bloom filter.
        LOGGER.info("Creating bloom filter from {} terms.", lowerCaseTerms.size());
        terms.forEach(t -> lowerCaseTerms.add(t.toLowerCase()));
        lowerCaseTerms.forEach(t -> bloomFilter.put(t.toLowerCase()));

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String text) throws Exception {

        final List<Span> spans = new LinkedList<>();

        // Tokenize the text.
        //final StringTokenizer stringTokenizer = new StringTokenizer(text);
        final BreakIterator breakIterator = BreakIterator.getWordInstance(Locale.US);
        breakIterator.setText(text);

        int start = breakIterator.first();
        for(int end = breakIterator.next(); end != BreakIterator.DONE; start = end, end = breakIterator.next()) {

            final String token = text.substring(start, end);
            final String lowerCaseToken = token.toLowerCase();

            if(bloomFilter.mightContain(lowerCaseToken)) {

                if(lowerCaseTerms.contains(lowerCaseToken)) {

                    // Set the meta values for the span.
                    final boolean isIgnored = ignored.contains(token);
                    final int characterStart = start;
                    final int characterEnd = end;
                    final double confidence = 1.0;
                    final String[] window = getWindow(text, characterStart, characterEnd);

                    final Replacement replacement = getReplacement(filterProfile.getName(), context, documentId, token, confidence, classification);
                    spans.add(Span.make(characterStart, characterEnd, getFilterType(), context, documentId, confidence, token, replacement.getReplacement(), replacement.getSalt(), isIgnored, window));

                }

            }

        }

        return new FilterResult(context, documentId, spans);

    }

}
