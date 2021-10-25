package com.mtnfog.test.phileas.services.profiles.utils;

import com.google.gson.Gson;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.services.FilterProfileService;
import com.mtnfog.phileas.services.profiles.utils.FilterProfileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.mockito.Mockito.when;

public class FilterProfileUtilsTest {

    @Test
    public void onlyOne() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/profiles/profile1.json"), Charset.defaultCharset());

        final FilterProfileService filterProfileService = Mockito.mock(FilterProfileService.class);
        when(filterProfileService.get("profile1")).thenReturn(json1);

        final Gson gson = new Gson();
        final FilterProfileUtils filterProfileUtils = new FilterProfileUtils(filterProfileService, gson);
        final FilterProfile filterProfile = filterProfileUtils.getCombinedFilterProfiles(Arrays.asList("profile1"));

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

}
