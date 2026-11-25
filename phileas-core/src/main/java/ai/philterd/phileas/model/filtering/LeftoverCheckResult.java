/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.model.filtering;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.List;

/**
 * The outcome of the policy's {@code config.review.leftoverCheck} for one filter call.
 */
public final class LeftoverCheckResult {

    /** Why the check was not performed. */
    public enum Reason {

        /** The policy's mode did not call for the check on this call. */
        @SerializedName("notRequested")
        NOT_REQUESTED,

        /** The output has no readable text, such as a PDF whose pages were all flattened to images. */
        @SerializedName("unreadableOutput")
        UNREADABLE_OUTPUT

    }

    private static final LeftoverCheckResult NOT_REQUESTED = new LeftoverCheckResult(false, Reason.NOT_REQUESTED, List.of());

    private final boolean performed;
    private final Reason reason;
    private final int findingCount;
    private final List<LeftoverFinding> findings;

    private LeftoverCheckResult(final boolean performed, final Reason reason, final List<LeftoverFinding> findings) {
        this.performed = performed;
        this.reason = reason;
        this.findings = Collections.unmodifiableList(findings);
        this.findingCount = findings.size();
    }

    /**
     * A check that ran.
     * @param findings What it found.
     * @return The {@link LeftoverCheckResult}.
     */
    public static LeftoverCheckResult performed(final List<LeftoverFinding> findings) {
        return new LeftoverCheckResult(true, null, List.copyOf(findings));
    }

    /**
     * A check that did not run.
     * @param reason Why not.
     * @return The {@link LeftoverCheckResult}.
     */
    public static LeftoverCheckResult notPerformed(final Reason reason) {
        return reason == Reason.NOT_REQUESTED ? NOT_REQUESTED : new LeftoverCheckResult(false, reason, List.of());
    }

    /**
     * Whether the check ran. When it did not, {@link #getReason()} says why, and the count is not a zero
     * finding: nothing was checked.
     * @return <code>true</code> if the check ran.
     */
    public boolean isPerformed() {
        return performed;
    }

    /**
     * Why the check did not run.
     * @return The {@link Reason}, or {@code null} when it ran.
     */
    public Reason getReason() {
        return reason;
    }

    public int getFindingCount() {
        return findingCount;
    }

    public List<LeftoverFinding> getFindings() {
        return findings;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

}
