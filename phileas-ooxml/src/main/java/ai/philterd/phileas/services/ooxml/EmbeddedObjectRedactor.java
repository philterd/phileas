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

import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static ai.philterd.phileas.services.ooxml.OfficeSpanMath.Order;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.REL;

/**
 * Embedded objects (Insert, Object): an embedded Word or Excel document is redacted in place; any other
 * embedded object cannot be inspected and is removed, or kept and reported. Workbooks embedded under a
 * chart are handled by the chart pass. Runs at the top level of a redaction only, so it does not recurse
 * through nested embeddings. A port of phileas-dotnet's {@code EmbeddedObjectRedactor}.
 */
public final class EmbeddedObjectRedactor {

    static final String REMOVED_CLASSIFICATION = "embedded-object-removed";
    static final String KEPT_CLASSIFICATION = "embedded-object-kept";

    private static final String PACKAGE = REL + "package";
    private static final String OLE_OBJECT = REL + "oleObject";

    private EmbeddedObjectRedactor() {
    }

    /** True when the Word document carries an embedded object that cannot be inspected. */
    public static boolean docxHasUninspectable(final byte[] input) {
        return hasUninspectable(input);
    }

    /** True when the workbook carries an embedded object that cannot be inspected. */
    public static boolean xlsxHasUninspectable(final byte[] input) {
        return hasUninspectable(input);
    }

    private static boolean hasUninspectable(final byte[] input) {
        try (OoxmlPackage pkg = new OoxmlPackage(input)) {
            for (final Found found : find(pkg)) {
                if (isOpaque(found)) {
                    return true;
                }
            }
            return false;
        } catch (final IOException e) {
            return false;
        }
    }

    // An embedded workbook follows the policy's column rules, as the document it is embedded in does.
    static void process(final OoxmlPackage pkg, final TextFilter filter, final ColumnRules rules,
                        final boolean removeUninspectable, final boolean write,
                        final List<OfficeRedactionSpan> captured, final Order order) {
        for (final Found found : find(pkg)) {
            handle(pkg, found, filter, rules, removeUninspectable, write, captured, order);
        }
    }

    private record Found(PackagePart parent, PackageRelationship rel, PackagePart part) {
    }

    // Every embedded package or OLE object whose parent is not a chart part.
    private static List<Found> find(final OoxmlPackage pkg) {
        final List<Found> found = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (final PackagePart parent : pkg.allPackageParts()) {
            if (WordDocumentRedactor.CHART_CONTENT_TYPE.equals(parent.getContentType())) {
                continue;
            }
            for (final PackageRelationship rel : pkg.relationships(parent)) {
                final String type = rel.getRelationshipType();
                if (!PACKAGE.equals(type) && !OLE_OBJECT.equals(type)) {
                    continue;
                }
                final PackagePart part = pkg.related(parent, rel);
                if (part != null && seen.add(part.getPartName().getName().toLowerCase(Locale.ROOT))) {
                    found.add(new Found(parent, rel, part));
                }
            }
        }
        return found;
    }

    private static boolean isPackage(final Found found) {
        return PACKAGE.equals(found.rel().getRelationshipType());
    }

    private static boolean isXlsx(final Found found) {
        return isPackage(found) && found.part().getContentType().toLowerCase(Locale.ROOT).contains("spreadsheetml");
    }

    private static boolean isDocx(final Found found) {
        return isPackage(found) && found.part().getContentType().toLowerCase(Locale.ROOT).contains("wordprocessingml");
    }

    private static boolean isOpaque(final Found found) {
        return !isXlsx(found) && !isDocx(found);
    }

    private static void handle(final OoxmlPackage pkg, final Found found, final TextFilter filter,
                               final ColumnRules rules, final boolean removeUninspectable, final boolean write,
                               final List<OfficeRedactionSpan> captured, final Order order) {
        final byte[] bytes = pkg.partBytes(found.part());
        if (bytes == null || bytes.length == 0) {
            return;
        }
        if (!write) {
            final List<OfficeRedactionSpan> spans = isXlsx(found) ? XlsxRedactor.detectEmbeddedBytes(bytes, filter, rules)
                    : isDocx(found) ? WordDocumentRedactor.detectEmbeddedBytes(bytes, filter, rules) : List.of();
            spans.forEach(s -> XlsxRedactor.addEmbedded(captured, order, s));
            return;
        }
        final List<OfficeRedactionSpan> embeddedSpans = new ArrayList<>();
        byte[] redacted = null;
        if (isXlsx(found)) {
            redacted = XlsxRedactor.redactEmbeddedBytes(bytes, filter, rules, embeddedSpans);
        } else if (isDocx(found)) {
            redacted = WordDocumentRedactor.redactEmbeddedBytes(bytes, filter, rules, embeddedSpans);
        }
        if (redacted != null) {
            pkg.replacePart(found.part(), redacted);
            embeddedSpans.forEach(s -> XlsxRedactor.addEmbedded(captured, order, s));
            return;
        }
        // Opaque, or an embedded document that did not parse: its content cannot be inspected.
        if (removeUninspectable) {
            pkg.deletePart(found.parent(), found.rel());
            marker(captured, order, "[removed]", REMOVED_CLASSIFICATION);
        } else {
            marker(captured, order, "[kept, not inspected]", KEPT_CLASSIFICATION);
        }
    }

    private static void marker(final List<OfficeRedactionSpan> captured, final Order order, final String replacement,
                               final String classification) {
        if (captured != null) {
            captured.add(new OfficeRedactionSpan(order.next(), -1, 0, 0, "[embedded object]", replacement, classification));
        }
    }

}
