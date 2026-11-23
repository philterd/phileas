// Copyright 2026 Philterd, LLC. Licensed under the Apache License, Version 2.0.
//
// Usage: dotnet run --project src/test/validator -- <dir>
//
// <dir> holds input/ and output/, written by OoxmlValidationCorpusTest: each output document is the
// redaction of the input document with the same name. An output fails when the Open XML SDK validator
// (Office 2019 schema) reports an error for it more often than for its input, so a fixture's own defects
// are not blamed on redaction. Errors are compared by part and description, not by element path, since
// redaction can move an element with a pre-existing error (rebuilding the runs before it, for example).
using DocumentFormat.OpenXml;
using DocumentFormat.OpenXml.Packaging;
using DocumentFormat.OpenXml.Validation;

if (args.Length != 1)
{
    Console.Error.WriteLine("Usage: OoxmlValidator <dir with input/ and output/>");
    return 2;
}

string outputDir = Path.Combine(args[0], "output");
string inputDir = Path.Combine(args[0], "input");
string[] outputs = Directory.GetFiles(outputDir).Where(f => f.EndsWith(".docx") || f.EndsWith(".xlsx")).OrderBy(f => f).ToArray();
if (outputs.Length == 0)
{
    Console.Error.WriteLine($"No .docx or .xlsx files in {outputDir}.");
    return 2;
}

int failed = 0;
foreach (string output in outputs)
{
    string name = Path.GetFileName(output);
    Dictionary<string, int> before = Errors(Path.Combine(inputDir, name)).GroupBy(e => e.Key).ToDictionary(g => g.Key, g => g.Count());
    List<(string Key, string Detail)> after = Errors(output);
    var added = new List<string>();
    foreach (IGrouping<string, (string Key, string Detail)> group in after.GroupBy(e => e.Key))
    {
        int extra = group.Count() - before.GetValueOrDefault(group.Key);
        added.AddRange(group.Take(Math.Max(0, extra)).Select(e => e.Detail));
    }
    if (added.Count == 0)
    {
        Console.WriteLine($"ok      {name}");
        continue;
    }
    failed++;
    Console.WriteLine($"FAILED  {name}");
    foreach (string error in added.Take(20))
    {
        Console.WriteLine($"        {error}");
    }
}

Console.WriteLine($"{outputs.Length} documents, {failed} with new validation errors.");
return failed == 0 ? 0 : 1;

// The validation errors of a document: a key of part and description, and the full detail with the
// element path. A document the SDK cannot open or validate yields one error naming the exception.
static List<(string Key, string Detail)> Errors(string path)
{
    var errors = new List<(string, string)>();
    if (!File.Exists(path))
    {
        return errors;
    }
    try
    {
        using OpenXmlPackage package = path.EndsWith(".docx")
            ? WordprocessingDocument.Open(path, false)
            : SpreadsheetDocument.Open(path, false);
        foreach (ValidationErrorInfo error in new OpenXmlValidator(FileFormatVersions.Office2019).Validate(package))
        {
            errors.Add(($"{error.Part?.Uri}: {error.Description}", $"{error.Part?.Uri} {error.Path?.XPath}: {error.Description}"));
        }
    }
    catch (Exception e)
    {
        string message = $"cannot validate: {e.GetType().Name}: {e.Message}";
        errors.Add((message, message));
    }
    return errors;
}
