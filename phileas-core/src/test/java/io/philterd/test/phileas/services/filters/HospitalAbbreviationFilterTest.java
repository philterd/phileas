package io.philterd.test.phileas.services.filters;

import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.enums.SensitivityLevel;
import io.philterd.phileas.model.filter.FilterConfiguration;
import io.philterd.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import io.philterd.phileas.model.objects.FilterResult;
import io.philterd.phileas.model.profile.filters.strategies.dynamic.HospitalAbbreviationFilterStrategy;
import io.philterd.phileas.model.services.AlertService;
import io.philterd.phileas.services.anonymization.HospitalAbbreviationAnonymizationService;
import io.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public class HospitalAbbreviationFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(HospitalAbbreviationFilterTest.class);

    private String INDEX_DIRECTORY = getIndexDirectory("hospital-abbreviations");

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    @Disabled
    public void makeAbbreviations() throws Exception {

        final String file = "/mtnfog/code/bitbucket/philter/philter/index-data/hospitals";
        final InputStream is = new FileInputStream(file);
        final List<String> lines = IOUtils.readLines(is, Charset.defaultCharset());

        for(String line : lines) {

            final String abbreviated = WordUtils.initials(line, null);

            System.out.println(abbreviated);

        }

        is.close();

    }

    @BeforeEach
    public void before() {
        INDEX_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEX_DIRECTORY.substring(1) : INDEX_DIRECTORY;
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new HospitalAbbreviationFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new HospitalAbbreviationAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.HOSPITAL_ABBREVIATION, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.HIGH, false);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.HIGH), "context", "documentid", PIECE, "Went to WMC");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(4, filterResult.getSpans().size());

    }

}
