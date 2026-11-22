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

using DocumentFormat.OpenXml;
using DocumentFormat.OpenXml.Office2019.Excel.ThreadedComments;
using DocumentFormat.OpenXml.Packaging;
using DocumentFormat.OpenXml.Spreadsheet;
using Phileas.Services.Office;

namespace Phileas.Tests
{
    /// <summary>
    /// Test-only helpers to build .xlsx fixtures (using the realistic shared-string format) and read
    /// them back, so the tests don't depend on a committed binary.
    /// </summary>
    internal static class SpreadsheetTestHelper
    {
        /// <summary>Creates a single-sheet workbook; every text value goes through the shared-string table.</summary>
        public static void CreateXlsx(string path, IReadOnlyList<string?[]> rows, string sheetName = "Sheet1")
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Create(path, SpreadsheetDocumentType.Workbook);
            WorkbookPart wbPart = doc.AddWorkbookPart();
            wbPart.Workbook = new Workbook();
            WorksheetPart wsPart = wbPart.AddNewPart<WorksheetPart>();
            SharedStringTablePart sstPart = wbPart.AddNewPart<SharedStringTablePart>();
            sstPart.SharedStringTable = new SharedStringTable();

            var sharedIndex = new Dictionary<string, int>();
            int Intern(string value)
            {
                if (sharedIndex.TryGetValue(value, out int existing))
                {
                    return existing;
                }
                int index = sharedIndex.Count;
                sharedIndex[value] = index;
                sstPart.SharedStringTable.AppendChild(new SharedStringItem(new Text(value)));
                return index;
            }

            var sheetData = new SheetData();
            for (int r = 0; r < rows.Count; r++)
            {
                var rowElement = new Row { RowIndex = (uint)(r + 1) };
                string?[] row = rows[r];
                for (int c = 0; c < row.Length; c++)
                {
                    string? value = row[c];
                    if (value is null)
                    {
                        continue;
                    }
                    string reference = SpreadsheetColumn.IndexToLetter(c + 1) + (r + 1);

                    Cell cell;
                    if (double.TryParse(value, out _))
                    {
                        // store as a number cell (no DataType)
                        cell = new Cell { CellReference = reference, CellValue = new CellValue(value) };
                    }
                    else
                    {
                        cell = new Cell
                        {
                            CellReference = reference,
                            DataType = new EnumValue<CellValues>(CellValues.SharedString),
                            CellValue = new CellValue(Intern(value).ToString())
                        };
                    }
                    rowElement.AppendChild(cell);
                }
                sheetData.AppendChild(rowElement);
            }

            wsPart.Worksheet = new Worksheet(sheetData);
            Sheets sheets = wbPart.Workbook.AppendChild(new Sheets());
            sheets.AppendChild(new Sheet { Id = wbPart.GetIdOfPart(wsPart), SheetId = 1, Name = sheetName });
            wbPart.Workbook.Save();
        }

        /// <summary>
        /// Creates a single-sheet workbook (shared-string cells) that also has a print header and footer.
        /// The header/footer strings may contain Excel field codes (e.g. <c>&amp;C</c> centre section,
        /// <c>&amp;P</c> page number) around the literal text.
        /// </summary>
        public static void CreateXlsxWithHeaderFooter(
            string path, IReadOnlyList<string?[]> rows, string oddHeader, string oddFooter, string sheetName = "Sheet1")
        {
            CreateXlsx(path, rows, sheetName);

            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: true);
            WorksheetPart wsPart = doc.WorkbookPart!.WorksheetParts.First();
            Worksheet worksheet = wsPart.Worksheet!;
            // headerFooter follows sheetData in schema order; appending after it is valid here.
            worksheet.AppendChild(new HeaderFooter(
                new OddHeader(oddHeader),
                new OddFooter(oddFooter)));
            worksheet.Save();
        }

        /// <summary>Creates a single-sheet workbook with a <b>legacy</b> cell comment (xl/comments1.xml).</summary>
        public static void CreateXlsxWithLegacyComment(
            string path, IReadOnlyList<string?[]> rows, string cellRef, string commentText,
            string author = "Reviewer", string sheetName = "Sheet1")
        {
            CreateXlsx(path, rows, sheetName);

            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: true);
            WorksheetPart wsPart = doc.WorkbookPart!.WorksheetParts.First();
            WorksheetCommentsPart commentsPart = wsPart.AddNewPart<WorksheetCommentsPart>();
            commentsPart.Comments = new Comments(
                new Authors(new Author(author)),
                new CommentList(
                    new Comment(
                        new CommentText(new Run(new Text(commentText) { Space = SpaceProcessingModeValues.Preserve })))
                    { Reference = cellRef, AuthorId = 0U }));
            commentsPart.Comments.Save();
        }

        /// <summary>Creates a single-sheet workbook with a <b>threaded</b> cell comment (xl/threadedComments + xl/persons).</summary>
        public static void CreateXlsxWithThreadedComment(
            string path, IReadOnlyList<string?[]> rows, string cellRef, string commentText,
            string authorDisplayName = "Reviewer", string sheetName = "Sheet1")
        {
            CreateXlsx(path, rows, sheetName);

            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: true);
            WorkbookPart wbPart = doc.WorkbookPart!;
            WorksheetPart wsPart = wbPart.WorksheetParts.First();

            const string personId = "{11111111-1111-1111-1111-111111111111}";
            WorkbookPersonPart personPart = wbPart.AddNewPart<WorkbookPersonPart>();
            personPart.PersonList = new PersonList(
                new Person { DisplayName = authorDisplayName, Id = personId, UserId = "S::demo::abc", ProviderId = "None" });
            personPart.PersonList.Save();

            WorksheetThreadedCommentsPart tcPart = wsPart.AddNewPart<WorksheetThreadedCommentsPart>();
            tcPart.ThreadedComments = new ThreadedComments(
                new ThreadedComment(new ThreadedCommentText(commentText))
                {
                    Ref = cellRef,
                    DT = new DateTime(2024, 1, 1, 0, 0, 0, DateTimeKind.Utc),
                    PersonId = personId,
                    Id = "{22222222-2222-2222-2222-222222222222}"
                });
            tcPart.ThreadedComments.Save();
        }

        /// <summary>Creates a workbook that has <b>both</b> a legacy and a threaded comment (as a real
        /// modern Excel file does), on separate cells, each carrying its own text.</summary>
        public static void CreateXlsxWithLegacyAndThreadedComments(
            string path, IReadOnlyList<string?[]> rows,
            string legacyCellRef, string legacyText,
            string threadedCellRef, string threadedText, string threadedAuthorDisplayName,
            string sheetName = "Sheet1")
        {
            CreateXlsxWithLegacyComment(path, rows, legacyCellRef, legacyText, sheetName: sheetName);

            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: true);
            WorkbookPart wbPart = doc.WorkbookPart!;
            WorksheetPart wsPart = wbPart.WorksheetParts.First();

            const string personId = "{11111111-1111-1111-1111-111111111111}";
            WorkbookPersonPart personPart = wbPart.AddNewPart<WorkbookPersonPart>();
            personPart.PersonList = new PersonList(
                new Person { DisplayName = threadedAuthorDisplayName, Id = personId, UserId = "S::demo::abc", ProviderId = "None" });
            personPart.PersonList.Save();

            WorksheetThreadedCommentsPart tcPart = wsPart.AddNewPart<WorksheetThreadedCommentsPart>();
            tcPart.ThreadedComments = new ThreadedComments(
                new ThreadedComment(new ThreadedCommentText(threadedText))
                {
                    Ref = threadedCellRef,
                    DT = new DateTime(2024, 1, 1, 0, 0, 0, DateTimeKind.Utc),
                    PersonId = personId,
                    Id = "{22222222-2222-2222-2222-222222222222}"
                });
            tcPart.ThreadedComments.Save();
        }

        /// <summary>All comment text and author/person names in the workbook (legacy + threaded), concatenated.</summary>
        public static string AllCommentText(string path)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            WorkbookPart wbPart = doc.WorkbookPart!;
            var parts = new List<string>();
            foreach (WorksheetPart wsPart in wbPart.WorksheetParts)
            {
                if (wsPart.WorksheetCommentsPart?.Comments is Comments comments)
                {
                    parts.Add(comments.InnerText);
                }
                foreach (WorksheetThreadedCommentsPart tc in wsPart.WorksheetThreadedCommentsParts)
                {
                    parts.Add(tc.ThreadedComments?.InnerText ?? string.Empty);
                }
            }
            foreach (WorkbookPersonPart personPart in wbPart.WorkbookPersonParts)
            {
                foreach (Person p in personPart.PersonList?.Elements<Person>() ?? Enumerable.Empty<Person>())
                {
                    parts.Add(p.DisplayName?.Value ?? string.Empty);
                }
            }
            return string.Join("\n", parts);
        }

        /// <summary>The raw XML of every comment-related part (for strong leak checks).</summary>
        public static string AllCommentXml(string path)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            WorkbookPart wbPart = doc.WorkbookPart!;
            var xml = new List<string>();
            foreach (WorksheetPart wsPart in wbPart.WorksheetParts)
            {
                if (wsPart.WorksheetCommentsPart?.Comments is Comments comments)
                {
                    xml.Add(comments.OuterXml);
                }
                foreach (WorksheetThreadedCommentsPart tc in wsPart.WorksheetThreadedCommentsParts)
                {
                    xml.Add(tc.ThreadedComments?.OuterXml ?? string.Empty);
                }
            }
            foreach (WorkbookPersonPart personPart in wbPart.WorkbookPersonParts)
            {
                xml.Add(personPart.PersonList?.OuterXml ?? string.Empty);
            }
            return string.Concat(xml);
        }

        // --- Charts -----------------------------------------------------------

        /// <summary>
        /// The chart part XML (a <c>c:chartSpace</c>) with the given title (DrawingML text), cached series
        /// name, and cached category value - shared by the Word and Excel chart fixtures.
        /// </summary>
        public static string ChartSpaceXml(string title, string seriesName, string category) =>
            ChartSpaceXmlWithTitleParagraph($"<a:p><a:r><a:t>{Escape(title)}</a:t></a:r></a:p>", seriesName, category);

        /// <summary>As <see cref="ChartSpaceXml"/>, with the title given as raw DrawingML paragraph (&lt;a:p&gt;) XML.</summary>
        public static string ChartSpaceXmlWithTitleParagraph(string titleAParagraphXml, string seriesName, string category) =>
            "<c:chartSpace xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" " +
            "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
            "<c:chart>" +
            $"<c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/>{titleAParagraphXml}</c:rich></c:tx><c:overlay val=\"0\"/></c:title>" +
            "<c:autoTitleDeleted val=\"0\"/>" +
            "<c:plotArea><c:layout/>" +
            "<c:barChart><c:barDir val=\"col\"/><c:grouping val=\"clustered\"/>" +
            "<c:ser><c:idx val=\"0\"/><c:order val=\"0\"/>" +
            $"<c:tx><c:strRef><c:f>Sheet1!$B$1</c:f><c:strCache><c:ptCount val=\"1\"/><c:pt idx=\"0\"><c:v>{Escape(seriesName)}</c:v></c:pt></c:strCache></c:strRef></c:tx>" +
            $"<c:cat><c:strRef><c:f>Sheet1!$A$2</c:f><c:strCache><c:ptCount val=\"1\"/><c:pt idx=\"0\"><c:v>{Escape(category)}</c:v></c:pt></c:strCache></c:strRef></c:cat>" +
            "<c:val><c:numRef><c:f>Sheet1!$B$2</c:f><c:numCache><c:formatCode>General</c:formatCode><c:ptCount val=\"1\"/><c:pt idx=\"0\"><c:v>42</c:v></c:pt></c:numCache></c:numRef></c:val>" +
            "</c:ser><c:axId val=\"111111111\"/><c:axId val=\"222222222\"/></c:barChart>" +
            "<c:catAx><c:axId val=\"111111111\"/><c:scaling><c:orientation val=\"minMax\"/></c:scaling><c:delete val=\"0\"/><c:axPos val=\"b\"/><c:crossAx val=\"222222222\"/></c:catAx>" +
            "<c:valAx><c:axId val=\"222222222\"/><c:scaling><c:orientation val=\"minMax\"/></c:scaling><c:delete val=\"0\"/><c:axPos val=\"l\"/><c:crossAx val=\"111111111\"/></c:valAx>" +
            "</c:plotArea><c:plotVisOnly val=\"1\"/></c:chart></c:chartSpace>";

        /// <summary>Creates a single-sheet workbook with an embedded chart (under the worksheet's drawing).</summary>
        public static void CreateXlsxWithChart(
            string path, IReadOnlyList<string?[]> rows, string title, string seriesName, string category, string sheetName = "Sheet1") =>
            CreateXlsxWithChartXml(path, rows, ChartSpaceXml(title, seriesName, category), sheetName);

        /// <summary>Creates a single-sheet workbook with an embedded chart whose chart part is <paramref name="chartSpaceXml"/>.</summary>
        public static void CreateXlsxWithChartXml(
            string path, IReadOnlyList<string?[]> rows, string chartSpaceXml, string sheetName = "Sheet1")
        {
            CreateXlsx(path, rows, sheetName);

            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: true);
            WorksheetPart wsPart = doc.WorkbookPart!.WorksheetParts.First();

            DrawingsPart drawingsPart = wsPart.AddNewPart<DrawingsPart>();
            ChartPart chartPart = drawingsPart.AddNewPart<ChartPart>();
            WriteXml(chartPart, chartSpaceXml);

            string chartRelId = drawingsPart.GetIdOfPart(chartPart);
            string drawingXml =
                "<xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" " +
                "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" " +
                "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" " +
                "xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">" +
                "<xdr:twoCellAnchor>" +
                "<xdr:from><xdr:col>3</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>1</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>" +
                "<xdr:to><xdr:col>9</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>15</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>" +
                "<xdr:graphicFrame macro=\"\"><xdr:nvGraphicFramePr><xdr:cNvPr id=\"2\" name=\"Chart 1\"/><xdr:cNvGraphicFramePr/></xdr:nvGraphicFramePr>" +
                "<xdr:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/></xdr:xfrm>" +
                $"<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/chart\"><c:chart r:id=\"{chartRelId}\"/></a:graphicData></a:graphic>" +
                "</xdr:graphicFrame><xdr:clientData/></xdr:twoCellAnchor></xdr:wsDr>";
            WriteXml(drawingsPart, drawingXml);

            wsPart.Worksheet!.Append(new Drawing { Id = wsPart.GetIdOfPart(drawingsPart) });
            wsPart.Worksheet.Save();
        }

        /// <summary>
        /// Creates a single-sheet workbook with a text box (an <c>xdr:sp</c> shape) on the sheet whose one
        /// paragraph holds the given runs. Pass more than one run to exercise PII split across <c>a:t</c> runs.
        /// </summary>
        public static void CreateXlsxWithTextBox(
            string path, IReadOnlyList<string?[]> rows, params string[] runs) =>
            CreateXlsxWithTextBoxParagraph(path, rows,
                "<a:p>" + string.Concat(runs.Select(r => $"<a:r><a:t>{Escape(r)}</a:t></a:r>")) + "</a:p>");

        /// <summary>As <see cref="CreateXlsxWithTextBox"/>, with the text box's paragraph given as raw &lt;a:p&gt; XML.</summary>
        public static void CreateXlsxWithTextBoxParagraph(
            string path, IReadOnlyList<string?[]> rows, string aParagraphXml)
        {
            CreateXlsx(path, rows);

            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: true);
            WorksheetPart wsPart = doc.WorkbookPart!.WorksheetParts.First();

            DrawingsPart drawingsPart = wsPart.AddNewPart<DrawingsPart>();
            string drawingXml =
                "<xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" " +
                "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">" +
                "<xdr:twoCellAnchor>" +
                "<xdr:from><xdr:col>1</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>1</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>" +
                "<xdr:to><xdr:col>5</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>6</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>" +
                "<xdr:sp macro=\"\" textlink=\"\">" +
                "<xdr:nvSpPr><xdr:cNvPr id=\"2\" name=\"TextBox 1\"/><xdr:cNvSpPr txBox=\"1\"/></xdr:nvSpPr>" +
                "<xdr:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"1000000\" cy=\"500000\"/></a:xfrm>" +
                "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></xdr:spPr>" +
                $"<xdr:txBody><a:bodyPr/><a:lstStyle/>{aParagraphXml}</xdr:txBody>" +
                "</xdr:sp><xdr:clientData/></xdr:twoCellAnchor></xdr:wsDr>";
            WriteXml(drawingsPart, drawingXml);

            wsPart.Worksheet!.Append(new Drawing { Id = wsPart.GetIdOfPart(drawingsPart) });
            wsPart.Worksheet.Save();
        }

        private const string SsmlMain = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
        private const string RelNs = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

        /// <summary>
        /// Creates a workbook with a pivot cache: a cache definition whose one field's shared items hold
        /// <paramref name="sharedItems"/>, cache records (an index into a shared item plus an optional inline
        /// <paramref name="recordInlineString"/>), and a minimal pivot table part referencing the cache.
        /// </summary>
        public static void CreateXlsxWithPivotCache(
            string path, IReadOnlyList<string?[]> rows, string[] sharedItems, string? recordInlineString = null)
        {
            CreateXlsx(path, rows);

            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: true);
            WorkbookPart wbPart = doc.WorkbookPart!;
            WorksheetPart wsPart = wbPart.WorksheetParts.First();

            PivotTableCacheDefinitionPart defPart = wbPart.AddNewPart<PivotTableCacheDefinitionPart>();
            string sharedXml = string.Concat(sharedItems.Select(s => $"<s v=\"{Escape(s)}\"/>"));
            WriteXml(defPart,
                $"<pivotCacheDefinition xmlns=\"{SsmlMain}\" xmlns:r=\"{RelNs}\" recordCount=\"2\">" +
                "<cacheSource type=\"worksheet\"><worksheetSource ref=\"A1:A3\" sheet=\"Sheet1\"/></cacheSource>" +
                "<cacheFields count=\"1\">" +
                $"<cacheField name=\"Name\" numFmtId=\"0\"><sharedItems count=\"{sharedItems.Length}\">{sharedXml}</sharedItems></cacheField>" +
                "</cacheFields></pivotCacheDefinition>");

            PivotTableCacheRecordsPart recPart = defPart.AddNewPart<PivotTableCacheRecordsPart>();
            string inlineRecord = recordInlineString is null ? string.Empty : $"<r><s v=\"{Escape(recordInlineString)}\"/></r>";
            WriteXml(recPart,
                $"<pivotCacheRecords xmlns=\"{SsmlMain}\" xmlns:r=\"{RelNs}\" count=\"2\"><r><x v=\"0\"/></r>{inlineRecord}</pivotCacheRecords>");

            PivotTablePart ptPart = wsPart.AddNewPart<PivotTablePart>();
            string cacheRelId = wbPart.GetIdOfPart(defPart);
            WriteXml(ptPart,
                $"<pivotTableDefinition xmlns=\"{SsmlMain}\" name=\"PivotTable1\" cacheId=\"1\" dataOnRows=\"1\">" +
                "<location ref=\"A5:B7\" firstHeaderRow=\"1\" firstDataRow=\"1\" firstDataCol=\"1\"/>" +
                "<pivotFields count=\"1\"><pivotField axis=\"axisRow\" showAll=\"0\"><items count=\"1\"><item x=\"0\"/></items></pivotField></pivotFields>" +
                "</pivotTableDefinition>");

            wbPart.Workbook!.AppendChild(new PivotCaches(new PivotCache { CacheId = 1U, Id = cacheRelId }));
            wbPart.Workbook.Save();
        }

        /// <summary>The concatenated XML of every pivot cache/table part (for redaction checks).</summary>
        public static string AllPivotXml(string path)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            WorkbookPart wbPart = doc.WorkbookPart!;
            var xml = new List<string>();
            foreach (PivotTableCacheDefinitionPart defPart in wbPart.PivotTableCacheDefinitionParts)
            {
                xml.Add(defPart.RootElement?.OuterXml ?? string.Empty);
                if (defPart.PivotTableCacheRecordsPart?.RootElement is OpenXmlElement rec)
                {
                    xml.Add(rec.OuterXml);
                }
            }
            foreach (WorksheetPart wsPart in wbPart.WorksheetParts)
            {
                foreach (PivotTablePart ptPart in wsPart.PivotTableParts)
                {
                    xml.Add(ptPart.RootElement?.OuterXml ?? string.Empty);
                }
            }
            return string.Concat(xml);
        }

        /// <summary>True if any pivot cache definition is set to refresh on load.</summary>
        public static bool AnyPivotRefreshOnLoad(string path)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            return doc.WorkbookPart!.PivotTableCacheDefinitionParts
                .Any(p => (p.RootElement as PivotCacheDefinition)?.RefreshOnLoad?.Value == true);
        }

        /// <summary>Creates a workbook with an embedded package (an Office document) on the first worksheet.</summary>
        public static void CreateXlsxWithEmbeddedPackage(string path, IReadOnlyList<string?[]> rows, byte[] pkgBytes, string contentType)
        {
            CreateXlsx(path, rows);
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: true);
            WorksheetPart wsPart = doc.WorkbookPart!.WorksheetParts.First(); // embeddings hang off the sheet
            EmbeddedPackagePart pkg = wsPart.AddNewPart<EmbeddedPackagePart>(contentType, "rIdEmb");
            using Stream s = pkg.GetStream(FileMode.Create, FileAccess.Write);
            s.Write(pkgBytes, 0, pkgBytes.Length);
        }

        /// <summary>
        /// Creates a workbook with an embedded object Philter Desktop can't inspect - modeled as an embedded
        /// package with a non-Word/Excel content type (e.g. PowerPoint), which the redactor treats as opaque.
        /// </summary>
        public static void CreateXlsxWithEmbeddedOleObject(string path, IReadOnlyList<string?[]> rows, byte[] bytes) =>
            CreateXlsxWithEmbeddedPackage(path, rows, bytes, "application/vnd.openxmlformats-officedocument.presentationml.presentation");

        /// <summary>True if the workbook still carries any embedded package or OLE object (on any worksheet).</summary>
        public static bool XlsxHasAnyEmbeddedObject(string path)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            return doc.WorkbookPart!.WorksheetParts.Any(ws =>
                ws.GetPartsOfType<EmbeddedPackagePart>().Any() || ws.GetPartsOfType<EmbeddedObjectPart>().Any());
        }

        /// <summary>All cell text of the first embedded package on any worksheet, read as an .xlsx (empty if none).</summary>
        public static string XlsxEmbeddedPackageAllText(string path)
        {
            byte[]? bytes;
            using (SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false))
            {
                EmbeddedPackagePart? pkg = doc.WorkbookPart!.WorksheetParts
                    .SelectMany(ws => ws.GetPartsOfType<EmbeddedPackagePart>()).FirstOrDefault();
                if (pkg is null) { return string.Empty; }
                using Stream s = pkg.GetStream(FileMode.Open, FileAccess.Read);
                using var ms = new MemoryStream();
                s.CopyTo(ms);
                bytes = ms.ToArray();
            }
            string tmp = Path.Combine(Path.GetTempPath(), "xlemb-" + Guid.NewGuid().ToString("N") + ".xlsx");
            try { File.WriteAllBytes(tmp, bytes); return AllText(tmp); }
            finally { try { File.Delete(tmp); } catch { /* best effort */ } }
        }

        /// <summary>The concatenated drawing XML (shapes/text boxes) of every worksheet (for redaction checks).</summary>
        public static string AllDrawingXml(string path)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            var xml = new List<string>();
            foreach (WorksheetPart wsPart in doc.WorkbookPart!.WorksheetParts)
            {
                if (wsPart.DrawingsPart?.RootElement is OpenXmlElement root)
                {
                    xml.Add(root.OuterXml);
                }
            }
            return string.Concat(xml);
        }

        /// <summary>The concatenated XML of every chart part in the workbook (for redaction checks).</summary>
        public static string AllChartXml(string path)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            var xml = new List<string>();
            foreach (WorksheetPart wsPart in doc.WorkbookPart!.WorksheetParts)
            {
                if (wsPart.DrawingsPart is null)
                {
                    continue;
                }
                foreach (ChartPart chartPart in wsPart.DrawingsPart.ChartParts)
                {
                    xml.Add(chartPart.RootElement?.OuterXml ?? string.Empty);
                }
            }
            return string.Concat(xml);
        }

        private static void WriteXml(OpenXmlPart part, string xml)
        {
            using Stream s = part.GetStream(FileMode.Create, FileAccess.Write);
            using var w = new StreamWriter(s, new System.Text.UTF8Encoding(false));
            w.Write(xml);
        }

        private static string Escape(string text) =>
            text.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;");

        /// <summary>The concatenated header/footer text across every worksheet (for redaction checks).</summary>
        public static string HeaderFooterText(string path)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            var parts = new List<string>();
            foreach (WorksheetPart wsPart in doc.WorkbookPart!.WorksheetParts)
            {
                HeaderFooter? hf = wsPart.Worksheet?.GetFirstChild<HeaderFooter>();
                if (hf is null)
                {
                    continue;
                }
                foreach (OpenXmlLeafTextElement element in hf.Elements<OpenXmlLeafTextElement>())
                {
                    parts.Add(element.Text ?? string.Empty);
                }
            }
            return string.Join("\n", parts);
        }

        internal enum CellKind { Auto, Number, Text, Boolean, Date, Formula, Error }

        internal readonly record struct CellSpec(string Value, CellKind Kind = CellKind.Auto, string? Formula = null)
        {
            public static implicit operator CellSpec(string value) => new(value);
        }

        /// <summary>Creates a single-sheet workbook with explicitly-typed cells (numbers, booleans, dates, formulas).</summary>
        public static void CreateTyped(string path, IReadOnlyList<CellSpec?[]> rows, string sheetName = "Sheet1")
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Create(path, SpreadsheetDocumentType.Workbook);
            WorkbookPart wbPart = doc.AddWorkbookPart();
            wbPart.Workbook = new Workbook();
            WorksheetPart wsPart = wbPart.AddNewPart<WorksheetPart>();

            var sheetData = new SheetData();
            for (int r = 0; r < rows.Count; r++)
            {
                var rowElement = new Row { RowIndex = (uint)(r + 1) };
                CellSpec?[] row = rows[r];
                for (int c = 0; c < row.Length; c++)
                {
                    if (row[c] is not CellSpec spec)
                    {
                        continue;
                    }
                    string reference = SpreadsheetColumn.IndexToLetter(c + 1) + (r + 1);
                    rowElement.AppendChild(BuildCell(reference, spec));
                }
                sheetData.AppendChild(rowElement);
            }

            wsPart.Worksheet = new Worksheet(sheetData);
            Sheets sheets = wbPart.Workbook.AppendChild(new Sheets());
            sheets.AppendChild(new Sheet { Id = wbPart.GetIdOfPart(wsPart), SheetId = 1, Name = sheetName });
            wbPart.Workbook.Save();
        }

        private static Cell BuildCell(string reference, CellSpec spec)
        {
            CellKind kind = spec.Kind;
            if (kind == CellKind.Auto)
            {
                kind = double.TryParse(spec.Value, out _) ? CellKind.Number : CellKind.Text;
            }

            return kind switch
            {
                CellKind.Number => new Cell { CellReference = reference, CellValue = new CellValue(spec.Value) },
                CellKind.Boolean => new Cell { CellReference = reference, DataType = new EnumValue<CellValues>(CellValues.Boolean), CellValue = new CellValue(spec.Value) },
                CellKind.Date => new Cell { CellReference = reference, DataType = new EnumValue<CellValues>(CellValues.Date), CellValue = new CellValue(spec.Value) },
                CellKind.Error => new Cell { CellReference = reference, DataType = new EnumValue<CellValues>(CellValues.Error), CellValue = new CellValue(spec.Value) },
                CellKind.Formula => new Cell { CellReference = reference, CellFormula = new CellFormula(spec.Formula ?? "1+1"), CellValue = new CellValue(spec.Value) },
                _ => new Cell { CellReference = reference, DataType = new EnumValue<CellValues>(CellValues.InlineString), InlineString = new InlineString(new Text(spec.Value) { Space = SpaceProcessingModeValues.Preserve }) }
            };
        }

        /// <summary>
        /// Creates a single-sheet workbook whose cells have <b>no</b> CellReference (some generators omit
        /// it). Column identity then depends purely on document order - used to test.
        /// </summary>
        public static void CreateXlsxWithoutCellReferences(string path, IReadOnlyList<string?[]> rows, string sheetName = "Sheet1")
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Create(path, SpreadsheetDocumentType.Workbook);
            WorkbookPart wbPart = doc.AddWorkbookPart();
            wbPart.Workbook = new Workbook();
            WorksheetPart wsPart = wbPart.AddNewPart<WorksheetPart>();

            var sheetData = new SheetData();
            for (int r = 0; r < rows.Count; r++)
            {
                var rowElement = new Row { RowIndex = (uint)(r + 1) };
                foreach (string? value in rows[r])
                {
                    if (value is null)
                    {
                        continue;
                    }
                    // No CellReference is set on purpose.
                    rowElement.AppendChild(new Cell
                    {
                        DataType = new EnumValue<CellValues>(CellValues.InlineString),
                        InlineString = new InlineString(new Text(value))
                    });
                }
                sheetData.AppendChild(rowElement);
            }

            wsPart.Worksheet = new Worksheet(sheetData);
            Sheets sheets = wbPart.Workbook.AppendChild(new Sheets());
            sheets.AppendChild(new Sheet { Id = wbPart.GetIdOfPart(wsPart), SheetId = 1, Name = sheetName });
            wbPart.Workbook.Save();
        }

        /// <summary>Adds a second sheet to an existing workbook (text values as inline strings).</summary>
        public static void AppendSheet(string path, IReadOnlyList<string?[]> rows, string sheetName)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: true);
            WorkbookPart wbPart = doc.WorkbookPart!;
            WorksheetPart wsPart = wbPart.AddNewPart<WorksheetPart>();

            var sheetData = new SheetData();
            for (int r = 0; r < rows.Count; r++)
            {
                var rowElement = new Row { RowIndex = (uint)(r + 1) };
                string?[] row = rows[r];
                for (int c = 0; c < row.Length; c++)
                {
                    if (row[c] is not string value)
                    {
                        continue;
                    }
                    rowElement.AppendChild(new Cell
                    {
                        CellReference = SpreadsheetColumn.IndexToLetter(c + 1) + (r + 1),
                        DataType = new EnumValue<CellValues>(CellValues.InlineString),
                        InlineString = new InlineString(new Text(value))
                    });
                }
                sheetData.AppendChild(rowElement);
            }
            wsPart.Worksheet = new Worksheet(sheetData);

            Workbook workbook = wbPart.Workbook!;
            Sheets sheets = workbook.GetFirstChild<Sheets>() ?? workbook.AppendChild(new Sheets());
            uint sheetId = (uint)(sheets.Elements<Sheet>().Count() + 1);
            sheets.AppendChild(new Sheet { Id = wbPart.GetIdOfPart(wsPart), SheetId = sheetId, Name = sheetName });
            workbook.Save();
        }

        /// <summary>All cell text in the workbook (across sheets), concatenated with newlines.</summary>
        public static string AllText(string path)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            WorkbookPart wbPart = doc.WorkbookPart!;
            var lines = new List<string>();
            foreach (WorksheetPart wsPart in wbPart.WorksheetParts)
            {
                SheetData? sheetData = wsPart.Worksheet?.GetFirstChild<SheetData>();
                if (sheetData is null)
                {
                    continue;
                }
                foreach (Cell cell in sheetData.Descendants<Cell>())
                {
                    lines.Add(ReadCell(cell, wbPart));
                }
            }
            return string.Join("\n", lines);
        }

        /// <summary>True if the given cell reference is a genuine numeric cell (no DataType) in the workbook.</summary>
        public static bool IsNumberCell(string path, string cellReference)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            Cell? cell = doc.WorkbookPart!.WorksheetParts
                .SelectMany(w => w.Worksheet?.Descendants<Cell>() ?? Enumerable.Empty<Cell>())
                .FirstOrDefault(c => c.CellReference?.Value == cellReference);
            return cell is not null && cell.CellFormula is null && cell.DataType is null;
        }

        /// <summary>True if the given cell still holds a formula (an <c>&lt;f&gt;</c> element).</summary>
        public static bool IsFormulaCell(string path, string cellReference)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            Cell? cell = doc.WorkbookPart!.WorksheetParts
                .SelectMany(w => w.Worksheet?.Descendants<Cell>() ?? Enumerable.Empty<Cell>())
                .FirstOrDefault(c => c.CellReference?.Value == cellReference);
            return cell?.CellFormula is not null;
        }

        /// <summary>The cell's cached/stored value (<c>&lt;v&gt;</c>), or null when the cell has none.</summary>
        public static string? CachedValue(string path, string cellReference)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            Cell? cell = doc.WorkbookPart!.WorksheetParts
                .SelectMany(w => w.Worksheet?.Descendants<Cell>() ?? Enumerable.Empty<Cell>())
                .FirstOrDefault(c => c.CellReference?.Value == cellReference);
            return cell?.CellValue?.InnerText;
        }

        /// <summary>True if the workbook is set to fully recalculate on open.</summary>
        public static bool FullCalcOnLoad(string path)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            return doc.WorkbookPart!.Workbook?.GetFirstChild<CalculationProperties>()?.FullCalculationOnLoad?.Value == true;
        }

        /// <summary>True if the given cell is still a boolean-typed cell.</summary>
        public static bool IsBooleanCell(string path, string cellReference)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            Cell? cell = doc.WorkbookPart!.WorksheetParts
                .SelectMany(w => w.Worksheet?.Descendants<Cell>() ?? Enumerable.Empty<Cell>())
                .FirstOrDefault(c => c.CellReference?.Value == cellReference);
            return cell?.DataType is not null && cell.DataType.Value == CellValues.Boolean;
        }

        /// <summary>The concatenated raw XML of every worksheet part (for residue checks).</summary>
        public static string WorksheetXml(string path)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            return string.Concat(doc.WorkbookPart!.WorksheetParts.Select(w => w.Worksheet?.OuterXml ?? string.Empty));
        }

        /// <summary>True if the given cell is stored as an inline string (what a redacted cell becomes).</summary>
        public static bool IsInlineStringCell(string path, string cellReference)
        {
            using SpreadsheetDocument doc = SpreadsheetDocument.Open(path, isEditable: false);
            Cell? cell = doc.WorkbookPart!.WorksheetParts
                .SelectMany(w => w.Worksheet?.Descendants<Cell>() ?? Enumerable.Empty<Cell>())
                .FirstOrDefault(c => c.CellReference?.Value == cellReference);
            return cell?.DataType is not null && cell.DataType.Value == CellValues.InlineString;
        }

        private static string ReadCell(Cell cell, WorkbookPart wbPart)
        {
            if (cell.DataType?.Value == CellValues.SharedString)
            {
                SharedStringTablePart? sst = wbPart.SharedStringTablePart;
                if (sst is not null && int.TryParse(cell.CellValue?.InnerText, out int index))
                {
                    return sst.SharedStringTable?.Elements<SharedStringItem>().ElementAtOrDefault(index)?.InnerText ?? string.Empty;
                }
                return string.Empty;
            }
            if (cell.DataType?.Value == CellValues.InlineString)
            {
                return cell.InlineString?.InnerText ?? string.Empty;
            }
            return cell.CellValue?.InnerText ?? string.Empty;
        }
    }
}
