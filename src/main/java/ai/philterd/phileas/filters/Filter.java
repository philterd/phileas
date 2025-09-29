/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Position;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.policy.IgnoredPattern;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.Identifier;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Filter {

    protected static final Logger LOGGER = LogManager.getLogger(Filter.class);

    /**
     * The {@link FilterType type} of identifiers handled by this filter.
     */
    protected final FilterType filterType;

    /**
     * The {@link AnonymizationService} to use when replacing values if enabled.
     */
    protected final AnonymizationService anonymizationService;

    /**
     * A list of filter strategies.
     */
    protected final List<? extends AbstractFilterStrategy> strategies;

    /**
     * The label is a custom value that the user can give to some types (identifiers).
     */
    protected String classification;

    /**
     * A list of ignored terms.
     */
    protected Set<String> ignored;

    /**
     * A list of ignored patterns;
     */
    protected List<IgnoredPattern> ignoredPatterns;

    /**
     * The encryption key for encrypting values.
     */
    protected final Crypto crypto;

    /**
     * The encryption details for format-preserving encryption.
     */
    protected final FPE fpe;

    /**
     * The window size for token spans.
     */
    protected int windowSize;

    /**
     * The priority of the filter.
     */
    protected int priority;

    /**
     * Filters the input text.
     * @param policy The {@link Policy} to use.
     * @param contextName The name of the context.
     * @param documentId An ID uniquely identifying the document.
     * @param piece A numbered piece of the document. Pass <code>0</code> if only piece of document.
     * @param input The input text.
     * @param attributes Attributes about the text.
     * @return A {@link FilterResult} containing the identified {@link Span spans}.
     */
    public abstract FilterResult filter(Policy policy, String contextName, String documentId, int piece, String input,
                                        final Map<String, String> attributes) throws Exception;

    /**
     * Determines if the input text may contain sensitive information matching the filter type.
     * @param policy The {@link Policy}.
     * @param input The input text.
     * @param attributes Attributes about the text.
     * @return A count of possible occurrences of the filter type in the input text.
     */
    public abstract int getOccurrences(final Policy policy, final String input, Map<String, String> attributes) throws Exception;

    /**
     * Creates a new filter.
     *
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     */
    public Filter(final FilterType filterType, final FilterConfiguration filterConfiguration) {

        this.filterType = filterType;

        this.strategies = filterConfiguration.getStrategies();
        this.anonymizationService = filterConfiguration.getAnonymizationService();
        this.ignoredPatterns = filterConfiguration.getIgnoredPatterns();
        this.ignored = filterConfiguration.getIgnored();
        this.crypto = filterConfiguration.getCrypto();
        this.fpe = filterConfiguration.getFPE();
        this.windowSize = filterConfiguration.getWindowSize();
        this.priority = filterConfiguration.getPriority();

        if(this.ignored == null) {
            this.ignored = new LinkedHashSet<>();
        }

        if(this.ignoredPatterns == null) {
            this.ignoredPatterns = new LinkedList<>();
        }

        // Add the terms from the ignored files if there are any.
        if(CollectionUtils.isNotEmpty(filterConfiguration.getIgnoredFiles())) {
            for (final String fileName : filterConfiguration.getIgnoredFiles()) {
                final File file = new File(fileName);
                if (file.exists()) {
                    try {
                        final List<String> words = FileUtils.readLines(file, Charset.defaultCharset());
                        ignored.addAll(words);
                    } catch (IOException ex) {
                        LOGGER.error("Unable to process file of ignored terms: {}", fileName, ex);
                    }
                } else {
                    LOGGER.error("Ignore list file specified in policy does not exist: {}", fileName);
                }
            }
        }

        if(CollectionUtils.isNotEmpty(this.ignored)) {
            // PHL-151: Lowercase all terms in the ignore list to not be case-sensitive.
            this.ignored = ignored.stream().map(String::toLowerCase).collect(Collectors.toSet());
        }

    }

    /**
     * Get the window of tokens surrounding a token.
     * @param text The text containing the token.
     * @return The window of surrounding tokens, including the token itself.
     */
    public String[] getWindow(final String text, int characterStart, int characterEnd) {

        if(characterStart < 0) {
            characterStart = 0;
        }

        if(characterEnd > text.length()) {
            characterEnd = text.length();
        }

        // X = windowSize
        // Start at characterStart and walk backwards until X spaces are seen.
        // Start at characterEnd and walk forward until X spaces are seen.
        // Take the string between the final start and end and tokenize it.
        // That's the window.

        int spacesSeen = 0;
        int finalStart;
        int finalEnd;

        // TODO: Make all of this null safe.

        for(finalStart = characterStart; finalStart != 0 && spacesSeen <= windowSize; finalStart--) {

            if(finalStart < text.length() && Character.isWhitespace(text.charAt(finalStart))) {

                // Count it.
                spacesSeen++;

            }

        }

        spacesSeen = 0;

        for(finalEnd = characterEnd; finalEnd != text.length() && spacesSeen <= windowSize; finalEnd++) {

            if(Character.isWhitespace(text.charAt(finalEnd))) {

                // Count it.
                spacesSeen++;

            }

        }

        final String[] tokens = text.substring(finalStart + 1, finalEnd).trim().split("\\s");

        // Remove punctuation from each token.
        // TODO: Should punctuation be preserved in the token itself?
        for(int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].replaceAll("\\p{Punct}", "");
        }

        return tokens;

    }

    /**
     * Gets the string to be used as a replacement.
     * @param policy The policy that's being applied.
     * @param contextName The context.
     * @param documentId The document ID.
     * @param token The token to replace.
     * @param window The window surrounding the token.
     * @param confidence The confidence of the item.
     * @param classification The classification of the item.
     * @return The replacement string.
     */
    public Replacement getReplacement(final Policy policy, final String contextName, final String documentId,
                                      final String token, final String[] window, double confidence,
                                      final String classification, final Map<String, String> attributes,
                                      final FilterPattern filterPattern) throws Exception {

        if(strategies != null && !strategies.isEmpty()) {

            // Loop through the strategies. The first strategy without a condition or a satisfied condition will provide the replacement.
            for (AbstractFilterStrategy strategy : strategies) {

                // Get the condition. (There might not be one.)
                final String condition = strategy.getCondition();

                // Is there a condition for this strategy?
                final boolean hasCondition = StringUtils.isNotEmpty(condition);

                if(hasCondition) {

                    // If there is a condition, does it evaluate?
                    final boolean evaluates = strategy.evaluateCondition(policy, contextName, documentId, token, window, condition, confidence, attributes);

                    if(evaluates) {

                        // Break early since we met the strategy's condition.
                        return strategy.getReplacement(classification, contextName, documentId, token, window, crypto, fpe, anonymizationService, filterPattern);

                    }

                } else {

                    // Break early since there is no condition.
                    return strategy.getReplacement(classification, contextName, documentId, token, window, crypto, fpe, anonymizationService, filterPattern);

                }

            }

        } else {

            // PHL-68: When there are no strategies just redact.
            LOGGER.warn("No filter strategies found for filter type {}. Defaulting to redaction.", filterType.getType());
            return new Replacement(AbstractFilterStrategy.DEFAULT_REDACTION.replaceAll("%t", filterType.getType()));

        }

        // This token didn't meet any condition so don't do anything with it.
        return new Replacement(token, false);

    }

    /**
     * Determines if a token is ignored.
     * @param token The token.
     * @return Returns <code>true</code> if the token is ignored; <code>false</code> otherwise.
     */
    public boolean isIgnored(final String token) {

        // Is this term ignored?
        boolean isIgnored = ignored.contains(token.toLowerCase());

        // Is this term ignored by a pattern?
        // No reason to check if it is already ignored by an ignored term.
        if(!isIgnored) {
            for (final IgnoredPattern ignoredPattern : ignoredPatterns) {
                if (token.matches(ignoredPattern.getPattern())) {
                    isIgnored = true;
                    break;
                }
            }
        }

        return isIgnored;

    }

    public static List<? extends AbstractFilterStrategy> getIdentifierFilterStrategies(Policy policy, String name) {

        final List<Identifier> identifiers = policy.getIdentifiers().getIdentifiers();

        final Identifier identifier = identifiers.stream().
                filter(p -> p.getClassification().equalsIgnoreCase(name)).
                findFirst().get();

        return identifier.getIdentifierFilterStrategies();

    }

    public Map<Position, String> splitWithIndexes(String text, String delimiter) {

        final Map<Position, String> splitsWithIndexes = new HashMap<>();

        String[] tokens = text.split(delimiter);

        int index = 0;
        for (String token : tokens) {
            splitsWithIndexes.put(new Position(index, index + token.length()), token);
            index += token.length() + delimiter.length();
        }

        return splitsWithIndexes;

    }

    public FilterType getFilterType() {
        return filterType;
    }

    public String getClassification() {
        return classification;
    }

    public Crypto getCrypto() {
        return crypto;
    }

    public List<IgnoredPattern> getIgnoredPatterns() {
        return ignoredPatterns;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public int getPriority() {
        return priority;

    }

}