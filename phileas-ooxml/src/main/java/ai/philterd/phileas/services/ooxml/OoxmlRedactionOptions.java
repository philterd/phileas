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

import ai.philterd.phileas.policy.config.Excel;
import ai.philterd.phileas.policy.config.Word;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Options for Word and Excel redaction: the policy's {@code config.word} and {@code config.excel} settings, and
 * the per-call options that pick a worksheet and columns. {@code OoxmlFilterService} takes the Word and Excel
 * settings from each call's policy; set them here when calling the redactors directly.
 */
public final class OoxmlRedactionOptions {

    private Word word = new Word();
    private Excel excel = new Excel();
    private boolean useHeaderContext = false;
    private Set<Integer> fullyRedactedColumns = Collections.emptySet();
    private String worksheet = null;

    /** Options with the defaults: the policy schema's defaults for Word and Excel, and no per-call options. */
    public static OoxmlRedactionOptions defaults() {
        return new OoxmlRedactionOptions();
    }

    /** A copy of these options. */
    public OoxmlRedactionOptions copy() {
        return new OoxmlRedactionOptions().setWord(word).setExcel(excel).setUseHeaderContext(useHeaderContext)
                .setFullyRedactedColumns(fullyRedactedColumns).setWorksheet(worksheet);
    }

    /**
     * The options for a document embedded in the one being redacted: the same Word and Excel settings, without
     * the per-call options, which pick worksheets and columns of the outer document.
     */
    OoxmlRedactionOptions forEmbedded() {
        return new OoxmlRedactionOptions().setWord(word).setExcel(excel);
    }

    /** The Word settings, as in the policy's {@code config.word}. */
    public Word getWord() {
        return word.copy();
    }

    public OoxmlRedactionOptions setWord(final Word word) {
        this.word = word == null ? new Word() : word.copy();
        return this;
    }

    /** The Excel settings, as in the policy's {@code config.excel}. */
    public Excel getExcel() {
        return excel.copy();
    }

    public OoxmlRedactionOptions setExcel(final Excel excel) {
        this.excel = excel == null ? new Excel() : excel.copy();
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
     * header rows are kept. Default none.
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

    // --- the settings as the redactors read them ------------------------------------------------

    boolean wordHighlight() {
        return word.isHighlight();
    }

    boolean wordRedactsHeadersFooters() {
        return word.getHeadersFooters() == Word.RedactOrKeep.REDACT;
    }

    boolean wordRedactsCharts() {
        return word.getCharts() == Word.RedactOrKeep.REDACT;
    }

    boolean wordRemovesUninspectableObjects() {
        return word.getUninspectableObjects() == Word.RemoveOrKeep.REMOVE;
    }

    boolean wordRemovesComments() {
        return word.getComments() == Word.RemoveOrRedact.REMOVE;
    }

    boolean wordAcceptsTrackedChanges() {
        return word.getTrackedChanges() == Word.AcceptOrRedact.ACCEPT;
    }

    boolean wordRemovesHiddenText() {
        return word.getHiddenText() == Word.RemoveOrRedact.REMOVE;
    }

    boolean wordRemovesDocumentProperties() {
        return word.isRemoveDocumentProperties();
    }

    boolean excelRedactsPrintHeadersFooters() {
        return excel.getPrintHeadersFooters() == Word.RedactOrKeep.REDACT;
    }

    boolean excelRedactsCharts() {
        return excel.getCharts() == Word.RedactOrKeep.REDACT;
    }

    boolean excelRedactsFormulaCaches() {
        return excel.getFormulaCaches() == Word.RedactOrKeep.REDACT;
    }

    boolean excelRedactsPivotCaches() {
        return excel.getPivotCaches() == Word.RedactOrKeep.REDACT;
    }

    boolean excelRemovesUninspectableObjects() {
        return excel.getUninspectableObjects() == Word.RemoveOrKeep.REMOVE;
    }

    boolean excelRemovesDocumentProperties() {
        return excel.isRemoveDocumentProperties();
    }

}
