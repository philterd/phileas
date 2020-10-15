package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.HospitalAbbreviationFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.HospitalAbbreviationAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
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
import java.util.Collections;
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

        final AnonymizationService anonymizationService = new HospitalAbbreviationAnonymizationService(new LocalAnonymizationCacheService());
        final List<HospitalAbbreviationFilterStrategy> strategies = Arrays.asList(new HospitalAbbreviationFilterStrategy());

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.HOSPITAL, strategies, INDEX_DIRECTORY, SensitivityLevel.HIGH, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.HIGH), "context", "documentid", 0, "Went to WMC");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(4, filterResult.getSpans().size());

    }

}
