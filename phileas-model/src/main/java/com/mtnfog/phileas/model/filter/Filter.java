package com.mtnfog.phileas.model.filter;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
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
import java.util.Optional;

public abstract class Filter implements Serializable {

    protected static final Logger LOGGER = LogManager.getLogger(Filter.class);

    /**
     * Scope of a single document.
     */
    public static final String ANONYMIZATION_SCOPE_DOCUMENT = "document";

    /**
     * Scope of a context.
     */
    public static final String ANONYMIZATION_SCOPE_CONTEXT = "context";

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
     * Filters the input text.
     * @param filterProfile The {@link FilterProfile} to use.
     * @param context The context.
     * @param documentId An ID uniquely identifying the document.
     * @param input The input text.
     * @return The filtered text.
     */
    public abstract List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException;

    /**
     * Creates a new filter with anonymization.
     *
     * @param filterType The {@link FilterType type} of the filter.
     * @param anonymizationService The {@link AnonymizationService} for this filter.
     */
    public Filter(FilterType filterType, List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService) {
        this.filterType = filterType;
        this.strategies = strategies;
        this.anonymizationService = anonymizationService;
    }

    /**
     * Gets the string to be used as a replacement.
     * @param context The context.
     * @param documentId The document ID.
     * @param token The token to replace.
     * @return The replacement string.
     */
    public String getReplacement(String name, String context, String documentId, String token, Map<String, Object> attributes) throws IOException {

        // Loop through the strategies. The first strategy without a condition or a satisfied condition will provide the replacement.
        for(AbstractFilterStrategy strategy : strategies) {

            final String condition = strategy.getCondition();

            // If there is no condition or if the condition evaluates then get the replacement.
            if(StringUtils.isEmpty(condition) || (strategy.evaluateCondition(context, documentId, token, condition, attributes))) {

                return strategy.getReplacement(name, context, documentId, token, anonymizationService);

            }

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

        LOGGER.info("Getting filter strategies for filter type {}", filterType.getType());

        if(filterType == FilterType.AGE) {
            return filterProfile.getIdentifiers().getAge().getAgeFilterStrategies();
        } else if(filterType == FilterType.CREDIT_CARD) {
            return filterProfile.getIdentifiers().getCreditCard().getCreditCardFilterStrategies();
        } else if(filterType == FilterType.CUSTOM_DICTIONARY) {
            // There can be multiple custom dictionaries in the filter profile.
            // How to know which filter strategy to retrieve from the list of custom dictionary filters?
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

}
