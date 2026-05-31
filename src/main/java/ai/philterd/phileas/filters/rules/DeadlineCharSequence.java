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
package ai.philterd.phileas.filters.rules;

/**
 * Wraps a {@link CharSequence} so each {@link #charAt(int)} enforces a deadline. A {@link
 * java.util.regex.Matcher} calls charAt() continually while backtracking, so a
 * catastrophic-backtracking pattern trips the deadline and aborts with a {@link
 * RegexTimeoutException} instead of hanging the matching thread. The clock is reset per match
 * attempt via {@link #startClock()}, and nanoTime() is sampled every 1024th access so the guard
 * adds negligible overhead to legitimate matches.
 */
final class DeadlineCharSequence implements CharSequence {

    private static final int SAMPLE_MASK = 0x3FF; // check the clock every 1024 accesses

    private final CharSequence inner;
    private final long budgetNanos; // a value <= 0 disables the guard
    private long deadlineNanos;
    private int accessCount;

    DeadlineCharSequence(final CharSequence inner, final long budgetMs) {
        this.inner = inner;
        this.budgetNanos = budgetMs * 1_000_000L;
    }

    /**
     * Resets the deadline relative to now. Call before each {@link java.util.regex.Matcher#find()}
     * so each individual match attempt is bounded rather than the loop as a whole.
     */
    void startClock() {
        this.deadlineNanos = System.nanoTime() + budgetNanos;
    }

    @Override
    public char charAt(final int index) {
        if (budgetNanos > 0 && (++accessCount & SAMPLE_MASK) == 0 && System.nanoTime() > deadlineNanos) {
            throw new RegexTimeoutException("Regex match exceeded the configured time budget.");
        }
        return inner.charAt(index);
    }

    @Override
    public int length() {
        return inner.length();
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return inner.subSequence(start, end);
    }

    @Override
    public String toString() {
        return inner.toString();
    }

}
