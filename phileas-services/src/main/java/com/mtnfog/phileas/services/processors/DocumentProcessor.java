package com.mtnfog.phileas.services.processors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.Explanation;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.fhir4.FhirItem;
import com.mtnfog.phileas.model.profile.fhir4.FhirR4;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.services.MetricsService;
import com.mtnfog.phileas.model.services.PostFilter;
import com.mtnfog.phileas.model.services.Store;
import org.hl7.fhir.r4.model.*;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Contains the functions to process and filter the input documents.
 */
public class DocumentProcessor {

    private Map<String, List<Filter>> filters;
    private List<PostFilter> postFilters;
    private MetricsService metricsService;
    private Store store;

    public DocumentProcessor(Map<String, List<Filter>> filters, List<PostFilter> postFilters, MetricsService metricsService, Store store) {

        this.filters = filters;
        this.postFilters = postFilters;
        this.metricsService = metricsService;
        this.store = store;

    }

    public FilterResponse processApplicationFhirJson(FilterProfile filterProfile, String context, String documentId, String json) {

        // TODO: I'm getting FhirR4 here but that version is really unknown to the API.
        // All we know is that it is an application/fhir+json document.
        // Should the version be passed in as an API header or something?

        final FhirR4 fhirR4 = filterProfile.getStructured().getFhirR4();
        final List<FhirItem> fhirItems = fhirR4.getFhirItems();

        final FhirContext ctx = FhirContext.forR4();
        final IParser parser = ctx.newJsonParser();
        final Bundle bundle = parser.parseResource(Bundle.class, json);

        // Make the changes in the document.

        for(final Bundle.BundleEntryComponent bundleEntryComponent : bundle.getEntry()) {

            if(bundleEntryComponent.getResource() instanceof Patient) {

                final Patient patient = (Patient) bundleEntryComponent.getResource();

                final Optional<FhirItem> birthDateFhirItem = fhirItems.stream()
                        .filter(p -> p.getPath().equalsIgnoreCase("patient.birthDate"))
                        .findFirst();

                if(birthDateFhirItem.isPresent()) {

                    if(birthDateFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE)) {
                        patient.setBirthDate(new Date());
                    } else if(birthDateFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_SHIFT)) {
                        // TODO: Shift the date.
                    } else if(birthDateFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_CRYPTO_REPLACE)) {
                        // TODO: Encrypt the value.
                    }

                }

                final Narrative narrative = patient.getText();
                // text.div
                //narrative.setDiv();

                // Address
                for(Address address : patient.getAddress()) {

                    // address.text
                    final Optional<FhirItem> addressFhirItem = fhirItems.stream()
                            .filter(p -> p.getPath().equalsIgnoreCase("patient.address.text"))
                            .findFirst();

                    if(addressFhirItem.isPresent()) {
                        if(addressFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE)) {
                            address.setText("");
                        } else if(addressFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_CRYPTO_REPLACE)) {
                            // TODO: Encrypt the value.
                        }
                    }

                    // address.line
                    final Optional<FhirItem> addressLineFhirItem = fhirItems.stream()
                            .filter(p -> p.getPath().equalsIgnoreCase("patient.address.line"))
                            .findFirst();

                    if(addressLineFhirItem.isPresent()) {
                        if(addressLineFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE)) {
                            address.setLine(Collections.emptyList());
                        } else if(addressFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_CRYPTO_REPLACE)) {
                            // TODO: Encrypt the value.
                        }
                    }

                    // address.city
                    final Optional<FhirItem> addressCityFhirItem = fhirItems.stream()
                            .filter(p -> p.getPath().equalsIgnoreCase("patient.address.city"))
                            .findFirst();

                    if(addressCityFhirItem.isPresent()) {
                        if(addressCityFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE)) {
                            address.setCity("");
                        } else if(addressFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_CRYPTO_REPLACE)) {
                            // TODO: Encrypt the value.
                        }
                    }

                    // address.district
                    final Optional<FhirItem> addressDistrictFhirItem = fhirItems.stream()
                            .filter(p -> p.getPath().equalsIgnoreCase("patient.address.district"))
                            .findFirst();

                    if(addressDistrictFhirItem.isPresent()) {
                        if(addressDistrictFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE)) {
                            address.setDistrict("");
                        } else if(addressFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_CRYPTO_REPLACE)) {
                            // TODO: Encrypt the value.
                        }
                    }

                    // address.postalCode
                    final Optional<FhirItem> addressPostalCodeFhirItem = fhirItems.stream()
                            .filter(p -> p.getPath().equalsIgnoreCase("patient.address.postalcode"))
                            .findFirst();

                    if(addressPostalCodeFhirItem.isPresent()) {
                        if(addressPostalCodeFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE)) {
                            address.setPostalCode("");
                        }
                        if(addressPostalCodeFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_TRUNCATE)) {
                            // TODO: Truncate the zip code.
                            // address.setPostalCode();
                        } else if(addressFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_CRYPTO_REPLACE)) {
                            // TODO: Encrypt the value.
                        }
                    }

                }

                // Human Name
                for(HumanName humanName : patient.getName()) {

                    // name.text
                    final Optional<FhirItem> humanNameFhirItem = fhirItems.stream()
                            .filter(p -> p.getPath().equalsIgnoreCase("patient.name.text"))
                            .findFirst();

                    if(humanNameFhirItem.isPresent()) {
                        if(humanNameFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE)) {
                            humanName.setText("");
                        } else if(humanNameFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_CRYPTO_REPLACE)) {
                            // TODO: Encrypt the value.
                        }
                    }

                    // name.family
                    final Optional<FhirItem> humanNameFamilyFhirItem = fhirItems.stream()
                            .filter(p -> p.getPath().equalsIgnoreCase("patient.name.family"))
                            .findFirst();

                    if(humanNameFamilyFhirItem.isPresent()) {
                        if(humanNameFamilyFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE)) {
                            humanName.setFamily("");
                        } else if(humanNameFamilyFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_CRYPTO_REPLACE)) {
                            // TODO: Encrypt the value.
                        }
                    }

                    // name.given
                    final Optional<FhirItem> humanNameGivenFhirItem = fhirItems.stream()
                            .filter(p -> p.getPath().equalsIgnoreCase("patient.name.given"))
                            .findFirst();

                    if(humanNameGivenFhirItem.isPresent()) {
                        if(humanNameGivenFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE)) {
                            humanName.setGiven(Collections.emptyList());
                        } else if(humanNameGivenFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_CRYPTO_REPLACE)) {
                            // TODO: Encrypt the value.
                        }
                    }

                    // name.prefix
                    final Optional<FhirItem> humanNamePrefixFhirItem = fhirItems.stream()
                            .filter(p -> p.getPath().equalsIgnoreCase("patient.name.prefix"))
                            .findFirst();

                    if(humanNamePrefixFhirItem.isPresent()) {
                        if(humanNamePrefixFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE)) {
                            humanName.setPrefix(Collections.emptyList());
                        } else if(humanNamePrefixFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_CRYPTO_REPLACE)) {
                            // TODO: Encrypt the value.
                        }
                    }

                    // name.suffix
                    final Optional<FhirItem> humanNameSuffixFhirItem = fhirItems.stream()
                            .filter(p -> p.getPath().equalsIgnoreCase("patient.name.suffix"))
                            .findFirst();

                    if(humanNameSuffixFhirItem.isPresent()) {
                        if(humanNameSuffixFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE)) {
                            humanName.setSuffix(Collections.emptyList());
                        } else if(humanNameSuffixFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_CRYPTO_REPLACE)) {
                            // TODO: Encrypt the value.
                        }
                    }

                }

            }

        }

        final String serialized = parser.encodeResourceToString(bundle);

        return new FilterResponse(serialized, context, documentId);

    }

    public FilterResponse processTextPlain(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        // The list that will contain the spans containing PHI/PII.
        List<Span> spans = new LinkedList<>();

        // Get the enabled filters for this filter profile.
        final List<Filter> allFiltersFromProfile = filters.get(filterProfile.getName());

        // Execute each filter.
        for(final Filter filter : allFiltersFromProfile) {
            spans.addAll(filter.filter(filterProfile, context, documentId, input));
        }

        // Drop overlapping spans.
        spans = Span.dropOverlappingSpans(spans);

        // Drop ignored spans.
        spans = Span.dropIgnoredSpans(spans);

        // Sort the spans based on the confidence.
        spans.sort(Comparator.comparing(Span::getConfidence));

        // Perform post-filtering on the spans.
        for(final PostFilter postFilter : postFilters) {
            spans = postFilter.filter(input, spans);
        }

        // The spans that will be persisted. Has to be a deep copy because the shift
        // below will change the indexes. Doing this to save the original locations of the spans.
        final List<Span> appliedSpans = spans.stream().map(d -> d.copy()).collect(toList());

        // TODO: Set a flag on each "span" not in appliedSpans indicating it was not used.

        // Log a metric for each filter type.
        appliedSpans.forEach(k -> metricsService.incrementFilterType(k.getFilterType()));

        // Define the explanation.
        final Explanation explanation = new Explanation(appliedSpans, spans);

        // Used to manipulate the text.
        final StringBuffer buffer = new StringBuffer(input);

        // Initialize this to the the input length but it may grow in length if redactions/replacements
        // are longer than the original spans.
        int stringLength = input.length();

        // Go character by character through the input.
        for(int i = 0; i < stringLength; i++) {

            // Is index i the start of a span?
            final Span span = Span.doesIndexStartSpan(i, spans);

            if(span != null) {

                // Get the replacement. This might be the token itself or an anonymized version.
                final String replacement = span.getReplacement();

                final int spanLength = span.getCharacterEnd() - span.getCharacterStart();
                final int replacementLength = replacement.length();

                if(spanLength != replacementLength) {

                    // We need to adjust the characterStart and characterEnd for the remaining spans.
                    // A negative value means shift left.
                    // A positive value means shift right.
                    final int shift = (spanLength - replacementLength) * -1;

                    // Shift the remaining spans by the shift value.
                    spans = Span.shiftSpans(shift, span, spans);

                    // Update the length of the string.
                    stringLength += shift;

                }

                // We can now do the replacement.
                buffer.replace(span.getCharacterStart(), span.getCharacterEnd(), replacement);

                // Jump ahead outside of this span.
                i = span.getCharacterEnd();

            }

        }

        metricsService.incrementProcessed();

        // Store the applied spans in the database.
        if(store != null) {
            store.insert(appliedSpans);
        }

        return new FilterResponse(buffer.toString(), context, documentId, explanation);

    }

}
