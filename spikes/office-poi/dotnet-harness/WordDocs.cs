/*
 * Copyright 2026 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

using System.Text;
using DocumentFormat.OpenXml;
using DocumentFormat.OpenXml.Packaging;
using DocumentFormat.OpenXml.Wordprocessing;
using A = DocumentFormat.OpenXml.Drawing;

namespace Phileas.Tests
{
    /// <summary>
    /// Minimal Open XML SDK helpers for the Word tests: create .docx fixtures and read their text
    /// back, replacing the previous Xceed-based helpers (no license required).
    /// </summary>
    internal static class WordDocs
    {
        /// <summary>Creates a .docx with one body paragraph per supplied string.</summary>
        public static void Create(string path, params string[] paragraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            var body = new Body();
            foreach (string text in paragraphs)
            {
                body.AppendChild(Para(text));
            }
            main.Document = new Document(body);
        }

        /// <summary>Creates a .docx with a (default/odd) header and footer plus body paragraphs.</summary>
        public static void CreateWithHeaderFooter(string path, string headerText, string footerText, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            main.Document = new Document(new Body());
            Body body = main.Document.Body!;

            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }

            HeaderPart headerPart = main.AddNewPart<HeaderPart>();
            headerPart.Header = new Header(Para(headerText));
            string headerId = main.GetIdOfPart(headerPart);

            FooterPart footerPart = main.AddNewPart<FooterPart>();
            footerPart.Footer = new Footer(Para(footerText));
            string footerId = main.GetIdOfPart(footerPart);

            body.AppendChild(new SectionProperties(
                new HeaderReference { Type = HeaderFooterValues.Default, Id = headerId },
                new FooterReference { Type = HeaderFooterValues.Default, Id = footerId }));
        }

        /// <summary>The body paragraphs' text, in order.</summary>
        public static string[] BodyParagraphs(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            return doc.MainDocumentPart!.Document!.Body!
                .Elements<Paragraph>()
                .Select(p => p.InnerText)
                .ToArray();
        }

        /// <summary>All body text concatenated and trimmed (matches the old DocX helper).</summary>
        public static string AllBodyText(string path) => string.Concat(BodyParagraphs(path)).Trim();

        /// <summary>All header parts' text concatenated.</summary>
        public static string HeadersText(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            return string.Concat(doc.MainDocumentPart!.HeaderParts
                .SelectMany(h => h.Header!.Descendants<Paragraph>())
                .Select(p => p.InnerText));
        }

        /// <summary>All footer parts' text concatenated.</summary>
        public static string FootersText(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            return string.Concat(doc.MainDocumentPart!.FooterParts
                .SelectMany(f => f.Footer!.Descendants<Paragraph>())
                .Select(p => p.InnerText));
        }

        private static Paragraph Para(string text) =>
            new(new Run(new Text(text) { Space = SpaceProcessingModeValues.Preserve }));

        // --- Tracked changes (deletions) ---------------------------------------------

        /// <summary>
        /// Creates a .docx whose body has a tracked <b>deletion</b> (<c>w:del</c> containing a
        /// <c>w:delText</c>) carrying <paramref name="deletedText"/>, plus optional visible body paragraphs.
        /// </summary>
        public static void CreateWithTrackedDeletion(string path, string deletedText, params string[] bodyParagraphs)
        {
            string deletion =
                "<w:p><w:del w:id=\"1\" w:author=\"Reviewer\" w:date=\"2024-01-01T00:00:00Z\">" +
                $"<w:r><w:delText xml:space=\"preserve\">{Escape(deletedText)}</w:delText></w:r></w:del></w:p>";
            CreateRaw(path, string.Concat(bodyParagraphs.Select(ParaXml)) + deletion);
        }

        /// <summary>The text of every tracked-deletion element (<c>w:delText</c>) in the body, in order.</summary>
        public static string[] DeletedTexts(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            return doc.MainDocumentPart!.Document!.Body!
                .Descendants<DeletedText>()
                .Select(d => d.Text)
                .ToArray();
        }

        // --- Hyperlinks ---------------------------------------------------------------

        /// <summary>
        /// Creates a .docx with one body paragraph holding a hyperlink whose visible text is
        /// <paramref name="displayText"/> and whose external target (stored as a relationship, referenced
        /// by r:id) is <paramref name="targetUri"/>, plus optional extra body paragraphs.
        /// </summary>
        // --- Field codes -------------------------------------------------------------

        /// <summary>
        /// Creates a .docx with a simple field (<c>w:fldSimple</c>) whose instruction is
        /// <paramref name="instruction"/> and whose cached (visible) result is <paramref name="resultText"/>.
        /// </summary>
        public static void CreateWithSimpleField(string path, string instruction, string resultText, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            var body = new Body();
            body.AppendChild(new Paragraph(
                new SimpleField(new Run(new Text(resultText) { Space = SpaceProcessingModeValues.Preserve }))
                { Instruction = instruction }));
            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }
            main.Document = new Document(body);
        }

        /// <summary>
        /// Creates a .docx with a complex field - begin / <c>w:instrText</c> run(s) / separate / result / end.
        /// Pass more than one <paramref name="instructionRuns"/> entry to split the instruction across runs.
        /// </summary>
        public static void CreateWithComplexField(string path, string[] instructionRuns, string resultText, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            var para = new Paragraph();
            para.AppendChild(new Run(new FieldChar { FieldCharType = FieldCharValues.Begin }));
            foreach (string t in instructionRuns)
            {
                para.AppendChild(new Run(new FieldCode(t) { Space = SpaceProcessingModeValues.Preserve }));
            }
            para.AppendChild(new Run(new FieldChar { FieldCharType = FieldCharValues.Separate }));
            para.AppendChild(new Run(new Text(resultText) { Space = SpaceProcessingModeValues.Preserve }));
            para.AppendChild(new Run(new FieldChar { FieldCharType = FieldCharValues.End }));
            var body = new Body(para);
            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }
            main.Document = new Document(body);
        }

        public static void CreateWithHyperlink(string path, string displayText, string targetUri, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            var body = new Body();

            HyperlinkRelationship rel = main.AddHyperlinkRelationship(new Uri(targetUri), isExternal: true);
            body.AppendChild(new Paragraph(new Hyperlink(
                new Run(new Text(displayText) { Space = SpaceProcessingModeValues.Preserve }))
            { Id = rel.Id }));

            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }
            main.Document = new Document(body);
        }

        /// <summary>
        /// Creates a .docx whose <b>header</b> holds a hyperlink (external target
        /// <paramref name="targetUri"/>) - exercises hyperlink handling on a non-main part.
        /// </summary>
        public static void CreateWithHyperlinkInHeader(string path, string displayText, string targetUri, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            main.Document = new Document(new Body());
            Body body = main.Document.Body!;
            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }

            HeaderPart headerPart = main.AddNewPart<HeaderPart>();
            HyperlinkRelationship rel = headerPart.AddHyperlinkRelationship(new Uri(targetUri), isExternal: true);
            headerPart.Header = new Header(new Paragraph(new Hyperlink(
                new Run(new Text(displayText) { Space = SpaceProcessingModeValues.Preserve }))
            { Id = rel.Id }));

            body.AppendChild(new SectionProperties(
                new HeaderReference { Type = HeaderFooterValues.Default, Id = main.GetIdOfPart(headerPart) }));
        }

        /// <summary>Every external hyperlink relationship target across all parts of the package, in order.</summary>
        public static string[] HyperlinkTargets(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            var seen = new HashSet<string>();
            var targets = new List<string>();
            CollectHyperlinkTargets(doc.MainDocumentPart!, seen, targets);
            return targets.ToArray();
        }

        /// <summary>
        /// True if every w:hyperlink element (across all parts) that carries an r:id still resolves to a
        /// hyperlink relationship on its owning part - i.e. redaction left no dangling reference that
        /// would make Word report the document as corrupt.
        /// </summary>
        public static bool HyperlinkIdsAllResolve(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            var seen = new HashSet<string>();
            return HyperlinkIdsResolve(doc.MainDocumentPart!, seen);
        }

        private static bool HyperlinkIdsResolve(OpenXmlPart part, HashSet<string> seen)
        {
            if (!seen.Add(part.Uri.OriginalString))
            {
                return true;
            }
            OpenXmlElement? root = null;
            try { root = part.RootElement; } catch { /* unparseable - nothing to check here */ }
            if (root is not null)
            {
                var ids = part.HyperlinkRelationships.Select(r => r.Id).ToHashSet();
                foreach (Hyperlink h in root.Descendants<Hyperlink>())
                {
                    if (h.Id?.Value is string id && !ids.Contains(id))
                    {
                        return false;
                    }
                }
            }
            return part.Parts.All(child => HyperlinkIdsResolve(child.OpenXmlPart, seen));
        }

        private static void CollectHyperlinkTargets(OpenXmlPart part, HashSet<string> seen, List<string> targets)
        {
            if (!seen.Add(part.Uri.OriginalString))
            {
                return;
            }
            foreach (HyperlinkRelationship rel in part.HyperlinkRelationships.Where(r => r.IsExternal))
            {
                targets.Add(rel.Uri.ToString());
            }
            foreach (IdPartPair child in part.Parts)
            {
                CollectHyperlinkTargets(child.OpenXmlPart, seen, targets);
            }
        }

        // --- Comments -----------------------------------------------------------------

        /// <summary>Creates a .docx with one Word comment (one paragraph) plus the given body paragraphs.</summary>
        public static void CreateWithComment(string path, string commentText, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            main.Document = new Document(new Body());
            Body body = main.Document.Body!;
            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }

            WordprocessingCommentsPart commentsPart = main.AddNewPart<WordprocessingCommentsPart>();
            commentsPart.Comments = new Comments(
                new Comment(Para(commentText)) { Id = "1", Author = "Reviewer", Initials = "R" });
        }

        /// <summary>The text of each Word comment, in order (empty if there is no comments part).</summary>
        public static string[] CommentTexts(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            WordprocessingCommentsPart? part = doc.MainDocumentPart!.WordprocessingCommentsPart;
            return part?.Comments is null
                ? Array.Empty<string>()
                : part.Comments.Elements<Comment>().Select(c => c.InnerText).ToArray();
        }

        /// <summary>Whether the document still has a comments part with at least one comment.</summary>
        public static bool HasComments(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            WordprocessingCommentsPart? part = doc.MainDocumentPart!.WordprocessingCommentsPart;
            return part?.Comments?.Elements<Comment>().Any() == true;
        }

        // --- DrawingML text: shapes / SmartArt / charts -------------------------------

        /// <summary>A DrawingML paragraph (&lt;a:p&gt;) with one run per supplied string.</summary>
        public static string AParagraph(params string[] runs) =>
            "<a:p>" + string.Concat(runs.Select(r => $"<a:r><a:t xml:space=\"preserve\">{Escape(r)}</a:t></a:r>")) + "</a:p>";

        /// <summary>
        /// Creates a .docx that embeds a full chart (title text + a series with cached series-name,
        /// category, and value) referenced from the body by an inline drawing - so it exercises both the
        /// chart's DrawingML text (<c>&lt;a:t&gt;</c>) and its cached values (<c>&lt;c:v&gt;</c>).
        /// </summary>
        public static void CreateWithChartData(string path, string title, string seriesName, string category, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();

            ChartPart chartPart = main.AddNewPart<ChartPart>();
            WritePart(chartPart, SpreadsheetTestHelper.ChartSpaceXml(title, seriesName, category));
            string relId = main.GetIdOfPart(chartPart);

            string bodyXml = string.Concat(bodyParagraphs.Select(ParaXml)) +
                "<w:p><w:r><w:drawing>" +
                "<wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">" +
                "<wp:extent cx=\"5000000\" cy=\"3000000\"/>" +
                "<wp:docPr id=\"1\" name=\"Chart 1\"/>" +
                "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">" +
                $"<c:chart xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" r:id=\"{relId}\"/>" +
                "</a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>";
            string xml = $"<w:document {DocNamespaces}><w:body>{bodyXml}</w:body></w:document>";
            using Stream stream = main.GetStream(FileMode.Create, FileAccess.Write);
            using var writer = new StreamWriter(stream, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
            writer.Write(xml);
        }

        /// <summary>
        /// Creates a .docx with a native chart whose <b>embedded source workbook</b> (an <c>EmbeddedPackagePart</c>
        /// referenced by <c>c:externalData</c>) is <paramref name="workbookBytes"/>. The plotted cache holds
        /// <paramref name="category"/>/42, but the embedded workbook can carry more (e.g. an unplotted PII column).
        /// </summary>
        public static void CreateWithChartEmbeddedWorkbook(string path, byte[] workbookBytes, string seriesName, string category)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();

            ChartPart chartPart = main.AddNewPart<ChartPart>();
            EmbeddedPackagePart pkg = chartPart.AddNewPart<EmbeddedPackagePart>(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "rIdEmb");
            using (Stream es = pkg.GetStream(FileMode.Create, FileAccess.Write)) { es.Write(workbookBytes, 0, workbookBytes.Length); }

            string chartXml =
                "<c:chartSpace xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" " +
                "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" " +
                "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                "<c:chart><c:plotArea><c:layout/><c:barChart><c:barDir val=\"col\"/>" +
                "<c:ser><c:idx val=\"0\"/><c:order val=\"0\"/>" +
                $"<c:tx><c:strRef><c:f>Sheet1!$B$1</c:f><c:strCache><c:ptCount val=\"1\"/><c:pt idx=\"0\"><c:v>{seriesName}</c:v></c:pt></c:strCache></c:strRef></c:tx>" +
                $"<c:cat><c:strRef><c:f>Sheet1!$A$2</c:f><c:strCache><c:ptCount val=\"1\"/><c:pt idx=\"0\"><c:v>{category}</c:v></c:pt></c:strCache></c:strRef></c:cat>" +
                "<c:val><c:numRef><c:f>Sheet1!$B$2</c:f><c:numCache><c:ptCount val=\"1\"/><c:pt idx=\"0\"><c:v>42</c:v></c:pt></c:numCache></c:numRef></c:val>" +
                "</c:ser></c:barChart></c:plotArea></c:chart>" +
                "<c:externalData r:id=\"rIdEmb\"><c:autoUpdate val=\"0\"/></c:externalData></c:chartSpace>";
            WritePart(chartPart, chartXml);
            string relId = main.GetIdOfPart(chartPart);

            string bodyXml =
                "<w:p><w:r><w:drawing><wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">" +
                "<wp:extent cx=\"5000000\" cy=\"3000000\"/><wp:docPr id=\"1\" name=\"Chart 1\"/>" +
                "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">" +
                $"<c:chart xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" r:id=\"{relId}\"/>" +
                "</a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>";
            string xml = $"<w:document {DocNamespaces}><w:body>{bodyXml}</w:body></w:document>";
            using Stream stream = main.GetStream(FileMode.Create, FileAccess.Write);
            using var writer = new StreamWriter(stream, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
            writer.Write(xml);
        }

        /// <summary>True if the first chart part still has an embedded package (source workbook).</summary>
        public static bool HasChartEmbeddedWorkbook(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            ChartPart? cp = doc.MainDocumentPart!.GetPartsOfType<ChartPart>().FirstOrDefault();
            return cp?.GetPartsOfType<EmbeddedPackagePart>().Any() == true;
        }

        /// <summary>All cell text of the embedded source workbook behind the first chart part (empty if none).</summary>
        public static string EmbeddedChartWorkbookAllText(string path)
        {
            byte[]? bytes;
            using (WordprocessingDocument doc = WordprocessingDocument.Open(path, false))
            {
                ChartPart? cp = doc.MainDocumentPart!.GetPartsOfType<ChartPart>().FirstOrDefault();
                EmbeddedPackagePart? pkg = cp?.GetPartsOfType<EmbeddedPackagePart>().FirstOrDefault();
                if (pkg is null) { return string.Empty; }
                using Stream s = pkg.GetStream(FileMode.Open, FileAccess.Read);
                using var ms = new MemoryStream();
                s.CopyTo(ms);
                bytes = ms.ToArray();
            }
            string tmp = Path.Combine(Path.GetTempPath(), "emb-" + Guid.NewGuid().ToString("N") + ".xlsx");
            try { File.WriteAllBytes(tmp, bytes); return SpreadsheetTestHelper.AllText(tmp); }
            finally { try { File.Delete(tmp); } catch { /* best effort */ } }
        }

        // --- Embedded objects (Insert > Object) --------------------------------------

        /// <summary>Creates a .docx with an embedded package (an Office document) of the given content type.</summary>
        public static void CreateWithEmbeddedPackage(string path, byte[] bytes, string contentType, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            var body = new Body();
            foreach (string t in bodyParagraphs) { body.AppendChild(Para(t)); }
            main.Document = new Document(body);
            EmbeddedPackagePart pkg = main.AddNewPart<EmbeddedPackagePart>(contentType, "rIdEmb");
            using Stream s = pkg.GetStream(FileMode.Create, FileAccess.Write);
            s.Write(bytes, 0, bytes.Length);
        }

        /// <summary>
        /// Creates a .docx with an embedded object Philter Desktop can't inspect - modeled as an embedded
        /// package with a non-Word/Excel content type (e.g. PowerPoint), which the redactor treats as opaque.
        /// </summary>
        public static void CreateWithEmbeddedOleObject(string path, byte[] bytes, params string[] bodyParagraphs) =>
            CreateWithEmbeddedPackage(path, bytes, "application/vnd.openxmlformats-officedocument.presentationml.presentation", bodyParagraphs);

        /// <summary>True if the document still carries any embedded package or OLE object.</summary>
        public static bool HasAnyEmbeddedObject(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            MainDocumentPart main = doc.MainDocumentPart!;
            return main.GetPartsOfType<EmbeddedPackagePart>().Any() || main.GetPartsOfType<EmbeddedObjectPart>().Any();
        }

        private static byte[]? FirstEmbeddedPackageBytes(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            EmbeddedPackagePart? pkg = doc.MainDocumentPart!.GetPartsOfType<EmbeddedPackagePart>().FirstOrDefault();
            if (pkg is null) { return null; }
            using Stream s = pkg.GetStream(FileMode.Open, FileAccess.Read);
            using var ms = new MemoryStream();
            s.CopyTo(ms);
            return ms.ToArray();
        }

        /// <summary>All cell text of the first embedded package, read as an .xlsx (empty if none).</summary>
        public static string EmbeddedPackageAllTextAsXlsx(string path)
        {
            byte[]? bytes = FirstEmbeddedPackageBytes(path);
            if (bytes is null) { return string.Empty; }
            string tmp = Path.Combine(Path.GetTempPath(), "embpkg-" + Guid.NewGuid().ToString("N") + ".xlsx");
            try { File.WriteAllBytes(tmp, bytes); return SpreadsheetTestHelper.AllText(tmp); }
            finally { try { File.Delete(tmp); } catch { /* best effort */ } }
        }

        /// <summary>All body text of the first embedded package, read as a .docx (empty if none).</summary>
        public static string EmbeddedPackageAllTextAsDocx(string path)
        {
            byte[]? bytes = FirstEmbeddedPackageBytes(path);
            if (bytes is null) { return string.Empty; }
            string tmp = Path.Combine(Path.GetTempPath(), "embpkg-" + Guid.NewGuid().ToString("N") + ".docx");
            try { File.WriteAllBytes(tmp, bytes); return AllBodyText(tmp); }
            finally { try { File.Delete(tmp); } catch { /* best effort */ } }
        }

        /// <summary>Creates a .docx with a chart part whose title rich-text is the supplied &lt;a:p&gt; XML.</summary>
        public static void CreateWithChart(string path, string titleAParagraphXml, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            main.Document = new Document(new Body());
            Body body = main.Document.Body!;
            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }

            ChartPart part = main.AddNewPart<ChartPart>();
            string xml =
                "<c:chartSpace xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" " +
                "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">" +
                $"<c:chart><c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/>{titleAParagraphXml}</c:rich></c:tx></c:title>" +
                "<c:plotArea><c:layout/></c:plotArea></c:chart></c:chartSpace>";
            WritePart(part, xml);
        }

        /// <summary>Creates a .docx with a SmartArt data part whose first node text is the supplied &lt;a:p&gt; XML.</summary>
        public static void CreateWithSmartArt(string path, string nodeAParagraphXml, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            main.Document = new Document(new Body());
            Body body = main.Document.Body!;
            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }

            DiagramDataPart part = main.AddNewPart<DiagramDataPart>();
            string xml =
                "<dgm:dataModel xmlns:dgm=\"http://schemas.openxmlformats.org/drawingml/2006/diagram\" " +
                "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">" +
                $"<dgm:ptLst><dgm:pt dgm:modelId=\"1\"><dgm:t><a:bodyPr/><a:lstStyle/>{nodeAParagraphXml}</dgm:t></dgm:pt></dgm:ptLst>" +
                "</dgm:dataModel>";
            WritePart(part, xml);
        }

        /// <summary>All DrawingML text (&lt;a:t&gt;) across every part of the package, concatenated.</summary>
        public static string AllDrawingText(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            var seen = new HashSet<string>();
            var sb = new StringBuilder();
            CollectDrawingText(doc.MainDocumentPart!, seen, sb);
            return sb.ToString();
        }

        private static void CollectDrawingText(OpenXmlPart part, HashSet<string> seen, StringBuilder sb)
        {
            if (!seen.Add(part.Uri.OriginalString))
            {
                return;
            }
            OpenXmlElement? root = null;
            try { root = part.RootElement; } catch { /* unparseable part - skip */ }
            if (root is not null)
            {
                foreach (A.Text t in root.Descendants<A.Text>())
                {
                    sb.Append(t.Text).Append('\n');
                }
            }
            foreach (IdPartPair child in part.Parts)
            {
                CollectDrawingText(child.OpenXmlPart, seen, sb);
            }
        }

        private static void WritePart(OpenXmlPart part, string xml)
        {
            using Stream s = part.GetStream(FileMode.Create, FileAccess.Write);
            using var w = new StreamWriter(s, new UTF8Encoding(false));
            w.Write(xml);
        }

        // --- Footnotes / endnotes -----------------------------------------------------

        /// <summary>Creates a .docx with one footnote (and optional endnote) plus body paragraphs.</summary>
        public static void CreateWithNotes(string path, string? footnoteText, string? endnoteText, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            main.Document = new Document(new Body());
            Body body = main.Document.Body!;
            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }

            if (footnoteText is not null)
            {
                FootnotesPart part = main.AddNewPart<FootnotesPart>();
                part.Footnotes = new Footnotes(
                    new Footnote(new Paragraph()) { Type = FootnoteEndnoteValues.Separator, Id = -1 },
                    new Footnote(Para(footnoteText)) { Id = 1 });
            }

            if (endnoteText is not null)
            {
                EndnotesPart part = main.AddNewPart<EndnotesPart>();
                part.Endnotes = new Endnotes(
                    new Endnote(new Paragraph()) { Type = FootnoteEndnoteValues.Separator, Id = -1 },
                    new Endnote(Para(endnoteText)) { Id = 1 });
            }
        }

        /// <summary>Creates a .docx with several footnotes and/or endnotes (one paragraph each).</summary>
        public static void CreateWithMultipleNotes(string path, string[]? footnotes, string[]? endnotes, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            main.Document = new Document(new Body());
            Body body = main.Document.Body!;
            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }

            if (footnotes is { Length: > 0 })
            {
                var elems = new List<OpenXmlElement> { new Footnote(new Paragraph()) { Type = FootnoteEndnoteValues.Separator, Id = -1 } };
                for (int i = 0; i < footnotes.Length; i++)
                {
                    elems.Add(new Footnote(Para(footnotes[i])) { Id = i + 1 });
                }
                main.AddNewPart<FootnotesPart>().Footnotes = new Footnotes(elems.ToArray());
            }

            if (endnotes is { Length: > 0 })
            {
                var elems = new List<OpenXmlElement> { new Endnote(new Paragraph()) { Type = FootnoteEndnoteValues.Separator, Id = -1 } };
                for (int i = 0; i < endnotes.Length; i++)
                {
                    elems.Add(new Endnote(Para(endnotes[i])) { Id = i + 1 });
                }
                main.AddNewPart<EndnotesPart>().Endnotes = new Endnotes(elems.ToArray());
            }
        }

        /// <summary>Creates a .docx with a single footnote that has multiple paragraphs.</summary>
        public static void CreateWithMultiParagraphFootnote(string path, string[] footnoteParagraphs, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            main.Document = new Document(new Body());
            Body body = main.Document.Body!;
            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }

            var footnote = new Footnote { Id = 1 };
            foreach (string p in footnoteParagraphs)
            {
                footnote.AppendChild(Para(p));
            }
            main.AddNewPart<FootnotesPart>().Footnotes = new Footnotes(
                new Footnote(new Paragraph()) { Type = FootnoteEndnoteValues.Separator, Id = -1 }, footnote);
        }

        /// <summary>Creates a .docx whose footnote #1 is built from raw WordprocessingML (for drawings/text boxes).</summary>
        public static void CreateWithRawFootnote(string path, string footnoteInnerXml, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            main.Document = new Document(new Body());
            Body body = main.Document.Body!;
            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }

            FootnotesPart part = main.AddNewPart<FootnotesPart>();
            string xml = $"<w:footnotes {DocNamespaces}>" +
                         "<w:footnote w:type=\"separator\" w:id=\"-1\"><w:p/></w:footnote>" +
                         $"<w:footnote w:id=\"1\">{footnoteInnerXml}</w:footnote></w:footnotes>";
            using Stream s = part.GetStream(FileMode.Create, FileAccess.Write);
            using var w = new StreamWriter(s, new UTF8Encoding(false));
            w.Write(xml);
        }

        /// <summary>Creates a .docx with a header, footer, one footnote, and one endnote - the full part set.</summary>
        public static void CreateWithHeaderFooterAndNotes(string path, string headerText, string footerText, string footnoteText, string endnoteText, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            main.Document = new Document(new Body());
            Body body = main.Document.Body!;
            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }

            HeaderPart headerPart = main.AddNewPart<HeaderPart>();
            headerPart.Header = new Header(Para(headerText));
            FooterPart footerPart = main.AddNewPart<FooterPart>();
            footerPart.Footer = new Footer(Para(footerText));

            main.AddNewPart<FootnotesPart>().Footnotes = new Footnotes(
                new Footnote(new Paragraph()) { Type = FootnoteEndnoteValues.Separator, Id = -1 },
                new Footnote(Para(footnoteText)) { Id = 1 });
            main.AddNewPart<EndnotesPart>().Endnotes = new Endnotes(
                new Endnote(new Paragraph()) { Type = FootnoteEndnoteValues.Separator, Id = -1 },
                new Endnote(Para(endnoteText)) { Id = 1 });

            body.AppendChild(new SectionProperties(
                new HeaderReference { Type = HeaderFooterValues.Default, Id = main.GetIdOfPart(headerPart) },
                new FooterReference { Type = HeaderFooterValues.Default, Id = main.GetIdOfPart(footerPart) }));
        }

        /// <summary>Text of each footnote (non-empty), in order.</summary>
        public static string[] FootnoteTexts(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            return doc.MainDocumentPart!.FootnotesPart?.Footnotes?
                .Elements<Footnote>().Select(f => f.InnerText).Where(t => t.Length > 0).ToArray()
                ?? Array.Empty<string>();
        }

        /// <summary>Text of each endnote (non-empty), in order.</summary>
        public static string[] EndnoteTexts(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            return doc.MainDocumentPart!.EndnotesPart?.Endnotes?
                .Elements<Endnote>().Select(e => e.InnerText).Where(t => t.Length > 0).ToArray()
                ?? Array.Empty<string>();
        }

        // --- Comment authors / people.xml ---------------------------------------------

        /// <summary>
        /// Creates a .docx with comments carrying the given (author, text) pairs and a matching
        /// word/people.xml listing each distinct author - i.e. the reviewer-identity metadata.
        /// </summary>
        public static void CreateWithAuthoredComments(string path, (string Author, string Text)[] comments, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            main.Document = new Document(new Body());
            Body body = main.Document.Body!;
            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }

            var root = new Comments();
            for (int i = 0; i < comments.Length; i++)
            {
                (string author, string text) = comments[i];
                string initials = author.Length > 0 ? author[..1] : "X";
                root.AppendChild(new Comment(Para(text)) { Id = (i + 1).ToString(), Author = author, Initials = initials });
            }
            main.AddNewPart<WordprocessingCommentsPart>().Comments = root;

            WordprocessingPeoplePart people = main.AddNewPart<WordprocessingPeoplePart>();
            string persons = string.Concat(comments.Select(c => c.Author).Distinct().Select(a =>
                $"<w15:person w15:author=\"{Escape(a)}\"><w15:presenceInfo w15:providerId=\"None\" w15:userId=\"{Escape(a)}\"/></w15:person>"));
            using (Stream s = people.GetStream(FileMode.Create, FileAccess.Write))
            using (var w = new StreamWriter(s, new UTF8Encoding(false)))
            {
                w.Write("<w15:people xmlns:w15=\"http://schemas.microsoft.com/office/word/2015/wml\">" + persons + "</w15:people>");
            }
        }

        /// <summary>The w:author value of each comment, in order.</summary>
        public static string[] CommentAuthors(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            WordprocessingCommentsPart? part = doc.MainDocumentPart!.WordprocessingCommentsPart;
            return part?.Comments is null
                ? Array.Empty<string>()
                : part.Comments.Elements<Comment>().Select(c => c.Author?.Value ?? string.Empty).ToArray();
        }

        /// <summary>The w:initials value of each comment, in order.</summary>
        public static string[] CommentInitials(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            WordprocessingCommentsPart? part = doc.MainDocumentPart!.WordprocessingCommentsPart;
            return part?.Comments is null
                ? Array.Empty<string>()
                : part.Comments.Elements<Comment>().Select(c => c.Initials?.Value ?? string.Empty).ToArray();
        }

        /// <summary>Whether the document still has a word/people.xml part.</summary>
        public static bool HasPeoplePart(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            return doc.MainDocumentPart!.GetPartsOfType<WordprocessingPeoplePart>().Any();
        }

        // --- Threaded comments --------------------------------------------------------

        private const string ThreadedRelType = "http://schemas.microsoft.com/office/2018/08/relationships/threadedComment";
        private const string ThreadedContentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.threadedcomments+xml";

        /// <summary>
        /// Creates a .docx with a classic comment (<paramref name="classicText"/>) and the modern
        /// threaded-comment duplicate store (<paramref name="threadedText"/> in word/threadedComments.xml).
        /// </summary>
        public static void CreateWithThreadedComment(string path, string threadedText, string classicText, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            main.Document = new Document(new Body());
            Body body = main.Document.Body!;
            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }

            WordprocessingCommentsPart commentsPart = main.AddNewPart<WordprocessingCommentsPart>();
            commentsPart.Comments = new Comments(
                new Comment(Para(classicText)) { Id = "1", Author = "Reviewer", Initials = "R" });

            ExtendedPart threaded = main.AddExtendedPart(ThreadedRelType, ThreadedContentType, "xml");
            string xml =
                "<w15:threadedComments xmlns:w15=\"http://schemas.microsoft.com/office/word/2015/wml\">" +
                "<w15:threadedComment w15:id=\"1\" w15:paraId=\"00000001\" w15:dateUtc=\"2024-01-01T00:00:00Z\">" +
                $"<w15:text>{Escape(threadedText)}</w15:text></w15:threadedComment></w15:threadedComments>";
            using Stream s = threaded.GetStream(FileMode.Create, FileAccess.Write);
            using var w = new StreamWriter(s, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
            w.Write(xml);
        }

        /// <summary>
        /// Creates a .docx that models a real Word comment thread: each message in
        /// <paramref name="messages"/> becomes both a classic comment (word/comments.xml) and a
        /// threaded comment (word/threadedComments.xml), with the companion commentsExtended part.
        /// </summary>
        public static void CreateWithThreadedThread(string path, string[] messages, params string[] bodyParagraphs)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            main.Document = new Document(new Body());
            Body body = main.Document.Body!;
            foreach (string text in bodyParagraphs)
            {
                body.AppendChild(Para(text));
            }

            var comments = new Comments();
            for (int i = 0; i < messages.Length; i++)
            {
                comments.AppendChild(new Comment(Para(messages[i])) { Id = (i + 1).ToString(), Author = "Reviewer", Initials = "R" });
            }
            main.AddNewPart<WordprocessingCommentsPart>().Comments = comments;

            // commentsExtended (threading metadata, no text) - present in real threaded docs.
            WordprocessingCommentsExPart exPart = main.AddNewPart<WordprocessingCommentsExPart>();
            using (Stream es = exPart.GetStream(FileMode.Create, FileAccess.Write))
            using (var ew = new StreamWriter(es, new UTF8Encoding(false)))
            {
                ew.Write("<w15:commentsEx xmlns:w15=\"http://schemas.microsoft.com/office/word/2015/wml\">" +
                         string.Concat(Enumerable.Range(0, messages.Length).Select(i =>
                             $"<w15:commentEx w15:paraId=\"{i + 1:00000000}\" w15:done=\"0\"/>")) +
                         "</w15:commentsEx>");
            }

            // threadedComments duplicate (the PII copy this issue is about).
            ExtendedPart threaded = main.AddExtendedPart(ThreadedRelType, ThreadedContentType, "xml");
            string threads = string.Concat(Enumerable.Range(0, messages.Length).Select(i =>
                $"<w15:threadedComment w15:id=\"{i + 1}\" w15:paraId=\"{i + 1:00000000}\" w15:dateUtc=\"2024-01-01T00:00:00Z\"" +
                (i == 0 ? "" : " w15:parentId=\"1\"") + $"><w15:text>{Escape(messages[i])}</w15:text></w15:threadedComment>"));
            using (Stream s = threaded.GetStream(FileMode.Create, FileAccess.Write))
            using (var w = new StreamWriter(s, new UTF8Encoding(false)))
            {
                w.Write("<w15:threadedComments xmlns:w15=\"http://schemas.microsoft.com/office/word/2015/wml\">" + threads + "</w15:threadedComments>");
            }
        }

        /// <summary>Whether the document still has a Word threaded-comments part.</summary>
        public static bool HasThreadedComments(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            return ThreadedPart(doc.MainDocumentPart!) is not null;
        }

        /// <summary>Whether the document still has the commentsExtended / commentsIds companion parts.</summary>
        public static bool HasCommentCompanionParts(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            MainDocumentPart main = doc.MainDocumentPart!;
            return main.GetPartsOfType<WordprocessingCommentsExPart>().Any()
                || main.GetPartsOfType<WordprocessingCommentsIdsPart>().Any();
        }

        /// <summary>True if any part in the package contains <paramref name="value"/> (strong leak check).</summary>
        public static bool AnyPartContains(string path, string value)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            var seen = new HashSet<string>();
            return AnyPartContains(doc.MainDocumentPart!, value, seen);
        }

        private static bool AnyPartContains(OpenXmlPart part, string value, HashSet<string> seen)
        {
            if (!seen.Add(part.Uri.OriginalString))
            {
                return false;
            }
            using (Stream s = part.GetStream(FileMode.Open, FileAccess.Read))
            using (var r = new StreamReader(s))
            {
                if (r.ReadToEnd().Contains(value, StringComparison.Ordinal))
                {
                    return true;
                }
            }
            return part.Parts.Any(child => AnyPartContains(child.OpenXmlPart, value, seen));
        }

        private static OpenXmlPart? ThreadedPart(MainDocumentPart main) =>
            main.Parts.Select(p => p.OpenXmlPart).FirstOrDefault(p =>
                p.ContentType.Contains("threadedcomment", StringComparison.OrdinalIgnoreCase)
                || p.Uri.OriginalString.EndsWith("threadedComments.xml", StringComparison.OrdinalIgnoreCase));

        // --- Raw-XML fixtures for drawings / text boxes -----------------------------
        //
        // Text boxes and drawings can't be authored conveniently through the strongly-typed DOM, so
        // these write document.xml directly with every namespace declared at the root.

        private const string DocNamespaces =
            "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" " +
            "xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" " +
            "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" " +
            "xmlns:wps=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" " +
            "xmlns:v=\"urn:schemas-microsoft-com:vml\" " +
            "xmlns:w10=\"urn:schemas-microsoft-com:office:word\" " +
            "xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\"";

        /// <summary>Creates a .docx whose body is exactly the supplied raw WordprocessingML.</summary>
        public static void CreateRaw(string path, string bodyInnerXml)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
            MainDocumentPart main = doc.AddMainDocumentPart();
            string xml = $"<w:document {DocNamespaces}><w:body>{bodyInnerXml}</w:body></w:document>";
            using Stream stream = main.GetStream(FileMode.Create, FileAccess.Write);
            using var writer = new StreamWriter(stream, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
            writer.Write(xml);
        }

        /// <summary>A simple body paragraph (raw XML).</summary>
        public static string ParaXml(string text) =>
            $"<w:p><w:r><w:t xml:space=\"preserve\">{Escape(text)}</w:t></w:r></w:p>";

        /// <summary>
        /// A body paragraph that anchors a modern (DrawingML) text box containing <paramref name="boxText"/>,
        /// optionally with the paragraph's own leading/trailing text around the drawing.
        /// </summary>
        public static string TextBoxParaXml(string boxText, string? before = null, string? after = null)
        {
            string lead = before is null ? "" : $"<w:r><w:t xml:space=\"preserve\">{Escape(before)}</w:t></w:r>";
            string trail = after is null ? "" : $"<w:r><w:t xml:space=\"preserve\">{Escape(after)}</w:t></w:r>";
            return "<w:p>" + lead +
                   "<w:r><w:drawing><wp:inline><a:graphic><a:graphicData " +
                   "uri=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\">" +
                   "<wps:wsp><wps:txbx><w:txbxContent>" +
                   $"<w:p><w:r><w:t xml:space=\"preserve\">{Escape(boxText)}</w:t></w:r></w:p>" +
                   "</w:txbxContent></wps:txbx><wps:bodyPr/></wps:wsp>" +
                   "</a:graphicData></a:graphic></wp:inline></w:drawing></w:r>" +
                   trail + "</w:p>";
        }

        /// <summary>A body paragraph that anchors a legacy (VML) text box containing <paramref name="boxText"/>.</summary>
        public static string VmlTextBoxParaXml(string boxText) =>
            "<w:p><w:r><w:pict><v:shape><v:textbox><w:txbxContent>" +
            $"<w:p><w:r><w:t xml:space=\"preserve\">{Escape(boxText)}</w:t></w:r></w:p>" +
            "</w:txbxContent></v:textbox></v:shape></w:pict></w:r></w:p>";

        /// <summary>A body paragraph that contains a non-text drawing (a shape) plus optional caption text.</summary>
        public static string DrawingParaXml(string? caption = null)
        {
            string cap = caption is null ? "" : $"<w:r><w:t xml:space=\"preserve\">{Escape(caption)}</w:t></w:r>";
            return "<w:p>" + cap +
                   "<w:r><w:drawing><wp:inline><a:graphic><a:graphicData " +
                   "uri=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\">" +
                   "<wps:wsp><wps:bodyPr/></wps:wsp>" +
                   "</a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>";
        }

        /// <summary>Text of every text box (txbxContent paragraph) in the document, in order.</summary>
        public static string[] TextBoxTexts(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            return doc.MainDocumentPart!.Document!.Body!
                .Descendants<TextBoxContent>()
                .SelectMany(t => t.Descendants<Paragraph>())
                .Select(p => p.InnerText)
                .ToArray();
        }

        /// <summary>Whether the document still contains any drawing / picture markup.</summary>
        public static bool HasDrawingOrPicture(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            Body body = doc.MainDocumentPart!.Document!.Body!;
            return body.Descendants<Drawing>().Any() || body.Descendants<Picture>().Any();
        }

        /// <summary>The full raw document.xml (for substring / occurrence assertions).</summary>
        public static string DocumentXml(string path)
        {
            using WordprocessingDocument doc = WordprocessingDocument.Open(path, false);
            return doc.MainDocumentPart!.Document!.OuterXml;
        }

        /// <summary>Counts non-overlapping occurrences of <paramref name="value"/> in the document.xml.</summary>
        public static int Occurrences(string path, string value)
        {
            string xml = DocumentXml(path);
            int count = 0, index = 0;
            while ((index = xml.IndexOf(value, index, StringComparison.Ordinal)) >= 0)
            {
                count++;
                index += value.Length;
            }
            return count;
        }

        private static string Escape(string text) =>
            text.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;");
    }
}
