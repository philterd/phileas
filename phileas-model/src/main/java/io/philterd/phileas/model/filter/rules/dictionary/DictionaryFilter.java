package io.philterd.phileas.model.filter.rules.dictionary;

import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.filter.FilterConfiguration;
import io.philterd.phileas.model.filter.rules.RulesFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.io.StringReader;

/**
 * A filter that operates on a preset list of dictionary words.
 */
public abstract class DictionaryFilter extends RulesFilter {

    // Lucene requires a min size of 2 for the ShingleFilter.
    protected int maxNgramSize = 2;

    /**
     * Creates a new dictionary-based filter.
     * @param filterType
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     */
    public DictionaryFilter(FilterType filterType, FilterConfiguration filterConfiguration) {
        super(filterType, filterConfiguration);
    }

    /**
     * Gets the n-grams from text having length 2 to <code>maxNgramSize</code>.
     * @param maxNgramSize The maximum size of the n-grams.
     * @param text The text to split.
     * @return The n-grams.
     */
    public ShingleFilter getNGrams(int maxNgramSize, String text) {

        // The standard analyzer lowercases the text.
        final StandardAnalyzer analyzer = new StandardAnalyzer();

        // Tokenize the input text.
        final TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text));

        // Make n-grams from the tokens.
        return new ShingleFilter(tokenStream, 2, maxNgramSize);

    }

}
