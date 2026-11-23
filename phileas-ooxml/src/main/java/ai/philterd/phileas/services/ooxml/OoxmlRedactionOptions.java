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
package ai.philterd.phileas.services.ooxml;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Options for Word and Excel redaction. The defaults match phileas-dotnet's.
 */
public final class OoxmlRedactionOptions {

    private boolean highlight = false;
    private boolean redactHeadersFooters = true;
    private boolean redactCharts = true;
    private boolean removeUninspectableEmbeddedObjects = true;
    private boolean redactFormulaValues = true;
    private boolean redactPivotCaches = true;
    private boolean useHeaderContext = false;
    private Set<Integer> fullyRedactedColumns = Collections.emptySet();
    private String worksheet = null;

    /** Options with the defaults. */
    public static OoxmlRedactionOptions defaults() {
        return new OoxmlRedactionOptions();
    }

    /** Word: wrap each replacement in a yellow highlight. Default {@code false}. */
    public boolean isHighlight() {
        return highlight;
    }

    public OoxmlRedactionOptions setHighlight(final boolean highlight) {
        this.highlight = highlight;
        return this;
    }

    /** Redact Word headers and footers, and Excel print headers and footers. Default {@code true}. */
    public boolean isRedactHeadersFooters() {
        return redactHeadersFooters;
    }

    public OoxmlRedactionOptions setRedactHeadersFooters(final boolean redactHeadersFooters) {
        this.redactHeadersFooters = redactHeadersFooters;
        return this;
    }

    /** Redact chart text and cached chart values. Default {@code true}. */
    public boolean isRedactCharts() {
        return redactCharts;
    }

    public OoxmlRedactionOptions setRedactCharts(final boolean redactCharts) {
        this.redactCharts = redactCharts;
        return this;
    }

    /**
     * Remove embedded objects that cannot be inspected (anything but an embedded Word or Excel document).
     * When {@code false} they are kept and reported. Default {@code true}.
     */
    public boolean isRemoveUninspectableEmbeddedObjects() {
        return removeUninspectableEmbeddedObjects;
    }

    public OoxmlRedactionOptions setRemoveUninspectableEmbeddedObjects(final boolean remove) {
        this.removeUninspectableEmbeddedObjects = remove;
        return this;
    }

    /**
     * Excel: redact the cached results of formula cells, and when any cell is redacted, clear formula
     * caches and set the workbook to recalculate on open. Default {@code true}.
     */
    public boolean isRedactFormulaValues() {
        return redactFormulaValues;
    }

    public OoxmlRedactionOptions setRedactFormulaValues(final boolean redactFormulaValues) {
        this.redactFormulaValues = redactFormulaValues;
        return this;
    }

    /** Excel: redact pivot caches and set a redacted cache to refresh on open. Default {@code true}. */
    public boolean isRedactPivotCaches() {
        return redactPivotCaches;
    }

    public OoxmlRedactionOptions setRedactPivotCaches(final boolean redactPivotCaches) {
        this.redactPivotCaches = redactPivotCaches;
        return this;
    }

    /**
     * Excel: give the detector each data cell's column header as leading context ("SSN: 123456789"); only
     * the cell is redacted. Default {@code false}.
     */
    public boolean isUseHeaderContext() {
        return useHeaderContext;
    }

    public OoxmlRedactionOptions setUseHeaderContext(final boolean useHeaderContext) {
        this.useHeaderContext = useHeaderContext;
        return this;
    }

    /**
     * Excel: 1-based columns whose data cells are all replaced, whether or not anything is detected. The
     * header row is kept. Default none.
     */
    public Set<Integer> getFullyRedactedColumns() {
        return fullyRedactedColumns;
    }

    public OoxmlRedactionOptions setFullyRedactedColumns(final Set<Integer> columns) {
        this.fullyRedactedColumns = columns == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(columns));
        return this;
    }

    /** Excel: the only worksheet to redact, by name, or {@code null} for the whole workbook. Default {@code null}. */
    public String getWorksheet() {
        return worksheet;
    }

    public OoxmlRedactionOptions setWorksheet(final String worksheet) {
        this.worksheet = worksheet;
        return this;
    }

}
