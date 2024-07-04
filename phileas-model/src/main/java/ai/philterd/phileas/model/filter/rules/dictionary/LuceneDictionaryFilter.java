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
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Policy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.LuceneLevenshteinDistance;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NoLockFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A filter that operates on a Lucene index.
 */
public class LuceneDictionaryFilter extends DictionaryFilter implements Serializable, Closeable {

    private static final Logger LOGGER = LogManager.getLogger(LuceneDictionaryFilter.class);

    private final SpellChecker spellChecker;
    private final LevenshteinDistance distanceFunction;
    private final SensitivityLevel sensitivityLevel;
    private final boolean capitalized;
    private int policyIndex = 0;

    /**
     * Creates a new Lucene dictionary filter.
     * @param filterConfiguration @param filterConfiguration The {@link FilterConfiguration} for the filter.
     * @param indexDirectory The path to the index on disk.
     * @param sensitivityLevel
     * @param capitalized
     * @throws IOException Thrown if the index cannot be opened or accessed.
     */
    public LuceneDictionaryFilter(final FilterType filterType,
                                  final FilterConfiguration filterConfiguration,
                                  final String indexDirectory,
                                  final SensitivityLevel sensitivityLevel,
                                  final boolean capitalized) throws IOException {

        super(filterType, filterConfiguration);

        LOGGER.info("Loading {} index from {}", filterType, indexDirectory);

        this.distanceFunction = new LevenshteinDistance();
        this.sensitivityLevel = sensitivityLevel;
        this.capitalized = capitalized;

        // Load the index for fuzzy search.
        this.spellChecker = new SpellChecker(FSDirectory.open(Paths.get(indexDirectory), NoLockFactory.INSTANCE));
        this.spellChecker.setStringDistance(new LuceneLevenshteinDistance());
        this.spellChecker.setAccuracy(0.0f);

    }

    /**
     * Creates a new Lucene dictionary filter from a list of custom terms.
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     * @param sensitivityLevel
     * @param terms
     * @param capitalized
     * @param type
     * @param policyIndex
     * @throws IOException Thrown if the index cannot be opened or accessed.
     */
    public LuceneDictionaryFilter(FilterType filterType,
                                  FilterConfiguration filterConfiguration,
                                  SensitivityLevel sensitivityLevel,
                                  Set<String> terms,
                                  boolean capitalized,
                                  String type,
                                  int policyIndex) throws IOException {

        super(filterType, filterConfiguration);

        LOGGER.info("Creating custom dictionary filter for custom type [{}]", type);

        this.distanceFunction = new LevenshteinDistance();
        this.sensitivityLevel = sensitivityLevel;
        this.capitalized = capitalized;
        this.policyIndex = policyIndex;

        // Find the max n-gram size. It is equal to the maximum
        // number of spaces in any single dictionary entry.
        for(final String term : terms) {
            final String[] split = term.split("\\s");
            if(split.length > this.maxNgramSize) {
                this.maxNgramSize = split.length;
            }
        }
        LOGGER.info("Max ngram size is {}", maxNgramSize);

        // Write the list of terms to a file in a temporary directory.
        final Path pathToIndex = Files.createTempDirectory("philter-name-index");
        final FileAttribute<Set<PosixFilePermission>> fileAttributes = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));
        final Path fileToIndex = Files.createTempFile(pathToIndex, "philter", type, fileAttributes);
        FileUtils.writeLines(fileToIndex.toFile(), terms);

        // Make a temp directory to hold the new index.
        final Path indexDirectory = Files.createTempDirectory(type + "-index-custom");

        LOGGER.info("Creating index of type {} from file {}.", type, pathToIndex);

        // The Lucene StandardAnalyzer uses the StandardTokenizer. It removes punctuation and stop words.
        // "Filters StandardTokenizer with StandardFilter, LowerCaseFilter and StopFilter, using a list of English stop words."
        // https://lucene.apache.org/core/8_1_1/core/org/apache/lucene/analysis/standard/StandardAnalyzer.html

        this.spellChecker = new SpellChecker(FSDirectory.open(pathToIndex, NoLockFactory.INSTANCE));
        this.spellChecker.indexDictionary(new PlainTextDictionary(fileToIndex), new IndexWriterConfig(new StandardAnalyzer(EnglishAnalyzer.getDefaultStopSet())), false);
        this.spellChecker.setStringDistance(new LuceneLevenshteinDistance());
        this.spellChecker.setAccuracy(0.0f);

        LOGGER.info("Custom index for type [{}] created at {}", type, indexDirectory);

    }

    @Override
    public void close() throws IOException {

        spellChecker.close();

    }

    @Override
    public FilterResult filter(Policy policy, String context, String documentId, int piece, String text,
                               final Map<String, String> attributes) throws Exception {

        final List<Span> spans = new LinkedList<>();

        if(policy.getIdentifiers().hasFilter(filterType)) {

            try(final Analyzer analyzer = new StandardAnalyzer()) {

                LOGGER.debug("Using sensitivity level = " + sensitivityLevel.getName());

                // Get the n-grams in the input.
                final ShingleFilter ngrams = getNGrams(5, text);

                final OffsetAttribute offsetAttribute = ngrams.getAttribute(OffsetAttribute.class);
                final CharTermAttribute termAttribute = ngrams.getAttribute(CharTermAttribute.class);

                try {

                    ngrams.reset();

                    while (ngrams.incrementToken()) {

                        final String token = termAttribute.toString();

                        // An underscore indicates Lucene removed a stopword.
                        if(!token.contains("_")) {

                            //LOGGER.info("Looking at token [{}]", token);

                            boolean isMatch = false;

                            if(spellChecker.exist(token)) {

                                //LOGGER.info("Exact match on token '{}'", token);

                                // The token has an identical match in the index.
                                isMatch = true;

                            } else {

                                // Do a fuzzy search against the index.
                                final String[] tokenSuggestions = spellChecker.suggestSimilar(token, 3);
                                LOGGER.debug("{} suggestions for '{}': {}", tokenSuggestions.length, token, tokenSuggestions);

                                if (tokenSuggestions.length > 0) {

                                    int distance = 0;

                                    // Calculate the distance.
                                    if(sensitivityLevel == SensitivityLevel.AUTO) {

                                        // Automatically adjust the distance based on the length.
                                        // https://www.elastic.co/guide/en/elasticsearch/reference/current/common-options.html#fuzziness
                                        /*0..2
                                        Must match exactly
                                        3..5
                                        One edit allowed
                                                >5
                                        Two edits allowed*/

                                        if(token.length() < 3) {
                                            distance = 0;
                                        } else if(token.length() >= 3 && token.length() <= 5) {
                                            distance = 1;
                                        } else {
                                            distance = 2;
                                        }

                                    } else if(sensitivityLevel == SensitivityLevel.LOW) {
                                        distance = 0;
                                    } else if(sensitivityLevel == SensitivityLevel.MEDIUM) {
                                        distance = 1;
                                    } else if(sensitivityLevel == SensitivityLevel.HIGH) {
                                        distance = 2;
                                    }

                                    //LOGGER.debug("Using distance value {}", distance);

                                    for (final String suggestion : tokenSuggestions) {

                                        final int d = distanceFunction.apply(token.toUpperCase(), suggestion.toUpperCase());

                                        if (d <= distance) {
                                            //LOGGER.debug("distance for {} and {} is {}", token, suggestion, d);
                                            isMatch = true;
                                        }

                                    }

                                }

                            }

                            if (isMatch) {

                                if(!capitalized || (capitalized && Character.isUpperCase(text.charAt(0)))) {

                                    // Set the meta values for the span.

                                    // Is this term ignored?
                                    final boolean ignored = isIgnored(text);

                                    final int characterStart = offsetAttribute.startOffset();
                                    final int characterEnd = offsetAttribute.endOffset();
                                    final String[] window = getWindow(text, characterStart, characterEnd);
                                    final double confidence = spellChecker.getAccuracy();

                                    // Get the replacement token or the original token if no filter strategy conditions are met.
                                    final Replacement replacement = getReplacement(policy, context, documentId, token, window, confidence, classification, attributes, null);

                                    // Add the span to the list.
                                    spans.add(Span.make(characterStart, characterEnd, getFilterType(), context, documentId, confidence, token, replacement.getReplacement(), replacement.getSalt(), ignored, window));

                                }

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

            }

        }

        return new FilterResult(context, documentId, spans);

    }

    @Override
    public int getOccurrences(Policy policy, String input, Map<String, String> attributes) throws Exception {

        return filter(policy, "none", "none", 0, input, attributes).getSpans().size();

    }

    /**
     * Run this class to create a Lucene index from a text file.
     * Usage: java -jar ./LuceneDictionaryFilter state states.txt
     * @param args Command line arguments.
     * @throws IOException Thrown if the index cannot be created.
     */
    public static void main(String[] args) throws IOException {

        // The location of the file containing the lines to index.
        final Path filetoIndex = Paths.get("/mtnfog/code/philter/phileas/data/index-data/surnames");

        // The name of the file minus the extension is the type of index.
        final String type = FilenameUtils.removeExtension(filetoIndex.toFile().getName());

        // Make a temp directory to hold the new index.
        final Path indexDirectory = Files.createTempDirectory(type);

        LOGGER.info("Creating index of type {} from file {}.", type, filetoIndex);

        // The Lucene StandardAnalyzer uses the StandardTokenizer. It removes punctuation and stop words.
        // "Filters StandardTokenizer with StandardFilter, LowerCaseFilter and StopFilter, using a list of English stop words."
        // https://lucene.apache.org/core/8_1_1/core/org/apache/lucene/analysis/standard/StandardAnalyzer.html

        try(final SpellChecker spellChecker = new SpellChecker(FSDirectory.open(indexDirectory))) {

            spellChecker.indexDictionary(new PlainTextDictionary(filetoIndex), new IndexWriterConfig(new StandardAnalyzer(EnglishAnalyzer.getDefaultStopSet())), true);

        }

        LOGGER.info("Index created at: " + indexDirectory);

        final SpellChecker spellChecker = new SpellChecker(FSDirectory.open(indexDirectory, NoLockFactory.INSTANCE));
        spellChecker.setStringDistance(new LuceneLevenshteinDistance());
        spellChecker.setAccuracy(0.0f);

        // Test the index.
        LOGGER.info("Index contains Jones: {}", spellChecker.exist("Jones"));
        final String[] suggestions = spellChecker.suggestSimilar("Jones", 2);
        LOGGER.info("Suggestions for Jones: {}", suggestions.length);
        for(final String s : suggestions) {
            LOGGER.info("Suggestion: {}", s);
        }

    }

}
