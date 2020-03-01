package com.mtnfog.phileas.model.filter;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.Identifier;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Filter implements Serializable {

    protected static final Logger LOGGER = LogManager.getLogger(Filter.class);

    /**
     * The {@link FilterType type} of identifiers handled by this filter.
     */
    protected FilterType filterType;

    /**
     * The {@link AnonymizationService} to use when replacing values if enabled.
     */
    protected AnonymizationService anonymizationService;

    protected List<? extends AbstractFilterStrategy> strategies;

    /**
     * The label is a custom value that the user can give to some types (identifiers).
     */
    protected String label;

    /**
     * A list of ignored terms.
     */
    protected Set<String> ignored;

    /**
     * The encryption key for encrypting values.
     */
    protected Crypto crypto;

    /**
     * Filters the input text.
     * @param filterProfile The {@link FilterProfile} to use.
     * @param context The context.
     * @param documentId An ID uniquely identifying the document.
     * @param input The input text.
     * @return The filtered text.
     */
    public abstract List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException, Exception;

    /**
     * Creates a new filter with anonymization.
     *
     * @param filterType The {@link FilterType type} of the filter.
     * @param anonymizationService The {@link AnonymizationService} for this filter.
     */
    public Filter(FilterType filterType, List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, Set<String> ignored, Crypto crypto) {
        this.filterType = filterType;
        this.strategies = strategies;
        this.anonymizationService = anonymizationService;
        this.ignored = ignored;
        this.crypto = crypto;
    }

    /**
     * Get the window of tokens surrounding a token.
     * @param text The text containing the token.
     * @param token The token.
     * @return The window of surrounding tokens.
     */
    public String[] getWindow(String text, String token, int characterStart, int characterEnd) {

        // TODO: Make this a setting / environment variable / something external.
        final int windowSize = 3;

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

            if(Character.isWhitespace(text.charAt(finalStart))) {

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

        final String tokens = text.substring(finalStart + 1, finalEnd).trim();

        return tokens.split(" ");

    }

    /**
     * Gets the string to be used as a replacement.
     * @param context The context.
     * @param documentId The document ID.
     * @param token The token to replace.
     * @return The replacement string.
     */
    public String getReplacement(String name, String context, String documentId, String token, Map<String, Object> attributes) throws Exception {

        if(strategies != null) {

            // Loop through the strategies. The first strategy without a condition or a satisfied condition will provide the replacement.
            for (AbstractFilterStrategy strategy : strategies) {

                final String condition = strategy.getCondition();

                // If there is no condition or if the condition evaluates then get the replacement.
                if (StringUtils.isEmpty(condition) || (strategy.evaluateCondition(context, documentId, token, condition, attributes))) {

                    return strategy.getReplacement(name, context, documentId, token, crypto, anonymizationService);

                }

            }

        } else {

            // PHL-68: When there are no strategies just redact.
            LOGGER.warn("No filter strategies found for filter type {}. Defaulting to redaction.", filterType.getType());
            return AbstractFilterStrategy.DEFAULT_REDACTION.replaceAll("%t", filterType.getType());

        }

        // No conditions matched so there is no replacement. Just return the original token.
        return token;

    }

    public static List<? extends AbstractFilterStrategy> getIdentifierFilterStrategies(FilterProfile filterProfile, String name) {

        final List<Identifier> identifiers = filterProfile.getIdentifiers().getIdentifiers();

        final Identifier identifier = identifiers.stream().
                filter(p -> p.getLabel().equalsIgnoreCase(name)).
                findFirst().get();

        return identifier.getIdentifierFilterStrategies();

    }

    public static List<? extends AbstractFilterStrategy> getFilterStrategies(FilterProfile filterProfile, FilterType filterType, int index) {

        LOGGER.debug("Getting filter strategies for filter type {}", filterType.getType());

        if(filterType == FilterType.AGE) {
            return filterProfile.getIdentifiers().getAge().getAgeFilterStrategies();
        } else if(filterType == FilterType.CREDIT_CARD) {
            return filterProfile.getIdentifiers().getCreditCard().getCreditCardFilterStrategies();
        } else if(filterType == FilterType.CUSTOM_DICTIONARY) {
            // There can be multiple custom dictionaries in the filter profile.
            // The index is used to determine which one is the appropriate one.
            return filterProfile.getIdentifiers().getCustomDictionaries().get(index).getCustomDictionaryFilterStrategies();
        } else if(filterType == FilterType.DATE) {
            return filterProfile.getIdentifiers().getDate().getDateFilterStrategies();
        } else if(filterType == FilterType.EMAIL_ADDRESS) {
            return filterProfile.getIdentifiers().getEmailAddress().getEmailAddressFilterStrategies();
        } else if(filterType == FilterType.IP_ADDRESS) {
            return filterProfile.getIdentifiers().getIpAddress().getIpAddressFilterStrategies();
        } else if(filterType == FilterType.NER_ENTITY) {
            return filterProfile.getIdentifiers().getNer().getNerStrategies();
        } else if(filterType == FilterType.PHONE_NUMBER) {
            return filterProfile.getIdentifiers().getPhoneNumber().getPhoneNumberFilterStrategies();
        } else if(filterType == FilterType.PHONE_NUMBER_EXTENSION) {
            return filterProfile.getIdentifiers().getPhoneNumberExtension().getPhoneNumberExtensionFilterStrategies();
        } else if(filterType == FilterType.SSN) {
            return filterProfile.getIdentifiers().getSsn().getSsnFilterStrategies();
        } else if(filterType == FilterType.STATE_ABBREVIATION) {
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

    public String getLabel() {
        return label;
    }

    public Crypto getCrypto() {
        return crypto;
    }

}
