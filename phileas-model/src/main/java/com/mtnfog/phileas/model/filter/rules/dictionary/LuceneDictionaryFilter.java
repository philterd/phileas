package com.mtnfog.phileas.model.filter.rules.dictionary;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
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
import java.util.*;

/**
 * A filter that operates on a Lucene index.
 */
public class LuceneDictionaryFilter extends DictionaryFilter implements Serializable, Closeable {

    private static final Logger LOGGER = LogManager.getLogger(LuceneDictionaryFilter.class);

    private SpellChecker spellChecker;
    private LevenshteinDistance distance;

    private Map<SensitivityLevel, Integer> distances;

    public static final Map<SensitivityLevel, Integer> FIRST_NAME_DISTANCES = new HashMap<SensitivityLevel, Integer>() {{
        FIRST_NAME_DISTANCES.put(SensitivityLevel.LOW, 0);
        FIRST_NAME_DISTANCES.put(SensitivityLevel.MEDIUM, 1);
        FIRST_NAME_DISTANCES.put(SensitivityLevel.HIGH, 2);
    }};

    public static final Map<SensitivityLevel, Integer> SURNAME_DISTANCES = new HashMap<SensitivityLevel, Integer>() {{

        SURNAME_DISTANCES.put(SensitivityLevel.LOW, 0);
        SURNAME_DISTANCES.put(SensitivityLevel.MEDIUM, 1);
        SURNAME_DISTANCES.put(SensitivityLevel.HIGH, 2);

    }};

    public static final Map<SensitivityLevel, Integer> CITIES_DISTANCES = new HashMap<SensitivityLevel, Integer>() {{

        CITIES_DISTANCES.put(SensitivityLevel.LOW, 0);
        CITIES_DISTANCES.put(SensitivityLevel.MEDIUM, 1);
        CITIES_DISTANCES.put(SensitivityLevel.HIGH, 2);

    }};

    public static final Map<SensitivityLevel, Integer> STATES_DISTANCES = new HashMap<SensitivityLevel, Integer>() {{

        STATES_DISTANCES.put(SensitivityLevel.LOW, 0);
        STATES_DISTANCES.put(SensitivityLevel.MEDIUM, 1);
        STATES_DISTANCES.put(SensitivityLevel.HIGH, 2);

    }};

    public static final Map<SensitivityLevel, Integer> COUNTIES_DISTANCES = new HashMap<SensitivityLevel, Integer>() {{

        COUNTIES_DISTANCES.put(SensitivityLevel.LOW, 0);
        COUNTIES_DISTANCES.put(SensitivityLevel.MEDIUM, 1);
        COUNTIES_DISTANCES.put(SensitivityLevel.HIGH, 2);

    }};

    public static final Map<SensitivityLevel, Integer> HOSPITALS_DISTANCES = new HashMap<SensitivityLevel, Integer>() {{

        HOSPITALS_DISTANCES.put(SensitivityLevel.LOW, 0);
        HOSPITALS_DISTANCES.put(SensitivityLevel.MEDIUM, 1);
        HOSPITALS_DISTANCES.put(SensitivityLevel.HIGH, 2);

    }};

    public static final Map<SensitivityLevel, Integer> HOSPITAL_ABBREVIATIONS_DISTANCES = new HashMap<SensitivityLevel, Integer>() {{

        HOSPITAL_ABBREVIATIONS_DISTANCES.put(SensitivityLevel.LOW, 0);
        HOSPITAL_ABBREVIATIONS_DISTANCES.put(SensitivityLevel.MEDIUM, 1);
        HOSPITAL_ABBREVIATIONS_DISTANCES.put(SensitivityLevel.HIGH, 2);

    }};

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

    /**
     * Creates a new Lucene dictionary filter.
     * @param filterType The {@link FilterType type} of filter.
     * @param indexDirectory The path to the index on disk.
     * @param distances A map of string edit distances for each {@link SensitivityLevel}.
     * @param anonymizationService The {@link AnonymizationService} for this filter.
     * @throws IOException Thrown if the index cannot be opened or accessed.
     */
    public LuceneDictionaryFilter(FilterType filterType,
                                  List<? extends AbstractFilterStrategy> strategies,
                                  String indexDirectory,
                                  Map<SensitivityLevel, Integer> distances,
                                  AnonymizationService anonymizationService) throws IOException {

        super(filterType, strategies, anonymizationService);

        LOGGER.info("Loading {} index from {}", filterType, indexDirectory);

        this.distance = new LevenshteinDistance();
        this.distances = distances;

        if(distances == null) {
            distances = new HashMap<>();
        }

        // Default string edit distances.
        if(distances.isEmpty()) {
            distances.put(SensitivityLevel.LOW, 1);
            distances.put(SensitivityLevel.MEDIUM, 3);
            distances.put(SensitivityLevel.HIGH, 5);
        }

        // Load the index for fuzzy search.
        this.spellChecker = new SpellChecker(FSDirectory.open(Paths.get(indexDirectory), NoLockFactory.INSTANCE));
        this.spellChecker.setStringDistance(new LuceneLevenshteinDistance());
        this.spellChecker.setAccuracy(0.0f);

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

                for(AbstractFilterStrategy strategy :Filter.getFilterStrategies(filterProfile,filterType)) {

                    final SensitivityLevel sensitivityLevel = SensitivityLevel.fromName(strategy.getSensitivityLevel());

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

                                        for (String suggestion : tokenSuggestions) {

                                            int d = distance.apply(token.toUpperCase(), suggestion.toUpperCase());
                                            LOGGER.info("distance for {} and {} is {}", token, suggestion, d);

                                            if (d <= distances.get(sensitivityLevel)) {
                                                isMatch = true;
                                            }

                                        }

                                    }

                                }

                                if (isMatch) {

                                    // There are no attributes for the span.
                                    final String replacement = getReplacement(context, documentId, token, Collections.emptyMap());
                                    spans.add(Span.make(offsetAttribute.startOffset(), offsetAttribute.endOffset(), getFilterType(), context, documentId, spellChecker.getAccuracy(), replacement));

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

}
