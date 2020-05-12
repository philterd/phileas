package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.HospitalAbbreviationFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.HospitalAbbreviationAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HospitalAbbreviationFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(HospitalAbbreviationFilterTest.class);

    private String INDEX_DIRECTORY = getIndexDirectory("hospital-abbreviations");

    @Test
    @Ignore
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

    @Before
    public void before() {
        INDEX_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEX_DIRECTORY.substring(1) : INDEX_DIRECTORY;
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filter1() throws Exception {

        final AnonymizationService anonymizationService = new HospitalAbbreviationAnonymizationService(new LocalAnonymizationCacheService());
        final List<HospitalAbbreviationFilterStrategy> strategies = Arrays.asList(new HospitalAbbreviationFilterStrategy());

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.HOSPITAL, strategies, INDEX_DIRECTORY, SensitivityLevel.HIGH, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        final List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.HIGH), "context", "documentid","Went to WMC");
        showSpans(spans);
        Assert.assertEquals(4, spans.size());

    }

}
