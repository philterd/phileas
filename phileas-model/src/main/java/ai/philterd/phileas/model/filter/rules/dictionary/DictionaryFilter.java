/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.model.filter.rules.dictionary;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.RulesFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

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

    public List<String> getNgrams(String text, int n) {
        List<String> ngrams = new ArrayList<>();
        String[] words = text.split(" ");

        for (int i = 0; i <= words.length - n; i++) {
            StringBuilder ngram = new StringBuilder();
            for (int j = 0; j < n; j++) {
                ngram.append(words[i + j]);
                if (j < n - 1) {
                    ngram.append(" ");
                }
            }
            ngrams.add(ngram.toString());
        }

        return ngrams;
    }

}
