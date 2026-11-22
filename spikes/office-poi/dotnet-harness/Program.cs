using System.Text.Json;
using DocumentFormat.OpenXml;
using DocumentFormat.OpenXml.Packaging;
using DocumentFormat.OpenXml.Validation;
using Phileas.Model;
using Phileas.Policy;
using Phileas.Services;
using Phileas.Services.Office;
using Phileas.Tests;

// Spike harness: the .NET reference side.
//   fixtures <dir>                 generate the fixture corpus with the phileas-dotnet test helpers
//   redact <policy> <in> <out>     redact with phileas-dotnet; prints span count
//   detect <policy> <file>         Detect (no write); prints JSON spans
//   paragraphs <file>              WordDocumentRedactor.ReadParagraphs as JSON (docx only)
//   validate <file>                OpenXmlValidator errors (Office 2019 schema)
var cmd = args[0];
switch (cmd)
{
    case "fixtures": Fixtures.Generate(args[1]); break;
    case "redact":
    {
        var filter = MakeFilter(args[1]);
        List<OfficeRedactionSpan> spans = IsDocx(args[2])
            ? WordDocumentRedactor.Redact(args[2], args[3], filter)
            : XlsxRedactor.Redact(args[2], args[3], filter);
        Console.WriteLine(spans.Count);
        break;
    }
    case "detect":
    {
        var filter = MakeFilter(args[1]);
        List<OfficeRedactionSpan> spans = IsDocx(args[2])
            ? WordDocumentRedactor.Detect(args[2], filter)
            : XlsxRedactor.Detect(args[2], filter);
        Console.WriteLine(JsonSerializer.Serialize(spans.Select(s => new { s.ParagraphIndex, s.Text, s.Replacement })));
        break;
    }
    case "paragraphs":
        Console.WriteLine(JsonSerializer.Serialize(WordDocumentRedactor.ReadParagraphs(args[1])));
        break;
    case "validate":
    {
        var validator = new OpenXmlValidator(FileFormatVersions.Office2019);
        using OpenXmlPackage pkg = IsDocx(args[1])
            ? WordprocessingDocument.Open(args[1], false)
            : SpreadsheetDocument.Open(args[1], false);
        foreach (ValidationErrorInfo e in validator.Validate(pkg).Take(20))
        {
            Console.WriteLine($"{e.Part?.Uri} {e.Path?.XPath}: {e.Description}");
        }
        break;
    }
}

static bool IsDocx(string path) => path.EndsWith(".docx", StringComparison.OrdinalIgnoreCase);

static Func<string, TextFilterResult> MakeFilter(string policyPath)
{
    Policy policy = PolicySerializer.DeserializeFromJson(File.ReadAllText(policyPath));
    var service = new FilterService();
    return text => service.Filter(policy, "spike", 0, text);
}

static class Fixtures
{
    // Synthetic values only. Each fixture carries at least one value the spike policy (SSN, email,
    // phone, credit card) detects, placed in the structure the fixture exists to exercise.
    const string Ssn = "123-45-6789";
    const string Email = "jane.doe@example.com";
    const string Phone = "(555) 555-0142";

    public static void Generate(string dir)
    {
        Directory.CreateDirectory(dir);
        string P(string name) => Path.Combine(dir, name);

        WordDocs.Create(P("w-body.docx"), $"Patient SSN {Ssn} on file.", $"Email {Email} or call {Phone}.", "No PII here.");
        WordDocs.CreateWithHeaderFooter(P("w-header-footer.docx"), $"Header {Email}", $"Footer SSN {Ssn}", "Body text.");
        WordDocs.CreateWithTrackedDeletion(P("w-tracked-deletion.docx"), $"deleted SSN {Ssn}", "Visible paragraph.");
        WordDocs.CreateWithSimpleField(P("w-simple-field.docx"), $"HYPERLINK \"mailto:{Email}\"", "Contact us", "Body.");
        WordDocs.CreateWithComplexField(P("w-complex-field.docx"), new[] { "HYPERLINK \"mailto:jane.doe@", "example.com\"" }, "Contact", "Body.");
        WordDocs.CreateWithHyperlink(P("w-hyperlink.docx"), "email me", $"mailto:{Email}", "Body.");
        WordDocs.CreateWithHyperlinkInHeader(P("w-hyperlink-header.docx"), "email me", $"mailto:{Email}", "Body.");
        WordDocs.CreateWithComment(P("w-comment.docx"), $"Reviewer note: SSN {Ssn}", "Body.");
        WordDocs.CreateWithThreadedThread(P("w-threaded-comments.docx"), new[] { $"First {Email}", $"Reply {Ssn}" }, "Body.");
        WordDocs.CreateWithChartData(P("w-chart.docx"), $"Chart for {Email}", $"Series {Ssn}", $"Cat {Phone}", "Body.");
        WordDocs.CreateWithSmartArt(P("w-smartart.docx"), WordDocs.AParagraph("Node ", Email), "Body.");
        WordDocs.CreateWithNotes(P("w-notes.docx"), $"Footnote {Ssn}", $"Endnote {Email}", "Body.");
        WordDocs.CreateRaw(P("w-textboxes.docx"),
            WordDocs.TextBoxParaXml($"Box {Ssn}", before: $"Before {Email} ", after: " after") +
            WordDocs.VmlTextBoxParaXml($"VML box {Phone}"));
        // Runs split mid-value, formatting, a break, a tab, a soft hyphen, a non-breaking hyphen, a table.
        WordDocs.CreateRaw(P("w-runs.docx"),
            "<w:p><w:r><w:rPr><w:b/></w:rPr><w:t xml:space=\"preserve\">SSN 123-</w:t></w:r>" +
            "<w:r><w:rPr><w:i/></w:rPr><w:t>45-6789</w:t></w:r><w:r><w:t xml:space=\"preserve\"> end.</w:t></w:r></w:p>" +
            "<w:p><w:r><w:t xml:space=\"preserve\">Line one</w:t><w:br/><w:t xml:space=\"preserve\">jane.doe@example.com</w:t>" +
            "<w:tab/><w:t>tabbed</w:t></w:r></w:p>" +
            "<w:p><w:r><w:t>123</w:t><w:noBreakHyphen/><w:t>45</w:t><w:noBreakHyphen/><w:t>6789</w:t></w:r></w:p>" +
            "<w:p><w:r><w:t>Wash</w:t><w:softHyphen/><w:t xml:space=\"preserve\">ington jane.doe@example.com</w:t></w:r></w:p>" +
            "<w:tbl><w:tr><w:tc><w:p><w:r><w:t>Name</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>555-555-0142</w:t></w:r></w:p></w:tc></w:tr></w:tbl>" +
            "<w:p/>");

        byte[] wb = BuildWorkbookBytes(new[] { new[] { "Name", "SSN" }, new[] { "Jane", Ssn } });
        WordDocs.CreateWithChartEmbeddedWorkbook(P("w-chart-embedded-workbook.docx"), wb, $"Series {Email}", "Cat");

        var rows = new List<string?[]>
        {
            new[] { "Name", "SSN", "Email" },
            new[] { "Jane", Ssn, Email },
            new[] { "John", "987-65-4321", "john@example.org" },
            new[] { "Shared", Email, "No PII" },   // the same shared string used twice
        };
        SpreadsheetTestHelper.CreateXlsx(P("x-cells.xlsx"), rows);
        SpreadsheetTestHelper.CreateXlsxWithHeaderFooter(P("x-header-footer.xlsx"), rows, $"&LHeader {Email}", $"&CFooter {Ssn}");
        SpreadsheetTestHelper.CreateXlsxWithLegacyAndThreadedComments(P("x-comments.xlsx"), rows, "A1", $"Legacy {Ssn}", "B1", $"Threaded {Email}", "Jane Reviewer");
        SpreadsheetTestHelper.CreateXlsxWithChart(P("x-chart.xlsx"), rows, $"Chart {Email}", $"Series {Ssn}", $"Cat {Phone}");
        SpreadsheetTestHelper.CreateXlsxWithTextBox(P("x-textbox.xlsx"), rows, "Box ", Ssn);
        SpreadsheetTestHelper.CreateXlsxWithPivotCache(P("x-pivot.xlsx"), rows, new[] { "Jane", Ssn, Email }, Phone);
        SpreadsheetTestHelper.CreateTyped(P("x-typed.xlsx"), new List<SpreadsheetTestHelper.CellSpec?[]>
        {
            new SpreadsheetTestHelper.CellSpec?[] { "SSN", "Copy" },
            new SpreadsheetTestHelper.CellSpec?[] { new SpreadsheetTestHelper.CellSpec("123456789", SpreadsheetTestHelper.CellKind.Number),
                new SpreadsheetTestHelper.CellSpec(Ssn, SpreadsheetTestHelper.CellKind.Formula, "A2") },
        });
    }

    static byte[] BuildWorkbookBytes(string[][] rows)
    {
        string tmp = Path.Combine(Path.GetTempPath(), "spike-" + Guid.NewGuid().ToString("N") + ".xlsx");
        try
        {
            SpreadsheetTestHelper.CreateXlsx(tmp, rows.Select(r => (string?[])r).ToList());
            return File.ReadAllBytes(tmp);
        }
        finally { File.Delete(tmp); }
    }
}
