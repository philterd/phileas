/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.pdf;

import ai.philterd.phileas.policy.Policy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.IntStream;

public class PdfPagePoliciesTest {

    private static List<Integer> pages(final PdfPagePolicies pagePolicies, final String label, final int upTo) {
        final PdfPagePolicies.Assignment assignment = pagePolicies.getAssignments().stream()
                .filter(a -> a.label().equals(label)).findFirst().orElseThrow();
        return IntStream.rangeClosed(1, upTo).filter(assignment::contains).boxed().toList();
    }

    @Test
    public void parsesPagesRangesListsAndOpenEndedRanges() {

        final PdfPagePolicies pagePolicies = new PdfPagePolicies()
                .assign("3", new Policy())
                .assign("1, 2", new Policy())
                .assign("4-5,8", new Policy(), "exhibits")
                .assign("10-", new Policy());

        Assertions.assertEquals(List.of(3), pages(pagePolicies, "3", 12));
        Assertions.assertEquals(List.of(1, 2), pages(pagePolicies, "1,2", 12));
        Assertions.assertEquals(List.of(4, 5, 8), pages(pagePolicies, "exhibits", 12));
        Assertions.assertEquals(List.of(10, 11, 12), pages(pagePolicies, "10-", 12));

        Assertions.assertEquals("exhibits", pagePolicies.assignmentFor(8).label());
        Assertions.assertNull(pagePolicies.assignmentFor(6));
        Assertions.assertNull(pagePolicies.assignmentFor(9));
        Assertions.assertEquals("10-", pagePolicies.assignmentFor(1000).label());

    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "a", "0", "5-3", "-3", "2-5,", "1,,2", "2-x"})
    public void rejectsMalformedPageSets(final String pages) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PdfPagePolicies().assign(pages, new Policy()));
    }

    @Test
    public void rejectsAMissingPageSetAsSuch() {
        final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new PdfPagePolicies().assign(null, new Policy()));
        Assertions.assertEquals("A page assignment needs a page set.", e.getMessage());
    }

    @Test
    public void rejectsOverlappingPageSetsNamingThePages() {

        final PdfPagePolicies pagePolicies = new PdfPagePolicies().assign("2-5", new Policy(), "a");

        final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> pagePolicies.assign("1,4-", new Policy(), "b"));
        Assertions.assertEquals("Pages 4-5 are assigned to both 'a' and 'b'.", e.getMessage());

        final IllegalArgumentException within = Assertions.assertThrows(IllegalArgumentException.class,
                () -> pagePolicies.assign("7-9,8", new Policy(), "c"));
        Assertions.assertEquals("Page 8 is listed twice in page set '7-9,8'.", within.getMessage());

        Assertions.assertEquals(1, pagePolicies.getAssignments().size());

    }

    @Test
    public void rejectsBlankDuplicateAndReservedLabels() {

        final PdfPagePolicies pagePolicies = new PdfPagePolicies().assign("1", new Policy(), "cover");

        Assertions.assertThrows(IllegalArgumentException.class, () -> pagePolicies.assign("2", new Policy(), "cover"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> pagePolicies.assign("2", new Policy(), " "));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> pagePolicies.assign("2", new Policy(), PdfPagePolicies.DEFAULT_ASSIGNMENT));
        Assertions.assertThrows(NullPointerException.class, () -> pagePolicies.assign("2", null, "x"));

    }

    @Test
    public void validatesPagesAgainstTheDocumentLength() {

        new PdfPagePolicies().assign("1-4", new Policy()).assign("5-", new Policy()).validate(5);
        new PdfPagePolicies().assign("3-", new Policy()).validate(3);

        final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new PdfPagePolicies().assign("2-6", new Policy(), "a").assign("9-", new Policy(), "b").validate(4));
        Assertions.assertEquals("The document has 4 pages, but pages beyond it are assigned a policy: 5-6 ('a'), 9- ('b').", e.getMessage());

    }

}
