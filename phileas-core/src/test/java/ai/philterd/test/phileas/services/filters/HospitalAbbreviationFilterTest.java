/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.test.phileas.services.filters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.HospitalAbbreviationFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.HospitalAbbreviationAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
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

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Went to WMC");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(4, filterResult.getSpans().size());

    }

}
