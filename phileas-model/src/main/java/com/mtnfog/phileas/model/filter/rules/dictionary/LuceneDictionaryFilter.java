package com.mtnfog.phileas.model.filter.rules.dictionary;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
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
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

/**
 * A filter that operates on a Lucene index.
 */
public class LuceneDictionaryFilter extends DictionaryFilter implements Serializable, Closeable {

    private static final Logger LOGGER = LogManager.getLogger(LuceneDictionaryFilter.class);

    private SpellChecker spellChecker;
    private LevenshteinDistance distanceFunction;
    private SensitivityLevel sensitivityLevel;
    private int filterProfileIndex = 0;

    /**
     * Creates a new Lucene dictionary filter.
     * @param filterType The {@link FilterType type} of filter.
     * @param indexDirectory The path to the index on disk.
     * @param anonymizationService The {@link AnonymizationService} for this filter.
     * @throws IOException Thrown if the index cannot be opened or accessed.
     */
    public LuceneDictionaryFilter(FilterType filterType,
                                  List<? extends AbstractFilterStrategy> strategies,
                                  String indexDirectory,
                                  SensitivityLevel sensitivityLevel,
                                  AnonymizationService anonymizationService,
                                  Set<String> ignored) throws IOException {

        super(filterType, strategies, anonymizationService, ignored);

        LOGGER.info("Loading {} index from {}", filterType, indexDirectory);

        this.distanceFunction = new LevenshteinDistance();
        this.sensitivityLevel = sensitivityLevel;

        // Load the index for fuzzy search.
        this.spellChecker = new SpellChecker(FSDirectory.open(Paths.get(indexDirectory), NoLockFactory.INSTANCE));
        this.spellChecker.setStringDistance(new LuceneLevenshteinDistance());
        this.spellChecker.setAccuracy(0.0f);

    }

    /**
     * Creates a new Lucene dictionary filter from a list of custom terms.
     * @param filterType The {@link FilterType type} of filter.
     * @param anonymizationService The {@link AnonymizationService} for this filter.
     * @throws IOException Thrown if the index cannot be opened or accessed.
     */
    public LuceneDictionaryFilter(FilterType filterType,
                                        List<? extends AbstractFilterStrategy> strategies,
                                        SensitivityLevel sensitivityLevel,
                                        AnonymizationService anonymizationService,
                                        String type,
                                        List<String> terms,
                                        int filterProfileIndex,
                                        Set<String> ignored) throws IOException {

        super(filterType, strategies, anonymizationService, ignored);

        LOGGER.info("Creating custom dictionary filter for custom type [{}]", type);

        this.distanceFunction = new LevenshteinDistance();
        this.sensitivityLevel = sensitivityLevel;
        this.filterProfileIndex = filterProfileIndex;

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
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String text) {

        final List<Span> spans = new LinkedList<>();

        if(filterProfile.getIdentifiers().hasFilter(filterType)) {

            try(final Analyzer analyzer = new StandardAnalyzer()) {

                for(final AbstractFilterStrategy strategy : Filter.getFilterStrategies(filterProfile, filterType, filterProfileIndex)) {

                    LOGGER.info("Using sensitivity level = " + sensitivityLevel.getName());

                    // Tokenize the input text.
                    final TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text));

                    // Make n-grams from the tokens.
                    final ShingleFilter ngrams = new ShingleFilter(tokenStream, 5);

                    final OffsetAttribute offsetAttribute = ngrams.getAttribute(OffsetAttribute.class);
                    final CharTermAttribute termAttribute = ngrams.getAttribute(CharTermAttribute.class);

                    try {

                        ngrams.reset();

                        while (ngrams.incrementToken()) {

                            final String token = termAttribute.toString();

                            // An underscore indicates Lucene removed a stopword.
                            if (!token.contains("_")) {

                                //LOGGER.info("Looking at token '{}'", token);

                                boolean isMatch = false;

                                if (spellChecker.exist(token)) {

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

                                        LOGGER.debug("Using distance value {}", distance);

                                        for (final String suggestion : tokenSuggestions) {

                                            final int d = distanceFunction.apply(token.toUpperCase(), suggestion.toUpperCase());

                                            if (d <= distance) {
                                                LOGGER.debug("distance for {} and {} is {}", token, suggestion, d);
                                                isMatch = true;
                                            }

                                        }

                                    }

                                }

                                if (isMatch) {

                                    // Is this term ignored?
                                    boolean isIgnored = ignored.contains(text);

                                    // There are no attributes for the span.
                                    final String replacement = getReplacement(label, context, documentId, token, Collections.emptyMap());
                                    spans.add(Span.make(offsetAttribute.startOffset(), offsetAttribute.endOffset(), getFilterType(), context, documentId, spellChecker.getAccuracy(), token, replacement, isIgnored));

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

        }

        return spans;

    }

    /**
     * Run this class to create a Lucene index from a text file.
     * Usage: java -jar ./LuceneDictionaryFilter state states.txt
     * @param args Command line arguments.
     * @throws IOException Thrown if the index cannot be created.
     */
    public static void main(String[] args) throws IOException {

        // The location of the file containing the lines to index.
        final Path filetoIndex = Paths.get("/mtnfog/code/bitbucket/philter/philter/data/index-data/hospitals-abbreviations");

        // The name of the file minus the extension is the type of index.
        final String type = FilenameUtils.removeExtension(filetoIndex.toFile().getName());

        // Make a temp directory to hold the new index.
        final Path indexDirectory = Files.createTempDirectory(type);

        LOGGER.info("Creating index of type {} from file {}.", type, filetoIndex);

        // The Lucene StandardAnalyzer uses the StandardTokenizer. It removes punctuation and stop words.
        // "Filters StandardTokenizer with StandardFilter, LowerCaseFilter and StopFilter, using a list of English stop words."
        // https://lucene.apache.org/core/8_1_1/core/org/apache/lucene/analysis/standard/StandardAnalyzer.html

        try (SpellChecker spellChecker = new SpellChecker(FSDirectory.open(indexDirectory))) {

            spellChecker.indexDictionary(new PlainTextDictionary(filetoIndex), new IndexWriterConfig(new StandardAnalyzer(EnglishAnalyzer.getDefaultStopSet())), false);

        }

        LOGGER.info("Index created at: " + indexDirectory);

    }

}
