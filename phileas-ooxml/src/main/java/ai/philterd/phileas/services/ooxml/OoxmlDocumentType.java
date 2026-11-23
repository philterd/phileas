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
import ai.philterd.phileas.model.filtering.MimeType;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;

import java.io.ByteArrayInputStream;

/** The Office Open XML formats Phileas redacts, identified by the content type of the package's main part. */
public enum OoxmlDocumentType {

    DOCX(MimeType.APPLICATION_DOCX, "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"),
    XLSX(MimeType.APPLICATION_XLSX, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml");

    private final MimeType mimeType;
    private final String mainPartContentType;

    OoxmlDocumentType(final MimeType mimeType, final String mainPartContentType) {
        this.mimeType = mimeType;
        this.mainPartContentType = mainPartContentType;
    }

    public MimeType mimeType() {
        return mimeType;
    }

    /**
     * The format of {@code input}, or null when it is not a Word document or Excel workbook (including
     * templates and macro-enabled files, and any other zip).
     */
    public static OoxmlDocumentType of(final byte[] input) {
        if (input == null || input.length < 4 || input[0] != 'P' || input[1] != 'K') {
            return null;
        }
        final OPCPackage opc;
        try {
            opc = OPCPackage.open(new ByteArrayInputStream(input));
        } catch (final Exception e) {
            return null;
        }
        try {
            for (final PackageRelationship rel : opc.getRelationshipsByType(OoxmlPackage.OFFICE_DOCUMENT)) {
                final PackagePart main = opc.getPart(rel);
                if (main == null) {
                    return null;
                }
                for (final OoxmlDocumentType type : values()) {
                    if (type.mainPartContentType.equals(main.getContentType())) {
                        return type;
                    }
                }
                return null;
            }
            return null;
        } catch (final Exception e) {
            return null;
        } finally {
            // revert() releases the package without the save close() can attempt.
            opc.revert();
        }
    }

}
