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
package ai.philterd.phileas.services.csv;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Per-call options for CSV redaction, matching the Excel ones. How the document is read, and its column rules,
 * come from the policy's {@code config.csv} and {@code tabular}.
 */
public final class CsvRedactionOptions {

    private boolean headerRow = true;
    private boolean useHeaderContext = false;
    private Set<Integer> fullyRedactedColumns = Collections.emptySet();

    /** Options with the defaults. */
    public static CsvRedactionOptions defaults() {
        return new CsvRedactionOptions();
    }

    /**
     * Whether the first row is a header row: kept by fully redacted columns and used for header context. It is
     * scanned like any other row. When the policy has a {@code tabular} entry for the document, its
     * {@code headerRows} decide instead. Default {@code true}.
     */
    public boolean isHeaderRow() {
        return headerRow;
    }

    public CsvRedactionOptions setHeaderRow(final boolean headerRow) {
        this.headerRow = headerRow;
        return this;
    }

    /**
     * Give the detector each data field's column header as leading context ("SSN: 123456789"); only the field
     * is redacted. Default {@code false}.
     */
    public boolean isUseHeaderContext() {
        return useHeaderContext;
    }

    public CsvRedactionOptions setUseHeaderContext(final boolean useHeaderContext) {
        this.useHeaderContext = useHeaderContext;
        return this;
    }

    /**
     * Columns, counting from 1, whose data fields are all replaced, whether or not anything is detected. Header
     * rows are kept. Overrides the policy's column rules for these columns. Default none.
     */
    public Set<Integer> getFullyRedactedColumns() {
        return fullyRedactedColumns;
    }

    public CsvRedactionOptions setFullyRedactedColumns(final Set<Integer> columns) {
        this.fullyRedactedColumns = columns == null ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(columns));
        return this;
    }

}
