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
package ai.philterd.phileas.services.rtf;

import ai.philterd.phileas.model.filtering.RedactedTextBuilder;
import ai.philterd.phileas.model.filtering.ReplacedRange;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.config.Word;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Redacts RTF documents on their token stream. Text is replaced in place, in the runs that hold it, so the rest of
 * the document is written back byte for byte. The policy's {@code config.word} settings are applied first:
 * {@code headersFooters}, {@code comments}, {@code trackedChanges}, {@code hiddenText}, {@code uninspectableObjects},
 * and {@code removeDocumentProperties}; {@code charts} and {@code highlight} have no RTF counterpart. Authorship in
 * {@code \info}, comment authors, and revision authors are always cleared.
 *
 * <p>Spans have offsets into their paragraph, cell, or other run of text, and that run's index in the document as
 * their {@link Span#getParagraphNumber() paragraph number}.</p>
 */
public final class RtfRedactor {

    /** The replacement for a comment or revision author. */
    static final String NEUTRAL_AUTHOR = "Redacted";

    // Revision marks removed when changes are accepted.
    private static final Set<String> REVISION_WORDS = Set.of("deleted", "revised", "revauth", "revauthdel", "revdttm",
            "revdttmdel", "crauth", "crdate");

    /** A redacted document and the redactions made in it. */
    public record Result(byte[] document, List<Span> spans) {
    }

    /** Segment labels of an RTF document's redacted text. */
    public static final String BODY_LABEL = "body";
    public static final String HEADERS_FOOTERS_LABEL = "headers and footers";
    public static final String FOOTNOTES_LABEL = "footnotes";
    public static final String COMMENTS_LABEL = "comments";
    public static final String TRACKED_CHANGES_LABEL = "tracked changes";

    // The document properties removeDocumentProperties clears, by destination, and their labels in the text.
    private static final Map<String, String> PROPERTIES = new LinkedHashMap<>();

    static {
        PROPERTIES.put("title", "title");
        PROPERTIES.put("subject", "subject");
        PROPERTIES.put("keywords", "keywords");
        PROPERTIES.put("category", "category");
        PROPERTIES.put("doccomm", "comments");
    }

    /**
     * A document's redacted text and the redactions that produced it.
     * @param text The redacted text.
     * @param spans The redactions, as {@link #redact} reports them.
     */
    public record TextResult(RedactedTextBuilder text, List<Span> spans) {
    }

    // A span to apply and the units of its run it replaces.
    private record Planned(Span span, int first, int end) {
    }

    private RtfRedactor() {
    }

    /**
     * Redacts a document's text without producing a document: one line per paragraph or table cell, in the body
     * (with shape text), headers and footers, footnotes, comments, and tracked deletions, after the
     * {@code config.word} settings, as {@link #redact} reads them. Each run is detected once, and a replacement
     * takes the place of the same characters it does in the document. Headers and footers kept by
     * {@code headersFooters} are left unscanned and left out. Field instructions and HTML tags are not included,
     * nor, in RTF that encapsulates HTML, the RTF-only text ({@code \htmlrtf}) that stands in for the HTML when
     * an RTF reader shows it, as {@link RtfTextExtractor} leaves it out.
     * @param input The RTF document.
     * @param filter Detects what to redact in a run of text.
     * @param word The {@code config.word} settings.
     * @param metadata Whether to add the document properties the settings keep, redacted, as a last segment.
     * @return The text and its spans.
     * @throws RtfFormatException when the document is malformed or exceeds a limit.
     */
    public static TextResult redactToText(final byte[] input, final Function<String, TextFilterResult> filter,
                                          final Word word, final boolean metadata) {

        final List<RtfToken> tokens = RtfTokenizer.tokenize(prepare(read(input), word));
        final RtfAnalysis analysis = new RtfAnalysis(tokens);
        final RedactedTextBuilder text = new RedactedTextBuilder();
        final List<Span> spans = new ArrayList<>();
        text.ensure(BODY_LABEL);

        for (int i = 0; i < analysis.targets.size(); i++) {
            final RtfAnalysis.Target target = analysis.targets.get(i);
            final String label = label(target.kind);
            if (label == null) {
                continue;
            }
            if (!scanned(target, word)) {
                text.notScanned(label);
                continue;
            }
            final String original = target.text();
            final List<Planned> planned = plan(target, detected(filter.apply(original), original));
            final Rendered rendered = rendered(target, planned);
            text.line(label, rendered.text(), rendered.ranges());
            for (final Planned p : planned) {
                p.span().setParagraphNumber(i);
                spans.add(p.span());
            }
        }

        if (metadata) {
            if (word.isRemoveDocumentProperties()) {
                text.notScanned(RedactedTextBuilder.METADATA_LABEL);
            } else {
                metadata(tokens, analysis, filter, text);
            }
        }

        return new TextResult(text, spans);

    }

    // The segment a run's text goes in, or null for text that is not included.
    private static String label(final RtfAnalysis.Kind kind) {
        return switch (kind) {
            case BODY, SHAPE_TEXT -> BODY_LABEL;
            case HEADER_FOOTER -> HEADERS_FOOTERS_LABEL;
            case FOOTNOTE -> FOOTNOTES_LABEL;
            case ANNOTATION -> COMMENTS_LABEL;
            case DELETED -> TRACKED_CHANGES_LABEL;
            case FIELD_INSTRUCTION, HTML_TAG -> null;
        };
    }

    private record Rendered(String text, List<ReplacedRange> ranges) {
    }

    // A run's text as redaction leaves it: each planned replacement in place of its units, and the RTF-only text of
    // encapsulated HTML left out.
    private static Rendered rendered(final RtfAnalysis.Target target, final List<Planned> planned) {
        final Map<Integer, Planned> byFirst = new HashMap<>();
        for (final Planned p : planned) {
            byFirst.put(p.first(), p);
        }
        final StringBuilder sb = new StringBuilder();
        final List<ReplacedRange> ranges = new ArrayList<>();
        int u = 0;
        while (u < target.units.size()) {
            final Planned p = byFirst.get(u);
            if (p != null && rtfOnly(target, p)) {
                // A value wholly in RTF-only text is left out with that text.
                u = p.end() + 1;
            } else if (p != null) {
                final int start = sb.length();
                sb.append(p.span().getReplacement() == null ? "" : p.span().getReplacement());
                ranges.add(new ReplacedRange(start, sb.length()));
                u = p.end() + 1;
            } else {
                final RtfAnalysis.Unit unit = target.units.get(u);
                if (!unit.htmlOnly) {
                    sb.append(unit.text);
                }
                u++;
            }
        }
        return new Rendered(sb.toString(), ranges);
    }

    private static boolean rtfOnly(final RtfAnalysis.Target target, final Planned planned) {
        for (int u = planned.first(); u <= planned.end(); u++) {
            if (!target.units.get(u).htmlOnly) {
                return false;
            }
        }
        return true;
    }

    // Each kept property, read as body text of a document with this one's header, so its characters are decoded
    // with the document's code page and fonts, then redacted together, one "key: value" line each.
    private static void metadata(final List<RtfToken> tokens, final RtfAnalysis analysis,
                                 final Function<String, TextFilterResult> filter, final RedactedTextBuilder text) {
        text.ensure(RedactedTextBuilder.METADATA_LABEL);
        final List<int[]> info = analysis.groups("info");
        if (info.isEmpty()) {
            return;
        }
        final StringBuilder header = new StringBuilder();
        for (int t = 0; t < info.get(0)[0]; t++) {
            header.append(tokens.get(t).raw());
        }
        final List<String> lines = new ArrayList<>();
        for (final Map.Entry<String, String> property : PROPERTIES.entrySet()) {
            for (final int[] group : analysis.groups(property.getKey())) {
                final StringBuilder content = new StringBuilder();
                for (int t = analysis.destinationWord(group[0]) + 1; t < group[1]; t++) {
                    content.append(tokens.get(t).raw());
                }
                final String document = header + "{\\pard " + content + "\\par}}";
                final StringBuilder value = new StringBuilder();
                for (final RtfAnalysis.Target target : new RtfAnalysis(RtfTokenizer.tokenize(document)).targets) {
                    value.append(target.text());
                }
                if (!value.toString().isBlank()) {
                    lines.add(property.getValue() + ": " + RedactedTextBuilder.flatten(value.toString().strip()));
                }
            }
        }
        final String original = String.join(RedactedTextBuilder.LINE_SEPARATOR, lines);
        if (original.isEmpty()) {
            return;
        }
        final List<Span> found = detected(filter.apply(original), original);
        found.sort(Comparator.comparingInt(Span::getCharacterStart));
        final StringBuilder sb = new StringBuilder();
        final List<ReplacedRange> ranges = new ArrayList<>();
        int copiedTo = 0;
        for (final Span span : found) {
            if (span.getCharacterStart() < copiedTo) {
                continue;
            }
            sb.append(original, copiedTo, span.getCharacterStart());
            final int start = sb.length();
            sb.append(span.getReplacement() == null ? "" : span.getReplacement());
            ranges.add(new ReplacedRange(start, sb.length()));
            copiedTo = span.getCharacterEnd();
        }
        sb.append(original, copiedTo, original.length());
        text.line(RedactedTextBuilder.METADATA_LABEL, sb.toString(), ranges);
    }

    /**
     * Redacts a document.
     * @param input The RTF document.
     * @param filter Detects what to redact in a run of text.
     * @param word The {@code config.word} settings.
     * @return The redacted document and its spans.
     * @throws RtfFormatException when the document is malformed or exceeds a limit.
     */
    public static Result redact(final byte[] input, final Function<String, TextFilterResult> filter, final Word word) {
        final List<RtfToken> tokens = RtfTokenizer.tokenize(prepare(read(input), word));
        final RtfAnalysis analysis = new RtfAnalysis(tokens);
        final RtfEdits edits = new RtfEdits(tokens);
        final List<Span> spans = new ArrayList<>();
        for (int i = 0; i < analysis.targets.size(); i++) {
            final RtfAnalysis.Target target = analysis.targets.get(i);
            if (!scanned(target, word)) {
                continue;
            }
            final String text = target.text();
            final List<Span> found = detected(filter.apply(text), text);
            for (final Span span : replace(target, found, edits)) {
                span.setParagraphNumber(i);
                spans.add(span);
            }
        }
        return new Result(edits.write().getBytes(StandardCharsets.ISO_8859_1), spans);
    }

    /**
     * Detects what {@link #redact} would redact, without producing a document.
     * @param input The RTF document.
     * @param filter Detects what to redact in a run of text.
     * @param word The {@code config.word} settings.
     * @return The spans.
     */
    public static List<Span> detect(final byte[] input, final Function<String, TextFilterResult> filter, final Word word) {
        final RtfAnalysis analysis = new RtfAnalysis(RtfTokenizer.tokenize(prepare(read(input), word)));
        final List<Span> spans = new ArrayList<>();
        for (int i = 0; i < analysis.targets.size(); i++) {
            final RtfAnalysis.Target target = analysis.targets.get(i);
            if (!scanned(target, word)) {
                continue;
            }
            final String text = target.text();
            for (final Span span : detected(filter.apply(text), text)) {
                span.setParagraphNumber(i);
                spans.add(span);
            }
        }
        return spans;
    }

    /**
     * Applies spans, detected or added by a reviewer, by their paragraph number and offsets, after the
     * {@code config.word} settings, as redaction does.
     * @param input The RTF document.
     * @param spans The spans.
     * @param word The {@code config.word} settings.
     * @return The redacted document.
     */
    public static byte[] applySpans(final byte[] input, final List<Span> spans, final Word word) {
        final List<RtfToken> tokens = RtfTokenizer.tokenize(prepare(read(input), word));
        final RtfAnalysis analysis = new RtfAnalysis(tokens);
        final RtfEdits edits = new RtfEdits(tokens);
        final Map<Integer, List<Span>> byParagraph = new HashMap<>();
        for (final Span span : spans) {
            byParagraph.computeIfAbsent(span.getParagraphNumber(), k -> new ArrayList<>()).add(span);
        }
        for (final Map.Entry<Integer, List<Span>> entry : byParagraph.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < analysis.targets.size()) {
                replace(analysis.targets.get(entry.getKey()), entry.getValue(), edits);
            }
        }
        return edits.write().getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * The text of each run of text redaction reads, by paragraph number, after the {@code config.word} settings.
     * @param input The RTF document.
     * @param word The {@code config.word} settings.
     * @return The runs' text.
     */
    public static List<String> readParagraphs(final byte[] input, final Word word) {
        final RtfAnalysis analysis = new RtfAnalysis(RtfTokenizer.tokenize(prepare(read(input), word)));
        return analysis.targets.stream().map(RtfAnalysis.Target::text).toList();
    }

    static String read(final byte[] input) {
        return new String(input, StandardCharsets.ISO_8859_1);
    }

    private static boolean scanned(final RtfAnalysis.Target target, final Word word) {
        return target.kind != RtfAnalysis.Kind.HEADER_FOOTER || word.getHeadersFooters() == Word.RedactOrKeep.REDACT;
    }

    // The result's applied spans that fall within the text.
    private static List<Span> detected(final TextFilterResult result, final String text) {
        final List<Span> out = new ArrayList<>();
        if (result == null || result.getExplanation() == null || result.getExplanation().appliedSpans() == null) {
            return out;
        }
        for (final Span s : result.getExplanation().appliedSpans()) {
            if (s.getCharacterStart() >= 0 && s.getCharacterEnd() <= text.length() && s.getCharacterEnd() > s.getCharacterStart()) {
                final Span span = s.copy();
                span.setPattern(s.getPattern());
                span.setWindow(s.getWindow());
                out.add(span);
            }
        }
        return out;
    }

    // The spans to apply to a run, in order, each with the units it replaces: a span replaces every unit any of
    // its characters came from. A span that overlaps an earlier one is not applied.
    private static List<Planned> plan(final RtfAnalysis.Target target, final List<Span> spans) {
        final List<RtfAnalysis.Unit> units = target.units;
        // The unit each character of the run's text came from.
        final List<Integer> unitAt = new ArrayList<>();
        for (int u = 0; u < units.size(); u++) {
            for (int c = 0; c < units.get(u).text.length(); c++) {
                unitAt.add(u);
            }
        }
        final List<Span> sorted = new ArrayList<>(spans);
        sorted.sort(Comparator.comparingInt(Span::getCharacterStart)
                .thenComparing(Comparator.comparingInt(Span::getCharacterEnd).reversed()));
        final List<Planned> planned = new ArrayList<>();
        int last = 0;
        for (final Span span : sorted) {
            if (span.getCharacterStart() < last || span.getCharacterEnd() > unitAt.size()
                    || span.getCharacterEnd() <= span.getCharacterStart()) {
                continue;
            }
            planned.add(new Planned(span, unitAt.get(span.getCharacterStart()), unitAt.get(span.getCharacterEnd() - 1)));
            last = span.getCharacterEnd();
        }
        return planned;
    }

    // Replaces each span's text in the run, in place: the replacement goes before the span's first unit, and the
    // span's units are removed. Returns the spans applied.
    private static List<Span> replace(final RtfAnalysis.Target target, final List<Span> spans, final RtfEdits edits) {
        final List<Span> applied = new ArrayList<>();
        for (final Planned p : plan(target, spans)) {
            edits.insertText(target.units.get(p.first()).refs.get(0),
                    p.span().getReplacement() == null ? "" : p.span().getReplacement());
            for (int u = p.first(); u <= p.end(); u++) {
                edits.remove(target.units.get(u));
            }
            applied.add(p.span());
        }
        return applied;
    }

    static String prepare(final String rtf, final Word word) {

        final List<RtfToken> tokens = RtfTokenizer.tokenize(rtf);
        final RtfAnalysis analysis = new RtfAnalysis(tokens);
        final RtfEdits edits = new RtfEdits(tokens);

        final boolean removeComments = word.getComments() == Word.RemoveOrRedact.REMOVE;
        final boolean accept = word.getTrackedChanges() == Word.AcceptOrRedact.ACCEPT;
        final boolean removeHidden = word.getHiddenText() == Word.RemoveOrRedact.REMOVE;

        // Authorship, whatever the settings.
        for (final String dest : List.of("author", "operator", "company", "manager")) {
            for (final int[] group : analysis.groups(dest)) {
                replaceContent(analysis, edits, group, "");
            }
        }
        if (!removeComments) {
            for (final int[] group : analysis.groups("atnauthor")) {
                replaceContent(analysis, edits, group, NEUTRAL_AUTHOR);
            }
            for (final int[] group : analysis.groups("atnid")) {
                replaceContent(analysis, edits, group, "");
            }
        }
        if (accept) {
            removeGroups(analysis, edits, "revtbl");
        } else {
            for (final int[] table : analysis.groups("revtbl")) {
                for (int t = table[0] + 1; t < table[1]; t++) {
                    if (tokens.get(t).is(RtfToken.Kind.GROUP_START)) {
                        edits.removeTokens(t + 1, analysis.groupEnd[t] - 1);
                        edits.insertText(new RtfAnalysis.Ref(analysis.groupEnd[t], -1), NEUTRAL_AUTHOR + ";");
                        t = analysis.groupEnd[t];
                    }
                }
            }
        }

        if (word.isRemoveDocumentProperties()) {
            for (final String dest : List.of("title", "subject", "keywords", "doccomm", "category")) {
                for (final int[] group : analysis.groups(dest)) {
                    replaceContent(analysis, edits, group, "");
                }
            }
            removeGroups(analysis, edits, "userprops");
        }

        if (removeComments) {
            for (final String dest : List.of("atnid", "atnauthor", "annotation", "atrfstart", "atrfend", "atndate",
                    "atnparent", "atntime", "atnref")) {
                removeGroups(analysis, edits, dest);
            }
            removeWords(tokens, edits, Set.of("chatn"));
        }

        if (accept) {
            for (final RtfAnalysis.Target target : analysis.targets) {
                for (final RtfAnalysis.Unit unit : target.units) {
                    if (unit.deleted) {
                        edits.remove(unit);
                    }
                }
            }
            for (int t = 0; t < tokens.size(); t++) {
                if (analysis.tokenDeleted[t] && tokens.get(t).isWord("par")) {
                    edits.removeToken(t);
                }
            }
            removeWords(tokens, edits, REVISION_WORDS);
            for (final String dest : List.of("oldcprops", "oldpprops", "oldsprops", "oldtprops")) {
                removeGroups(analysis, edits, dest);
            }
        }

        if (removeHidden) {
            for (final RtfAnalysis.Target target : analysis.targets) {
                for (final RtfAnalysis.Unit unit : target.units) {
                    if (unit.hidden) {
                        edits.remove(unit);
                    }
                }
            }
            for (int t = 0; t < tokens.size(); t++) {
                if (analysis.tokenHidden[t] && tokens.get(t).isWord("par")) {
                    edits.removeToken(t);
                }
            }
        }

        if (word.getUninspectableObjects() == Word.RemoveOrKeep.REMOVE) {
            removeGroups(analysis, edits, "object");
        }

        return edits.write();

    }

    // Replaces a destination group's content, keeping its braces and destination word.
    private static void replaceContent(final RtfAnalysis analysis, final RtfEdits edits, final int[] group, final String text) {
        final int word = analysis.destinationWord(group[0]);
        edits.removeTokens(word + 1, group[1] - 1);
        if (!text.isEmpty()) {
            edits.insertText(new RtfAnalysis.Ref(group[1], -1), text);
        }
    }

    private static void removeGroups(final RtfAnalysis analysis, final RtfEdits edits, final String dest) {
        for (final int[] group : analysis.groups(dest)) {
            edits.removeTokens(group[0], group[1]);
        }
    }

    private static void removeWords(final List<RtfToken> tokens, final RtfEdits edits, final Set<String> words) {
        for (int t = 0; t < tokens.size(); t++) {
            final RtfToken token = tokens.get(t);
            if (token.is(RtfToken.Kind.WORD) && words.contains(token.word())) {
                edits.removeToken(t);
            }
        }
    }

}
