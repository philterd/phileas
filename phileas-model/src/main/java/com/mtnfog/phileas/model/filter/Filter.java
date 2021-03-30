package com.mtnfog.phileas.model.filter;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.IgnoredPattern;
import com.mtnfog.phileas.model.profile.filters.Identifier;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Filter {

    protected static final Logger LOGGER = LogManager.getLogger(Filter.class);

    /**
     * The {@link FilterType type} of identifiers handled by this filter.
     */
    protected final FilterType filterType;

    /**
     * The alert service.
     */
    protected final AlertService alertService;

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
     * The window size for token spans.
     */
    protected int windowSize;

    /**
     * Filters the input text.
     * @param filterProfile The {@link FilterProfile} to use.
     * @param context The context.
     * @param documentId An ID uniquely identifying the document.
     * @param piece A numbered piece of the document. Pass <code>0</code> if only piece of document.
     * @param input The input text.
     * @return A {@link FilterResult} containing the identified {@link Span spans}.
     */
    public abstract FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception;

    /**
     * Determines if the input text may contain sensitive information matching the filter type.
     * @param filterProfile The {@link FilterProfile}.
     * @param input The input text.
     * @return A count of possible occurrences of the filter type in the input text.
     */
    public abstract int getOccurrences(FilterProfile filterProfile, String input) throws Exception;

    /**
     * Creates a new filter.
     *
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     */
    public Filter(FilterType filterType, FilterConfiguration filterConfiguration) {

        this.filterType = filterType;
        this.strategies = filterConfiguration.getStrategies();
        this.anonymizationService = filterConfiguration.getAnonymizationService();
        this.alertService = filterConfiguration.getAlertService();
        this.ignoredPatterns = filterConfiguration.getIgnoredPatterns();
        this.ignored = filterConfiguration.getIgnored();
        this.crypto = filterConfiguration.getCrypto();
        this.windowSize = filterConfiguration.getWindowSize();

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
                    LOGGER.error("Ignore list file specified in filter profile does not exist: {}", fileName);
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
    public String[] getWindow(String text, int characterStart, int characterEnd) {

        LOGGER.trace("Getting window of size {}", windowSize);

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
     * @param filterProfile The name of the filter profile.
     * @param context The context.
     * @param documentId The document ID.
     * @param token The token to replace.
     * @param window The window surrounding the token.
     * @param confidence The confidence of the item.
     * @param classification The classification of the item.
     * @return The replacement string.
     */
    public Replacement getReplacement(String filterProfile, String context, String documentId, String token, String[] window, double confidence, String classification, FilterPattern filterPattern) throws Exception {

        if(strategies != null) {

            // Loop through the strategies. The first strategy without a condition or a satisfied condition will provide the replacement.
            for (AbstractFilterStrategy strategy : strategies) {

                // Get the condition. (There might not be one.)
                final String condition = strategy.getCondition();

                // Is there a condition for this strategy?
                final boolean hasCondition = StringUtils.isNotEmpty(condition);

                if(hasCondition) {

                    // If there is a condition, does it evaluate?
                    final boolean evaluates = strategy.evaluateCondition(context, documentId, token, window, condition, confidence, classification);

                    if(evaluates) {

                        // Generate an alert for this strategy?
                        if(strategy.isAlert()) {

                            LOGGER.info("Generating alert for strategy ID {}", strategy.getId());
                            alertService.generateAlert(filterProfile, strategy.getId(), documentId, context, filterType);

                        }

                        // Break early since we met the strategy's condition.
                        return strategy.getReplacement(classification, context, documentId, token, window, crypto, anonymizationService, filterPattern);

                    }

                } else {

                    // Break early since there is no condition.
                    return strategy.getReplacement(classification, context, documentId, token, window, crypto, anonymizationService, filterPattern);

                }

            }

        } else {

            // PHL-68: When there are no strategies just redact.
            LOGGER.warn("No filter strategies found for filter type {}. Defaulting to redaction.", filterType.getType());
            return new Replacement(AbstractFilterStrategy.DEFAULT_REDACTION.replaceAll("%t", filterType.getType()));

        }

        // No conditions matched so there is no replacement. Just return the original token.
        return new Replacement(token);

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

    public static List<? extends AbstractFilterStrategy> getIdentifierFilterStrategies(FilterProfile filterProfile, String name) {

        final List<Identifier> identifiers = filterProfile.getIdentifiers().getIdentifiers();

        final Identifier identifier = identifiers.stream().
                filter(p -> p.getClassification().equalsIgnoreCase(name)).
                findFirst().get();

        return identifier.getIdentifierFilterStrategies();

    }

    public static List<? extends AbstractFilterStrategy> getFilterStrategies(FilterProfile filterProfile, FilterType filterType, int index) {

        LOGGER.debug("Getting filter strategies for filter type {}", filterType.getType());

        if(filterType == FilterType.AGE) {
            return filterProfile.getIdentifiers().getAge().getAgeFilterStrategies();
        } else if(filterType == FilterType.BITCOIN_ADDRESS) {
            return filterProfile.getIdentifiers().getBitcoinAddress().getBitcoinFilterStrategies();
        } else if(filterType == FilterType.CREDIT_CARD) {
            return filterProfile.getIdentifiers().getCreditCard().getCreditCardFilterStrategies();
        } else if(filterType == FilterType.CUSTOM_DICTIONARY) {
            // There can be multiple custom dictionaries in the filter profile.
            // The index is used to determine which one is the appropriate one.
            return filterProfile.getIdentifiers().getCustomDictionaries().get(index).getCustomDictionaryFilterStrategies();
        } else if(filterType == FilterType.DATE) {
            return filterProfile.getIdentifiers().getDate().getDateFilterStrategies();
        } else if(filterType == FilterType.DRIVERS_LICENSE_NUMBER) {
            return filterProfile.getIdentifiers().getDriversLicense().getDriversLicenseFilterStrategies();
        } else if(filterType == FilterType.EMAIL_ADDRESS) {
            return filterProfile.getIdentifiers().getEmailAddress().getEmailAddressFilterStrategies();
        } else if(filterType == FilterType.IBAN_CODE) {
            return filterProfile.getIdentifiers().getIbanCode().getIbanCodeFilterStrategies();
        } else if(filterType == FilterType.IP_ADDRESS) {
            return filterProfile.getIdentifiers().getIpAddress().getIpAddressFilterStrategies();
        } else if(filterType == FilterType.NER_ENTITY) {
            return filterProfile.getIdentifiers().getNer().getNerStrategies();
        } else if(filterType == FilterType.PASSPORT_NUMBER) {
            return filterProfile.getIdentifiers().getPassportNumber().getPassportNumberFilterStrategies();
        } else if(filterType == FilterType.PHONE_NUMBER) {
            return filterProfile.getIdentifiers().getPhoneNumber().getPhoneNumberFilterStrategies();
        } else if(filterType == FilterType.PHONE_NUMBER_EXTENSION) {
            return filterProfile.getIdentifiers().getPhoneNumberExtension().getPhoneNumberExtensionFilterStrategies();
        } else if(filterType == FilterType.PHYSICIAN_NAME) {
            return filterProfile.getIdentifiers().getPhysicianName().getPhysicianNameFilterStrategies();
        } else if(filterType == FilterType.SSN) {
            return filterProfile.getIdentifiers().getSsn().getSsnFilterStrategies();
        } else if(filterType == FilterType.STATE_ABBREVIATION) {
            return filterProfile.getIdentifiers().getStateAbbreviation().getStateAbbreviationsFilterStrategies();
        } else if(filterType == FilterType.STREET_ADDRESS) {
            return filterProfile.getIdentifiers().getStateAbbreviation().getStateAbbreviationsFilterStrategies();
        } else if(filterType == FilterType.URL) {
            return filterProfile.getIdentifiers().getUrl().getUrlFilterStrategies();
        } else if(filterType == FilterType.VIN) {
            return filterProfile.getIdentifiers().getVin().getVinFilterStrategies();
        } else if(filterType == FilterType.ZIP_CODE) {
            return filterProfile.getIdentifiers().getZipCode().getZipCodeFilterStrategies();
        } else if(filterType == FilterType.LOCATION_CITY) {
            return filterProfile.getIdentifiers().getCity().getCityFilterStrategies();
        }  else if(filterType == FilterType.LOCATION_COUNTY) {
            return filterProfile.getIdentifiers().getCounty().getCountyFilterStrategies();
        } else if(filterType == FilterType.FIRST_NAME) {
            return filterProfile.getIdentifiers().getFirstName().getFirstNameFilterStrategies();
        } else if(filterType == FilterType.HOSPITAL_ABBREVIATION) {
            return filterProfile.getIdentifiers().getHospitalAbbreviation().getHospitalAbbreviationFilterStrategies();
        } else if(filterType == FilterType.HOSPITAL) {
            return filterProfile.getIdentifiers().getHospital().getHospitalFilterStrategies();
        } else if(filterType == FilterType.LOCATION_STATE) {
            return filterProfile.getIdentifiers().getState().getStateFilterStrategies();
        } else if(filterType == FilterType.SURNAME) {
            return filterProfile.getIdentifiers().getSurname().getSurnameFilterStrategies();
        }

        // Should never happen.
        return null;

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

}