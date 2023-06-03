package ai.philterd.test.phileas.services.postfilters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.profile.FilterProfile;
import ai.philterd.phileas.model.profile.Ignored;
import ai.philterd.phileas.services.postfilters.IgnoredTermsFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class IgnoredTermsFilterTest {

    @Test
    public void ignored() throws IOException {

        final Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("Washington", "California", "Virginia"));

        final FilterProfile filterProfile = new FilterProfile();
        filterProfile.setIgnored(Arrays.asList(ignored));

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.LOCATION_STATE, "context", "docid", 0.80, "test", "*****",  "", false, new String[0]));

        final IgnoredTermsFilter ignoredTermsFilter = new IgnoredTermsFilter(ignored);
        final List<Span> filteredSpans = ignoredTermsFilter.filter("He lived in Washington.", spans);

        Assertions.assertEquals(0, filteredSpans.size());

    }

    @Test
    public void ignoredFile1() throws IOException {

        final Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("Seattle", "California", "Virginia"));
        ignored.setFiles(Arrays.asList(new File("src/test/resources/ignored-terms.txt").getAbsolutePath()));

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 18, FilterType.IDENTIFIER, "context", "docid", 0.80, "test", "*****",  "", false, new String[0]));

        final IgnoredTermsFilter ignoredTermsFilter = new IgnoredTermsFilter(ignored);
        final List<Span> filteredSpans = ignoredTermsFilter.filter("He lived in samuel.", spans);

        Assertions.assertEquals(0, filteredSpans.size());

    }

    @Test
    public void ignoredFile2() throws IOException {

        final Ignored ignored = new Ignored();
        ignored.setFiles(Arrays.asList(new File("src/test/resources/ignored-terms.txt").getAbsolutePath()));

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 18, FilterType.IDENTIFIER, "context", "docid", 0.80, "test", "*****",  "", false, new String[0]));

        final IgnoredTermsFilter ignoredTermsFilter = new IgnoredTermsFilter(ignored);
        final List<Span> filteredSpans = ignoredTermsFilter.filter("He lived in samuel.", spans);

        Assertions.assertEquals(0, filteredSpans.size());

    }

    @Test()
    public void ignoredFile3() {

        final Ignored ignored = new Ignored();
        ignored.setFiles(Arrays.asList(new File("src/test/resources/does-not-exist.txt").getAbsolutePath()));

        Assertions.assertThrows(IOException.class, () -> {
            final IgnoredTermsFilter ignoredTermsFilter = new IgnoredTermsFilter(ignored);
        });

    }

    @Test
    public void notIgnored() throws IOException {

        final Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("Seattle", "California", "Virginia"));

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.LOCATION_STATE, "context", "docid", 0.80, "test", "*****",  "", false, new String[0]));

        final IgnoredTermsFilter ignoredTermsFilter = new IgnoredTermsFilter(ignored);
        final List<Span> filteredSpans = ignoredTermsFilter.filter("He lived in Washington.", spans);

        Assertions.assertEquals(1, filteredSpans.size());

    }

    @Test
    public void caseSensitive1Test() throws IOException {

        final Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("washington", "California", "Virginia"));

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.LOCATION_STATE, "context", "docid", 0.80, "test", "*****",  "", false, new String[0]));

        final IgnoredTermsFilter ignoredTermsFilter = new IgnoredTermsFilter(ignored);
        final List<Span> filteredSpans = ignoredTermsFilter.filter("He lived in Washington.", spans);

        Assertions.assertEquals(0, filteredSpans.size());

    }

    @Test
    public void caseSensitive2Test() throws IOException {

        final Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("Washington", "California", "Virginia"));

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.LOCATION_STATE, "context", "docid", 0.80, "test", "*****",  "", false, new String[0]));

        final IgnoredTermsFilter ignoredTermsFilter = new IgnoredTermsFilter(ignored);
        final List<Span> filteredSpans = ignoredTermsFilter.filter("He lived in Washington.", spans);

        Assertions.assertEquals(0, filteredSpans.size());

    }

}
