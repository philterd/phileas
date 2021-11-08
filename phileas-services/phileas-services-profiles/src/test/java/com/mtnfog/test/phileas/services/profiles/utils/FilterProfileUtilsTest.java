package com.mtnfog.test.phileas.services.profiles.utils;

import com.google.gson.Gson;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.services.FilterProfileService;
import com.mtnfog.phileas.services.profiles.utils.FilterProfileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.mockito.Mockito.when;

public class FilterProfileUtilsTest {

    @Disabled
    @Test
    public void onlyOne() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile1.json"), Charset.defaultCharset());

        final FilterProfileService filterProfileService = Mockito.mock(FilterProfileService.class);
        when(filterProfileService.get("profile1")).thenReturn(json1);

        final Gson gson = new Gson();
        final FilterProfileUtils filterProfileUtils = new FilterProfileUtils(filterProfileService, gson);
        final FilterProfile filterProfile = filterProfileUtils.getCombinedFilterProfiles(Arrays.asList("profile1"));

        final FilterProfile originalFilterProfile = gson.fromJson(json1, FilterProfile.class);

        // TODO: This needs a deep comparison.
        Assertions.assertTrue(originalFilterProfile.equals(filterProfile));

        Assertions.assertNotNull(filterProfile);
        Assertions.assertFalse(StringUtils.equals(filterProfile.getName(), "combined"));
        Assertions.assertTrue(filterProfile.getIdentifiers().hasFilter(FilterType.AGE));
        Assertions.assertFalse(filterProfile.getIdentifiers().hasFilter(FilterType.CREDIT_CARD));
        Assertions.assertFalse(filterProfile.getIdentifiers().hasFilter(FilterType.URL));

    }

    @Test
    public void combineAgeAndCreditCard() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile1.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile2.json"), Charset.defaultCharset());

        final FilterProfileService filterProfileService = Mockito.mock(FilterProfileService.class);
        when(filterProfileService.get("profile1")).thenReturn(json1);
        when(filterProfileService.get("profile2")).thenReturn(json2);

        final Gson gson = new Gson();
        final FilterProfileUtils filterProfileUtils = new FilterProfileUtils(filterProfileService, gson);
        final FilterProfile filterProfile = filterProfileUtils.getCombinedFilterProfiles(Arrays.asList("profile1", "profile2"));

        Assertions.assertNotNull(filterProfile);
        Assertions.assertTrue(StringUtils.equals(filterProfile.getName(), "combined"));
        Assertions.assertTrue(filterProfile.getIdentifiers().hasFilter(FilterType.AGE));
        Assertions.assertTrue(filterProfile.getIdentifiers().hasFilter(FilterType.CREDIT_CARD));
        Assertions.assertFalse(filterProfile.getIdentifiers().hasFilter(FilterType.URL));

    }

    @Test
    public void combineDuplicateFilter() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile1.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile1.json"), Charset.defaultCharset());

        final FilterProfileService filterProfileService = Mockito.mock(FilterProfileService.class);
        when(filterProfileService.get("profile1")).thenReturn(json1);
        when(filterProfileService.get("profile2")).thenReturn(json2);

        final Gson gson = new Gson();
        final FilterProfileUtils filterProfileUtils = new FilterProfileUtils(filterProfileService, gson);

        Assertions.assertThrows(IllegalStateException.class, () -> {
            final FilterProfile filterProfile = filterProfileUtils.getCombinedFilterProfiles(Arrays.asList("profile1", "profile2"));
        });

    }

    @Test
    public void combineCustomDictionaryAndZipCode() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile3.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile4.json"), Charset.defaultCharset());

        final FilterProfileService filterProfileService = Mockito.mock(FilterProfileService.class);
        when(filterProfileService.get("profile3")).thenReturn(json1);
        when(filterProfileService.get("profile4")).thenReturn(json2);

        final Gson gson = new Gson();
        final FilterProfileUtils filterProfileUtils = new FilterProfileUtils(filterProfileService, gson);
        final FilterProfile filterProfile = filterProfileUtils.getCombinedFilterProfiles(Arrays.asList("profile3", "profile4"));

        Assertions.assertNotNull(filterProfile);
        Assertions.assertTrue(filterProfile.getIdentifiers().hasFilter(FilterType.CUSTOM_DICTIONARY));
        Assertions.assertTrue(filterProfile.getIdentifiers().hasFilter(FilterType.ZIP_CODE));

    }

    @Test
    public void combineWithCryptoInFirst() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile5.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile6.json"), Charset.defaultCharset());

        final FilterProfileService filterProfileService = Mockito.mock(FilterProfileService.class);
        when(filterProfileService.get("profile5")).thenReturn(json1);
        when(filterProfileService.get("profile6")).thenReturn(json2);

        final Gson gson = new Gson();
        final FilterProfileUtils filterProfileUtils = new FilterProfileUtils(filterProfileService, gson);
        final FilterProfile filterProfile = filterProfileUtils.getCombinedFilterProfiles(Arrays.asList("profile5", "profile6"));

        Assertions.assertNotNull(filterProfile);
        Assertions.assertNotNull(filterProfile.getCrypto());
        Assertions.assertTrue(StringUtils.equalsIgnoreCase(filterProfile.getCrypto().getKey(), "keyhere"));
        Assertions.assertTrue(StringUtils.equalsIgnoreCase(filterProfile.getCrypto().getIv(), "ivhere"));
        Assertions.assertTrue(filterProfile.getIdentifiers().hasFilter(FilterType.ZIP_CODE));

    }

    @Test
    public void combineWithCryptoInSecond() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile6.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile5.json"), Charset.defaultCharset());

        final FilterProfileService filterProfileService = Mockito.mock(FilterProfileService.class);
        when(filterProfileService.get("profile6")).thenReturn(json1);
        when(filterProfileService.get("profile5")).thenReturn(json2);

        final Gson gson = new Gson();
        final FilterProfileUtils filterProfileUtils = new FilterProfileUtils(filterProfileService, gson);
        final FilterProfile filterProfile = filterProfileUtils.getCombinedFilterProfiles(Arrays.asList("profile6", "profile5"));

        Assertions.assertNotNull(filterProfile);
        Assertions.assertNull(filterProfile.getCrypto());
        Assertions.assertTrue(filterProfile.getIdentifiers().hasFilter(FilterType.ZIP_CODE));

    }

    @Test
    public void combineWithConfigInFirst() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile7.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile5.json"), Charset.defaultCharset());

        final FilterProfileService filterProfileService = Mockito.mock(FilterProfileService.class);
        when(filterProfileService.get("profile7")).thenReturn(json1);
        when(filterProfileService.get("profile5")).thenReturn(json2);

        final Gson gson = new Gson();
        final FilterProfileUtils filterProfileUtils = new FilterProfileUtils(filterProfileService, gson);
        final FilterProfile filterProfile = filterProfileUtils.getCombinedFilterProfiles(Arrays.asList("profile7", "profile5"));

        Assertions.assertNotNull(filterProfile);
        Assertions.assertNull(filterProfile.getCrypto());
        Assertions.assertTrue(filterProfile.getIdentifiers().hasFilter(FilterType.ZIP_CODE));

    }

    @Test
    public void combineWithIgnored() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile8.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile9.json"), Charset.defaultCharset());

        final FilterProfileService filterProfileService = Mockito.mock(FilterProfileService.class);
        when(filterProfileService.get("profile8")).thenReturn(json1);
        when(filterProfileService.get("profile9")).thenReturn(json2);

        final Gson gson = new Gson();
        final FilterProfileUtils filterProfileUtils = new FilterProfileUtils(filterProfileService, gson);
        final FilterProfile filterProfile = filterProfileUtils.getCombinedFilterProfiles(Arrays.asList("profile8", "profile9"));

        Assertions.assertNotNull(filterProfile);
        Assertions.assertNotNull(filterProfile.getIgnored());
        Assertions.assertEquals(2, filterProfile.getIgnored().size());
        Assertions.assertTrue(filterProfile.getIdentifiers().hasFilter(FilterType.ZIP_CODE));

    }

    @Test
    public void combineWithIgnoredPatterns() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile10.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile11.json"), Charset.defaultCharset());

        final FilterProfileService filterProfileService = Mockito.mock(FilterProfileService.class);
        when(filterProfileService.get("profile10")).thenReturn(json1);
        when(filterProfileService.get("profile11")).thenReturn(json2);

        final Gson gson = new Gson();
        final FilterProfileUtils filterProfileUtils = new FilterProfileUtils(filterProfileService, gson);
        final FilterProfile filterProfile = filterProfileUtils.getCombinedFilterProfiles(Arrays.asList("profile10", "profile11"));

        Assertions.assertNotNull(filterProfile);
        Assertions.assertNotNull(filterProfile.getIgnored());
        Assertions.assertEquals(2, filterProfile.getIgnoredPatterns().size());
        Assertions.assertFalse(filterProfile.getIdentifiers().hasFilter(FilterType.ZIP_CODE));

    }

}
