package spike;

import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;

import java.util.ArrayList;
import java.util.List;

import static spike.Office.*;
import static spike.Pkg.*;

/** Port of EmbeddedObjectRedactor: recurse into embedded Word/Excel packages, remove opaque OLE objects. */
final class EmbeddedObjects {

    private record Found(PackagePart parent, PackageRelationship rel, PackagePart part) { }

    static void process(final Pkg pkg, final Filter filter, final boolean removeUninspectable, final boolean write,
                        final List<OfficeSpan> captured, final Order order) throws Exception {
        final List<Found> found = new ArrayList<>();
        for (PackagePart parent : pkg.allParts(pkg.mainPart())) {
            if (parent.isRelationshipPart() || WordRedactor.CHART_CT.equals(parent.getContentType())) {
                continue;
            }
            for (PackageRelationship rel : parent.getRelationships()) {
                final String type = rel.getRelationshipType();
                if (type.equals(REL + "package") || type.equals(REL + "oleObject")) {
                    final PackagePart part = pkg.related(parent, rel);
                    if (part != null) {
                        found.add(new Found(parent, rel, part));
                    }
                }
            }
        }
        for (Found f : found) {
            handle(pkg, f, filter, removeUninspectable, write, captured, order);
        }
    }

    private static void handle(final Pkg pkg, final Found f, final Filter filter, final boolean removeUninspectable,
                               final boolean write, final List<OfficeSpan> captured, final Order order) throws Exception {
        final byte[] bytes = pkg.readBytes(f.part());
        if (bytes.length == 0) {
            return;
        }
        final boolean isPackage = f.rel().getRelationshipType().equals(REL + "package");
        final String ct = f.part().getContentType().toLowerCase();
        final boolean isXlsx = isPackage && ct.contains("spreadsheetml");
        final boolean isDocx = isPackage && ct.contains("wordprocessingml");

        if (!write) {
            final List<OfficeSpan> spans = isXlsx ? XlsxRedactor.detectEmbedded(bytes, filter)
                    : isDocx ? detectDocx(bytes, filter) : List.of();
            spans.forEach(s -> add(captured, order, s));
            return;
        }
        final List<OfficeSpan> embSpans = new ArrayList<>();
        byte[] redacted = null;
        if (isXlsx) {
            redacted = XlsxRedactor.redactEmbedded(bytes, filter, embSpans);
        } else if (isDocx) {
            redacted = redactDocx(bytes, filter, embSpans);
        }
        if (redacted != null) {
            pkg.writeBytes(f.part(), redacted);
            embSpans.forEach(s -> add(captured, order, s));
            return;
        }
        if (removeUninspectable) {
            pkg.deletePart(f.parent(), f.rel());
            captured.add(new OfficeSpan(order.next(), -1, 0, 0, "[embedded object]", "[removed]", "embedded-object-removed"));
        } else {
            captured.add(new OfficeSpan(order.next(), -1, 0, 0, "[embedded object]", "[kept, not inspected]", "embedded-object-kept"));
        }
    }

    private static void add(final List<OfficeSpan> captured, final Order order, final OfficeSpan s) {
        if (captured != null) {
            captured.add(new OfficeSpan(order.next(), -1, s.start(), s.end(), s.text(), s.replacement(), s.classification()));
        }
    }

    private static byte[] redactDocx(final byte[] bytes, final Filter filter, final List<OfficeSpan> spans) {
        try (Pkg pkg = new Pkg(bytes)) {
            if (pkg.mainPart() == null) {
                return null;
            }
            spans.addAll(WordRedactor.run(pkg, filter, true));
            return pkg.save();
        } catch (Exception e) {
            spans.clear();
            return null;
        }
    }

    private static List<OfficeSpan> detectDocx(final byte[] bytes, final Filter filter) {
        try (Pkg pkg = new Pkg(bytes)) {
            return pkg.mainPart() == null ? List.of() : WordRedactor.run(pkg, filter, false);
        } catch (Exception e) {
            return List.of();
        }
    }

    private EmbeddedObjects() { }
}
