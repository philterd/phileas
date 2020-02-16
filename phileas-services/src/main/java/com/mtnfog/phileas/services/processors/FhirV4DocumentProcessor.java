package com.mtnfog.phileas.services.processors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.fhir4.FhirItem;
import com.mtnfog.phileas.model.profile.fhir4.FhirR4;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.services.DocumentProcessor;
import com.mtnfog.phileas.model.services.MetricsService;
import com.mtnfog.phileas.model.utils.Encryption;
import org.hl7.fhir.r4.model.*;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Processes and filters FHIR v4 documents.
 */
public class FhirV4DocumentProcessor implements DocumentProcessor {

    private MetricsService metricsService;

    public FhirV4DocumentProcessor(MetricsService metricsService) {

        this.metricsService = metricsService;
    }

    @Override
    public FilterResponse process(FilterProfile filterProfile, String context, String documentId, String json) throws Exception {

        // TODO: I'm getting FhirR4 here but that version is really unknown to the API.
        // All we know is that it is an application/fhir+json document.
        // Should the version be passed in as an API header or something?

        final FhirR4 fhirR4 = filterProfile.getStructured().getFhirR4();
        final List<FhirItem> fhirItems = fhirR4.getFhirItems();

        final FhirContext ctx = FhirContext.forR4();
        final IParser parser = ctx.newJsonParser();
        final Bundle bundle = parser.parseResource(Bundle.class, json);

        // Used for value encryption. May not be needed.
        final Crypto crypto = filterProfile.getCrypto();

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
                            // TODO: Truncate the zip code.
                            // address.setPostalCode();
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

}