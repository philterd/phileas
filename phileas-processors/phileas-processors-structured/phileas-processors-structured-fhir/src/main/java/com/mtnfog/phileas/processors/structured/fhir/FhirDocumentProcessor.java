package com.mtnfog.phileas.processors.structured.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.fhir4.FhirItem;
import com.mtnfog.phileas.model.profile.fhir4.FhirR4;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.services.DocumentProcessor;
import com.mtnfog.phileas.model.services.MetricsService;
import com.mtnfog.phileas.model.services.PostFilter;
import com.mtnfog.phileas.model.services.SpanDisambiguationService;
import com.mtnfog.phileas.model.utils.Encryption;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Patient;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class FhirDocumentProcessor extends AbstractFhirDocumentProcessor implements DocumentProcessor {

    private static final Logger LOGGER = LogManager.getLogger(FhirDocumentProcessor.class);

    private MetricsService metricsService;
    private SpanDisambiguationService spanDisambiguationService;

    public FhirDocumentProcessor(MetricsService metricsService, SpanDisambiguationService spanDisambiguationService) {

        this.metricsService = metricsService;
        this.spanDisambiguationService = spanDisambiguationService;

    }

    @Override
    public FilterResponse process(FilterProfile filterProfile, List<Filter> filters, List<PostFilter> postFilters,
                                  String context, String documentId, String json) throws Exception {

        // TODO: I'm getting FhirR4 here but that version is really unknown to the API.
        // All we know is that it is an application/fhir+json document.
        // Should the version be passed in as an API header or something?

        LOGGER.debug("Doing FHIRv4 processing with filter profile [{}]", filterProfile.getName());

        final FhirR4 fhirR4 = filterProfile.getStructured().getFhirR4();
        final List<FhirItem> fhirItems = fhirR4.getFhirItems();

        final FhirContext ctx = FhirContext.forR4();
        final IParser parser = ctx.newJsonParser();
        //final Bundle bundle = parser.parseResource(Bundle.class, json);
        final Patient patient = parser.parseResource(Patient.class, json);

        // Used for value encryption. May not be needed.
        final Crypto crypto = filterProfile.getCrypto();

        // Make the changes in the Patient document.

        /*for(final Bundle.BundleEntryComponent bundleEntryComponent : bundle.getEntry()) {

            final Patient patient = (Patient) bundleEntryComponent.getResource();

            if(bundleEntryComponent.getResource() instanceof Patient) {

                processPatient(fhirItems, patient);

            }

        }*/

        final Patient processedPatient = processPatient(fhirItems, patient, crypto);

        final String serialized = parser.encodeResourceToString(processedPatient);

        return new FilterResponse(serialized, context, documentId);

    }

    private Patient processPatient(final List<FhirItem> fhirItems, final Patient patient, final Crypto crypto) throws Exception {

        final Optional<FhirItem> birthDateFhirItem = fhirItems.stream()
                .filter(p -> p.getPath().equalsIgnoreCase("patient.birthDate"))
                .findFirst();

        if(birthDateFhirItem.isPresent()) {

            if(birthDateFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE)) {
                patient.setBirthDate(new Date());
            } else if(birthDateFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_SHIFT)) {
                // TODO: Shift the date.
            }

        }

        // TODO: What to do with the narrative?
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
                    address.setText(Encryption.encrypt(address.getText(), crypto));
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
                    address.setLine(Encryption.encrypt(address.getLine(), crypto));
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
                    address.setCity(Encryption.encrypt(address.getCity(), crypto));
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
                    address.setDistrict(Encryption.encrypt(address.getDistrict(), crypto));
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

                    final String token = address.getPostalCode();
                    final int truncateDigits = 3;
                    final int truncateLength = 5;
                    final String truncated = token.substring(0, truncateDigits) + StringUtils.repeat("*", Math.min(token.length() - truncateLength, 5 - truncateDigits));
                    address.setPostalCode(truncated);

                } else if(addressFhirItem.get().getReplacementStrategy().equalsIgnoreCase(FhirItem.FHIR_ITEM_REPLACEMENT_STRATEGY_CRYPTO_REPLACE)) {
                    address.setPostalCode(Encryption.encrypt(address.getPostalCode(), crypto));
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
                    humanName.setText(Encryption.encrypt(humanName.getText(), crypto));
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
                    humanName.setFamily(Encryption.encrypt(humanName.getFamily(), crypto));
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
                    humanName.setGiven(encryptList(humanName.getGiven(), crypto));
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
                    humanName.setPrefix(encryptList(humanName.getPrefix(), crypto));
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
                    humanName.setSuffix(encryptList(humanName.getSuffix(), crypto));
                }
            }

        }

        return patient;

    }

}
