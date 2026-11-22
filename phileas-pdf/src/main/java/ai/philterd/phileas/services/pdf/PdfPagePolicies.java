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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Assigns policies to sets of pages of one PDF, passed per call to {@code PdfFilterService}. The policy passed
 * to {@code filter} applies to every page not assigned here. Pages are numbered from 1.
 */
public final class PdfPagePolicies {

    /** The assignment label of spans found on pages not assigned to any policy here. */
    public static final String DEFAULT_ASSIGNMENT = "default";

    private final List<Assignment> assignments = new ArrayList<>();

    /**
     * Assigns a policy to a page set, labeled with the page set as written.
     * @see #assign(String, Policy, String)
     */
    public PdfPagePolicies assign(final String pages, final Policy policy) {
        return assign(pages, policy, pages == null ? null : pages.replaceAll("\\s", ""));
    }

    /**
     * Assigns a policy to a page set.
     * @param pages Single pages, ranges, and open-ended ranges, separated by commas, such as {@code 3},
     *              {@code 2-5}, {@code 1,2,5}, {@code 2-5,8}, or {@code 5-}.
     * @param policy The policy for those pages.
     * @param label The label recorded on each span found on those pages. Must be unique and not
     *              {@value #DEFAULT_ASSIGNMENT}.
     * @throws IllegalArgumentException if the page set is malformed, overlaps one already assigned, or the
     *                                  label is blank or already used.
     */
    public PdfPagePolicies assign(final String pages, final Policy policy, final String label) {

        Objects.requireNonNull(policy, "policy");

        final List<PageRange> ranges = parse(pages);

        if (StringUtils.isBlank(label)) {
            throw new IllegalArgumentException("A page assignment needs a label.");
        }
        if (DEFAULT_ASSIGNMENT.equals(label)) {
            throw new IllegalArgumentException("The page assignment label '" + DEFAULT_ASSIGNMENT
                    + "' is reserved for pages not assigned a policy.");
        }
        if (assignments.stream().anyMatch(a -> a.label().equals(label))) {
            throw new IllegalArgumentException("The page assignment label '" + label + "' is already used.");
        }

        for (final Assignment existing : assignments) {
            for (final PageRange range : ranges) {
                for (final PageRange other : existing.ranges()) {
                    final PageRange overlap = range.intersect(other);
                    if (overlap != null) {
                        throw new IllegalArgumentException(overlap.describe() + " assigned to both '"
                                + existing.label() + "' and '" + label + "'.");
                    }
                }
            }
        }

        assignments.add(new Assignment(label, List.copyOf(ranges), policy));
        return this;

    }

    public List<Assignment> getAssignments() {
        return Collections.unmodifiableList(assignments);
    }

    /** The assignment for a page, numbered from 1, or {@code null} if the page is not assigned. */
    public Assignment assignmentFor(final int page) {
        for (final Assignment assignment : assignments) {
            if (assignment.contains(page)) {
                return assignment;
            }
        }
        return null;
    }

    /**
     * @throws IllegalArgumentException naming the pages if any assigned page is beyond the document's last page.
     */
    public void validate(final int pageCount) {

        final List<String> beyond = new ArrayList<>();

        for (final Assignment assignment : assignments) {
            for (final PageRange range : assignment.ranges()) {
                final PageRange outside = range.intersect(new PageRange(pageCount + 1, PageRange.OPEN));
                // An open-ended range is only beyond the end when it starts there.
                if (outside != null && (range.end() != PageRange.OPEN || range.start() > pageCount)) {
                    beyond.add(outside + " ('" + assignment.label() + "')");
                }
            }
        }

        if (!beyond.isEmpty()) {
            throw new IllegalArgumentException("The document has " + pageCount + " page" + (pageCount == 1 ? "" : "s")
                    + ", but pages beyond it are assigned a policy: " + String.join(", ", beyond) + ".");
        }

    }

    private static List<PageRange> parse(final String pages) {

        if (StringUtils.isBlank(pages)) {
            throw new IllegalArgumentException("A page assignment needs a page set.");
        }

        final List<PageRange> ranges = new ArrayList<>();

        for (final String part : pages.replaceAll("\\s", "").split(",", -1)) {

            final int dash = part.indexOf('-');
            final PageRange range;

            try {
                if (dash < 0) {
                    final int page = Integer.parseInt(part);
                    range = new PageRange(page, page);
                } else {
                    final int start = Integer.parseInt(part.substring(0, dash));
                    final String end = part.substring(dash + 1);
                    range = new PageRange(start, end.isEmpty() ? PageRange.OPEN : Integer.parseInt(end));
                }
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("'" + part + "' in page set '" + pages + "' is not a page or range.");
            }

            if (range.start() < 1 || range.end() < range.start()) {
                throw new IllegalArgumentException("'" + part + "' in page set '" + pages + "' is not a page or range.");
            }

            for (final PageRange other : ranges) {
                final PageRange overlap = range.intersect(other);
                if (overlap != null) {
                    throw new IllegalArgumentException(overlap.describe() + " listed twice in page set '" + pages + "'.");
                }
            }

            ranges.add(range);

        }

        return ranges;

    }

    /** A policy and the pages it is assigned to. */
    public record Assignment(String label, List<PageRange> ranges, Policy policy) {

        public boolean contains(final int page) {
            return ranges.stream().anyMatch(r -> page >= r.start() && page <= r.end());
        }

    }

    /** Pages {@code start} to {@code end}, inclusive, numbered from 1. An open-ended range ends at {@link #OPEN}. */
    public record PageRange(int start, int end) {

        public static final int OPEN = Integer.MAX_VALUE;

        PageRange intersect(final PageRange other) {
            final int s = Math.max(start, other.start);
            final int e = Math.min(end, other.end);
            return s <= e ? new PageRange(s, e) : null;
        }

        // "Page 3 is" or "Pages 3-5 are", for messages.
        String describe() {
            return start == end ? "Page " + this + " is" : "Pages " + this + " are";
        }

        @Override
        public String toString() {
            if (end == OPEN) {
                return start + "-";
            }
            return start == end ? String.valueOf(start) : start + "-" + end;
        }

    }

}
